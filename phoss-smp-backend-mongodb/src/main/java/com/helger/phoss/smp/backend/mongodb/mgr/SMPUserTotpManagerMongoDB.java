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

import java.time.LocalDateTime;
import java.util.Date;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.domain.totp.ISMPUserTotp;
import com.helger.phoss.smp.domain.totp.ISMPUserTotpManager;
import com.helger.phoss.smp.domain.totp.SMPUserTotp;
import com.helger.photon.audit.AuditHelper;
import com.helger.typeconvert.impl.TypeConverter;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;

/**
 * Implementation of {@link ISMPUserTotpManager} for MongoDB
 *
 * @author Philip Helger
 * @since 8.4.3
 */
public class SMPUserTotpManagerMongoDB extends AbstractManagerMongoDB implements ISMPUserTotpManager
{
  private static final String BSON_USER_ID = "userid";
  private static final String BSON_SECRET = "secret";
  private static final String BSON_ENABLED = "enabled";
  private static final String BSON_REGISTRATION_DT = "regdt";
  private static final String BSON_LAST_USED_TIME_SLOT = "lastslot";

  public SMPUserTotpManagerMongoDB ()
  {
    super ("smp-user-totp");
    getCollection ().createIndex (Indexes.ascending (BSON_USER_ID));
  }

  @NonNull
  @ReturnsMutableCopy
  public static Document toBson (@NonNull final ISMPUserTotp aValue)
  {
    final Document ret = new Document ().append (BSON_USER_ID, aValue.getID ())
                                        .append (BSON_SECRET, aValue.getSecret ())
                                        .append (BSON_ENABLED, Boolean.valueOf (aValue.isEnabled ()))
                                        .append (BSON_REGISTRATION_DT,
                                                 TypeConverter.convert (aValue.getRegistrationDateTime (), Date.class));
    if (aValue.hasLastUsedTimeSlot ())
      ret.append (BSON_LAST_USED_TIME_SLOT, aValue.getLastUsedTimeSlot ());
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public static SMPUserTotp toDomain (@NonNull final Document aDoc)
  {
    final String sUserID = aDoc.getString (BSON_USER_ID);
    final String sSecret = aDoc.getString (BSON_SECRET);
    final Boolean aEnabled = aDoc.getBoolean (BSON_ENABLED);
    final LocalDateTime aRegistrationDT = TypeConverter.convert (aDoc.getDate (BSON_REGISTRATION_DT),
                                                                 LocalDateTime.class);
    final Long aLastUsedTimeSlot = aDoc.getLong (BSON_LAST_USED_TIME_SLOT);
    return new SMPUserTotp (sUserID,
                            sSecret,
                            aEnabled != null && aEnabled.booleanValue (),
                            aRegistrationDT,
                            aLastUsedTimeSlot);
  }

  @NonNull
  public ISMPUserTotp createOrReplaceTotp (@NonNull @Nonempty final String sUserID,
                                           @NonNull @Nonempty final String sSecret)
  {
    final SMPUserTotp aTotp = SMPUserTotp.createPending (sUserID, sSecret);

    // An existing enrollment - confirmed or not - is always replaced
    getCollection ().deleteOne (new Document (BSON_USER_ID, sUserID));
    if (!getCollection ().insertOne (toBson (aTotp)).wasAcknowledged ())
      throw new IllegalStateException ("Failed to insert into MongoDB Collection");

    // Never audit the secret itself
    AuditHelper.onAuditCreateSuccess (SMPUserTotp.OT, sUserID);
    return aTotp;
  }

  @NonNull
  public EChange setTotpEnabled (@Nullable final String sUserID, final boolean bEnabled)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final Document aOldDoc = getCollection ().findOneAndUpdate (new Document (BSON_USER_ID, sUserID),
                                                                Updates.set (BSON_ENABLED,
                                                                             Boolean.valueOf (bEnabled)));
    if (aOldDoc == null)
    {
      AuditHelper.onAuditModifyFailure (SMPUserTotp.OT, "set-enabled", sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    final Boolean aOldEnabled = aOldDoc.getBoolean (BSON_ENABLED);
    if (aOldEnabled != null && aOldEnabled.booleanValue () == bEnabled)
      return EChange.UNCHANGED;

    AuditHelper.onAuditModifySuccess (SMPUserTotp.OT, "set-enabled", sUserID, Boolean.valueOf (bEnabled));
    return EChange.CHANGED;
  }

  @NonNull
  public EChange setTotpLastUsedTimeSlot (@Nullable final String sUserID, final long nTimeSlot)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    // Deliberately not audited - this happens on every single login
    final Document aOldDoc = getCollection ().findOneAndUpdate (new Document (BSON_USER_ID, sUserID),
                                                                Updates.set (BSON_LAST_USED_TIME_SLOT,
                                                                             Long.valueOf (nTimeSlot)));
    return EChange.valueOf (aOldDoc != null);
  }

  @NonNull
  public EChange deleteTotp (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final DeleteResult aDR = getCollection ().deleteOne (new Document (BSON_USER_ID, sUserID));
    if (!aDR.wasAcknowledged () || aDR.getDeletedCount () == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPUserTotp.OT, sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (SMPUserTotp.OT, sUserID);
    return EChange.CHANGED;
  }

  @Nullable
  public ISMPUserTotp getTotpOfUserID (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return null;

    return getCollection ().find (new Document (BSON_USER_ID, sUserID))
                           .map (SMPUserTotpManagerMongoDB::toDomain)
                           .first ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPUserTotp> getAllTotps ()
  {
    final ICommonsList <ISMPUserTotp> ret = new CommonsArrayList <> ();
    getCollection ().find ().forEach (x -> ret.add (toDomain (x)));
    return ret;
  }
}
