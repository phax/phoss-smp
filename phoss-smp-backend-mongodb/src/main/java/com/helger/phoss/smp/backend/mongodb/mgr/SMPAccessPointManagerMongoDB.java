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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPointHelper;
import com.helger.photon.audit.AuditHelper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;

/**
 * Implementation of {@link ISMPAccessPointManager} for MongoDB
 *
 * @author Philip Helger
 */
public final class SMPAccessPointManagerMongoDB extends AbstractManagerMongoDB implements ISMPAccessPointManager
{
  private static final String BSON_ID = "id";
  private static final String BSON_ENDPOINT_REFERENCE = "endpointreference";
  private static final String BSON_CERTIFICATE = "certificate";

  public SMPAccessPointManagerMongoDB ()
  {
    super ("smp-accesspoint");
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
    getCollection ().createIndex (Indexes.ascending (BSON_ENDPOINT_REFERENCE));
  }

  @NonNull
  @ReturnsMutableCopy
  public static Document toBson (@NonNull final ISMPAccessPoint aValue)
  {
    return new Document ().append (BSON_ID, aValue.getID ())
                          .append (BSON_ENDPOINT_REFERENCE, aValue.getEndpointReference ())
                          .append (BSON_CERTIFICATE, aValue.getCertificate ());
  }

  @NonNull
  @ReturnsMutableCopy
  public static SMPAccessPoint toDomain (@NonNull final Document aDoc)
  {
    return new SMPAccessPoint (aDoc.getString (BSON_ID),
                               aDoc.getString (BSON_ENDPOINT_REFERENCE),
                               aDoc.getString (BSON_CERTIFICATE));
  }

  @Nullable
  public ISMPAccessPoint findAccessPoint (@Nullable final String sEndpointReference,
                                          @Nullable final String sCertificate)
  {
    // Note: null and "" must be distinguished, so pre-select by endpoint
    // reference and compare the full content in Java
    final String sLookupKey = SMPAccessPointHelper.createLookupKey (sEndpointReference, sCertificate);
    for (final Document aDoc : getCollection ().find (sEndpointReference == null ? Filters.eq (BSON_ENDPOINT_REFERENCE,
                                                                                               null)
                                                                                 : Filters.eq (BSON_ENDPOINT_REFERENCE,
                                                                                               sEndpointReference)))
    {
      final SMPAccessPoint aAP = toDomain (aDoc);
      if (SMPAccessPointHelper.createLookupKey (aAP).equals (sLookupKey))
        return aAP;
    }
    return null;
  }

  @NonNull
  public ISMPAccessPoint getOrCreateAccessPoint (@Nullable final String sEndpointReference,
                                                 @Nullable final String sCertificate)
  {
    final ISMPAccessPoint aExisting = findAccessPoint (sEndpointReference, sCertificate);
    if (aExisting != null)
      return aExisting;

    final SMPAccessPoint aNew = SMPAccessPoint.createDetached (sEndpointReference, sCertificate);
    if (!getCollection ().insertOne (toBson (aNew)).wasAcknowledged ())
      throw new IllegalStateException ("Failed to insert into MongoDB Collection");

    AuditHelper.onAuditCreateSuccess (SMPAccessPoint.OT, aNew.getID (), sEndpointReference);
    return aNew;
  }

  @Nullable
  public ISMPAccessPoint getAccessPointOfID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;
    return getCollection ().find (new Document (BSON_ID, sID)).map (SMPAccessPointManagerMongoDB::toDomain).first ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPAccessPoint> getAllAccessPoints ()
  {
    final ICommonsList <ISMPAccessPoint> ret = new CommonsArrayList <> ();
    getCollection ().find ().forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @Nonnegative
  public long getAccessPointCount ()
  {
    return getCollection ().countDocuments ();
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

  @Nonnegative
  public long deleteAllUnusedAccessPoints (@NonNull final ICommonsSet <String> aUsedAccessPointIDs)
  {
    final DeleteResult aDR = getCollection ().deleteMany (Filters.nin (BSON_ID, aUsedAccessPointIDs));
    if (!aDR.wasAcknowledged ())
      return 0;
    return aDR.getDeletedCount ();
  }
}
