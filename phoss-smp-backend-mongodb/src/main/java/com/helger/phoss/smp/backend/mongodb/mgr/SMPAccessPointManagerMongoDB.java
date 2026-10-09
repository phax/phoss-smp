/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.backend.mongodb.mgr;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.IPagingSpec;
import com.helger.phoss.smp.backend.mongodb.SMPMongoQueryHelper;
import com.helger.phoss.smp.domain.accesspoint.ESMPAccessPointColumn;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPointHelper;
import com.helger.photon.audit.AuditHelper;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * Implementation of {@link ISMPAccessPointManager} for MongoDB
 *
 * @author Philip Helger
 */
public final class SMPAccessPointManagerMongoDB extends AbstractManagerMongoDB implements ISMPAccessPointManager
{
  private static final String BSON_ID = "id";
  private static final ESMPAccessPointColumn [] COLUMNS = ESMPAccessPointColumn.values ();
  private static final String BSON_NAME = "name";
  private static final String BSON_NAME_LOOKUP_KEY = "namekey";
  private static final String BSON_ENDPOINT_REFERENCE = "endpointreference";
  private static final String BSON_CERTIFICATE = "certificate";

  public SMPAccessPointManagerMongoDB ()
  {
    super ("smp-accesspoint");
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
    // Remove the old unique URL index, because Access Points are now identified by name.
    for (final Document aIndex : getCollection ().listIndexes ())
      if ((BSON_ENDPOINT_REFERENCE + "_1").equals (aIndex.getString ("name")))
      {
        getCollection ().dropIndex (BSON_ENDPOINT_REFERENCE + "_1");
        break;
      }
    getCollection ().createIndex (Indexes.ascending (BSON_NAME_LOOKUP_KEY),
                                  new IndexOptions ().unique (true)
                                                     .partialFilterExpression (Filters.exists (BSON_NAME_LOOKUP_KEY)));
  }

  @NonNull
  @ReturnsMutableCopy
  public static Document toBson (@NonNull final ISMPAccessPoint aValue)
  {
    return new Document ().append (BSON_ID, aValue.getID ())
                          .append (BSON_NAME, aValue.getName ())
                          .append (BSON_NAME_LOOKUP_KEY, SMPAccessPointHelper.createNameLookupKey (aValue))
                          .append (BSON_ENDPOINT_REFERENCE, aValue.getEndpointReference ())
                          .append (BSON_CERTIFICATE, aValue.getCertificate ());
  }

  @NonNull
  @ReturnsMutableCopy
  public static SMPAccessPoint toDomain (@NonNull final Document aDoc)
  {
    String sName = aDoc.getString (BSON_NAME);
    if (StringHelper.isEmpty (sName))
      sName = aDoc.getString (BSON_ID);
    return new SMPAccessPoint (aDoc.getString (BSON_ID),
                               sName,
                               aDoc.getString (BSON_ENDPOINT_REFERENCE),
                               aDoc.getString (BSON_CERTIFICATE));
  }

  @Nullable
  public ISMPAccessPoint createAccessPoint (@NonNull @Nonempty final String sName,
                                            @Nullable final String sEndpointReference,
                                            @Nullable final String sCertificate)
  {
    ValueEnforcer.notEmpty (sName, "Name");

    final SMPAccessPoint aNew = SMPAccessPoint.createWithNewID (sName, sEndpointReference, sCertificate);
    try
    {
      if (!getCollection ().insertOne (toBson (aNew)).wasAcknowledged ())
        throw new IllegalStateException ("Failed to insert into MongoDB Collection");
    }
    catch (final MongoWriteException ex)
    {
      if (getAccessPointOfName (sName) != null)
      {
        AuditHelper.onAuditCreateFailure (SMPAccessPoint.OT, "name-already-in-use", sName);
        return null;
      }
      throw ex;
    }

    AuditHelper.onAuditCreateSuccess (SMPAccessPoint.OT, aNew.getID (), sName, sEndpointReference);
    return aNew;
  }

