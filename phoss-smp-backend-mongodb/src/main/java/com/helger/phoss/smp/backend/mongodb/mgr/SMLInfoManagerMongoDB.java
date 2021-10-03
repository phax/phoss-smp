/**
 * Copyright (C) 2019-2021 Philip Helger and contributors
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

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.sml.CSMLDefault;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.photon.audit.AuditHelper;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;

/**
 * Implementation of {@link ISMLInfoManager} for MongoDB
 *
 * @author Philip Helger
 */
public class SMLInfoManagerMongoDB extends AbstractManagerMongoDB implements ISMLInfoManager
{
  private static final String BSON_ID = "id";
  private static final String BSON_DISPLAYNAME = "displayname";
  private static final String BSON_DNSZONE = "dnszone";
  private static final String BSON_SERVICEURL = "serviceurl";
  private static final String BSON_CLIENTCERT = "clientcert";

  public SMLInfoManagerMongoDB ()
  {
    super ("smp-smlinfo");
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final ISMLInfo aValue)
  {
    return new Document ().append (BSON_ID, aValue.getID ())
                          .append (BSON_DISPLAYNAME, aValue.getDisplayName ())
                          .append (BSON_DNSZONE, aValue.getDNSZone ())
                          .append (BSON_SERVICEURL, aValue.getManagementServiceURL ())
                          .append (BSON_CLIENTCERT, Boolean.valueOf (aValue.isClientCertificateRequired ()));
  }

  @Nonnull
  @ReturnsMutableCopy
  public static SMLInfo toDomain (@Nonnull final Document aDoc)
  {
    return new SMLInfo (aDoc.getString (BSON_ID),
                        aDoc.getString (BSON_DISPLAYNAME),
                        aDoc.getString (BSON_DNSZONE),
                        aDoc.getString (BSON_SERVICEURL),
                        aDoc.getBoolean (BSON_CLIENTCERT).booleanValue ());
  }

  @Nonnull
  public ISMLInfo createSMLInfo (@Nonnull @Nonempty final String sDisplayName,
                                 @Nonnull @Nonempty final String sDNSZone,
                                 @Nonnull @Nonempty final String sManagementServiceURL,
                                 final boolean bClientCertificateRequired)
  {
    final SMLInfo aSMLInfo = new SMLInfo (sDisplayName, sDNSZone, sManagementServiceURL, bClientCertificateRequired);

    if (!getCollection ().insertOne (toBson (aSMLInfo)).wasAcknowledged ())
      throw new IllegalStateException ("Failed to insert into MongoDB Collection");

    AuditHelper.onAuditCreateSuccess (SMLInfo.OT,
                                      aSMLInfo.getID (),
                                      sDisplayName,
                                      sDNSZone,
                                      sManagementServiceURL,
                                      Boolean.valueOf (bClientCertificateRequired));
    return aSMLInfo;
  }

  @Nonnull
  public EChange updateSMLInfo (@Nullable final String sSMLInfoID,
                                @Nonnull @Nonempty final String sDisplayName,
                                @Nonnull @Nonempty final String sDNSZone,
                                @Nonnull @Nonempty final String sManagementServiceURL,
                                final boolean bClientCertificateRequired)
  {
    final Document aOldDoc = getCollection ().findOneAndUpdate (new Document (BSON_ID, sSMLInfoID),
                                                                Updates.combine (Updates.set (BSON_DISPLAYNAME, sDisplayName),
                                                                                 Updates.set (BSON_DNSZONE, sDNSZone),
                                                                                 Updates.set (BSON_SERVICEURL, sManagementServiceURL),
                                                                                 Updates.set (BSON_CLIENTCERT,
                                                                                              Boolean.valueOf (bClientCertificateRequired))));
    if (aOldDoc == null)
      return EChange.UNCHANGED;

    AuditHelper.onAuditModifySuccess (SMLInfo.OT,
                                      "set-all",
                                      sSMLInfoID,
                                      sDisplayName,
                                      sDNSZone,
                                      sManagementServiceURL,
                                      Boolean.valueOf (bClientCertificateRequired));
    return EChange.CHANGED;
  }

  @Nullable
  public EChange deleteSMLInfo (@Nullable final String sSMLInfoID)
  {
    if (StringHelper.hasNoText (sSMLInfoID))
      return EChange.UNCHANGED;

    final DeleteResult aDR = getCollection ().deleteOne (new Document (BSON_ID, sSMLInfoID));
    if (!aDR.wasAcknowledged () || aDR.getDeletedCount () == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMLInfo.OT, sSMLInfoID, "no-such-id");
      return EChange.UNCHANGED;
    }
    AuditHelper.onAuditDeleteSuccess (SMLInfo.OT, sSMLInfoID);
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMLInfo> getAllSMLInfos ()
  {
    final ICommonsList <ISMLInfo> ret = new CommonsArrayList <> ();
    getCollection ().find ().forEach ((Consumer <Document>) x -> ret.add (toDomain (x)));
    return ret;
  }

  @Nullable
  public ISMLInfo getSMLInfoOfID (@Nullable final String sID)
  {
    return getCollection ().find (new Document (BSON_ID, sID)).map (SMLInfoManagerMongoDB::toDomain).first ();
  }

  public boolean containsSMLInfoWithID (@Nullable final String sID)
  {
    return getCollection ().find (new Document (BSON_ID, sID)).first () != null;
  }

  @Nullable
  public ISMLInfo findFirstWithManageParticipantIdentifierEndpointAddress (@Nullable final String sAddress)
  {
    if (StringHelper.hasNoText (sAddress))
      return null;

    // The stored field does not contain the suffix
    final String sSearchAddress = StringHelper.trimEnd (sAddress, '/' + CSMLDefault.MANAGEMENT_SERVICE_PARTICIPANTIDENTIFIER);
    return getCollection ().find (new Document (BSON_SERVICEURL, sSearchAddress)).map (SMLInfoManagerMongoDB::toDomain).first ();
  }
}