  @NonNull
  public EChange updateAccessPoint (@Nullable final String sID,
                                    @NonNull @Nonempty final String sName,
                                    @Nullable final String sEndpointReference,
                                    @Nullable final String sCertificate)
  {
    ValueEnforcer.notEmpty (sName, "Name");

    if (StringHelper.isEmpty (sID))
      return EChange.UNCHANGED;

    final ISMPAccessPoint aAP = getAccessPointOfID (sID);
    if (aAP == null)
      return EChange.UNCHANGED;

    final String sNewLookupKey = SMPAccessPointHelper.createNameLookupKey (sName);
    final ISMPAccessPoint aExistingNameAP = getAccessPointOfName (sName);
    if (aExistingNameAP != null && !aExistingNameAP.getID ().equals (sID))
      return EChange.UNCHANGED;

    if (sName.equals (aAP.getName ()) &&
        aAP.hasSameEndpointReference (sEndpointReference) &&
        aAP.hasSameCertificate (sCertificate))
      return EChange.UNCHANGED;

    try
    {
      final UpdateResult aUR = getCollection ().updateOne (Filters.eq (BSON_ID, sID),
                                                           Updates.combine (Updates.set (BSON_NAME, sName),
                                                                            Updates.set (BSON_NAME_LOOKUP_KEY,
                                                                                         sNewLookupKey),
                                                                            Updates.set (BSON_ENDPOINT_REFERENCE,
                                                                                         sEndpointReference),
                                                                            Updates.set (BSON_CERTIFICATE,
                                                                                         sCertificate)));
      if (!aUR.wasAcknowledged () || aUR.getMatchedCount () == 0)
        return EChange.UNCHANGED;
    }
    catch (final MongoWriteException ex)
    {
      final ISMPAccessPoint aConcurrentNameAP = getAccessPointOfName (sName);
      if (aConcurrentNameAP != null && !aConcurrentNameAP.getID ().equals (sID))
        return EChange.UNCHANGED;
      throw ex;
    }

    AuditHelper.onAuditModifySuccess (SMPAccessPoint.OT, "set-all", sID, sName, sEndpointReference);
    return EChange.CHANGED;
  }

  @NonNull
  public EChange updateAccessPointCertificate (@Nullable final String sID, @Nullable final String sNewCertificate)
  {
    if (StringHelper.isEmpty (sID))
      return EChange.UNCHANGED;

    final ISMPAccessPoint aAP = getAccessPointOfID (sID);
    if (aAP == null)
    {
      AuditHelper.onAuditModifyFailure (SMPAccessPoint.OT, "set-certificate", sID, "no-such-id");
      return EChange.UNCHANGED;
    }
    if (aAP.hasSameCertificate (sNewCertificate))
      return EChange.UNCHANGED;

    final UpdateResult aUR = getCollection ().updateOne (Filters.eq (BSON_ID, sID),
                                                         Updates.set (BSON_CERTIFICATE, sNewCertificate));
    if (!aUR.wasAcknowledged () || aUR.getMatchedCount () == 0)
      return EChange.UNCHANGED;

    AuditHelper.onAuditModifySuccess (SMPAccessPoint.OT, "set-certificate", sID);
    return EChange.CHANGED;
  }

  @Nullable
  public ISMPAccessPoint getAccessPointOfID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;
    return getCollection ().find (new Document (BSON_ID, sID)).map (SMPAccessPointManagerMongoDB::toDomain).first ();
  }

  @Nullable
  public ISMPAccessPoint getAccessPointOfName (@Nullable final String sName)
  {
    final String sLookupKey = SMPAccessPointHelper.createNameLookupKey (sName);
    if (sLookupKey.isEmpty ())
      return null;
    return getCollection ().find (new Document (BSON_NAME_LOOKUP_KEY, sLookupKey))
                           .map (SMPAccessPointManagerMongoDB::toDomain)
                           .first ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPAccessPoint> getAllAccessPoints ()
  {
    final ICommonsList <ISMPAccessPoint> ret = new CommonsArrayList <> ();
    getCollection ().find ().forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  @Override
  public ICommonsList <ISMPAccessPoint> getAllAccessPoints (@NonNull final IPagingSpec aPagingSpec,
                                                            @Nullable final String sSearchText)
  {
    final ICommonsList <ISMPAccessPoint> ret = new CommonsArrayList <> ();
    if (aPagingSpec.isEmptyPage ())
      return ret;

    final Bson aFilter = SMPMongoQueryHelper.createSearchFilter (COLUMNS, sSearchText);
    final FindIterable <Document> aCursor = aFilter == null ? getCollection ().find ()
                                                            : getCollection ().find (aFilter);
    aCursor.sort (SMPMongoQueryHelper.createSort (COLUMNS, aPagingSpec));
    if (aPagingSpec.getStartIndex () > 0)
      aCursor.skip ((int) Math.min (aPagingSpec.getStartIndex (), Integer.MAX_VALUE));
    if (!aPagingSpec.isUnlimited ())
      aCursor.limit ((int) Math.min (aPagingSpec.getMaxCount (), Integer.MAX_VALUE));
    aCursor.forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @Nonnegative
  public long getAccessPointCount ()
  {
    return getCollection ().countDocuments ();
  }

  @Override
  public long getAccessPointCount (@Nullable final String sSearchText)
  {
    final Bson aFilter = SMPMongoQueryHelper.createSearchFilter (COLUMNS, sSearchText);
    return aFilter == null ? getAccessPointCount () : getCollection ().countDocuments (aFilter);
  }

  @NonNull
  public EChange deleteAccessPoint (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return EChange.UNCHANGED;

    final DeleteResult aDR = getCollection ().deleteOne (new Document (BSON_ID, sID));
    if (!aDR.wasAcknowledged () || aDR.getDeletedCount () == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPAccessPoint.OT, sID, "no-such-id");
      return EChange.UNCHANGED;
    }
    AuditHelper.onAuditDeleteSuccess (SMPAccessPoint.OT, sID);
    return EChange.CHANGED;
  }
}
