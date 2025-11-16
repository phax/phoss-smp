/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupCallback;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPSMLException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.smlhook.IRegistrationHook;
import com.helger.phoss.smp.smlhook.RegistrationHookException;
import com.helger.phoss.smp.smlhook.RegistrationHookFactory;
import com.helger.photon.audit.AuditHelper;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;

/**
 * Implementation of {@link ISMPServiceGroupManager} for the XML backend.
 *
 * @author Philip Helger
 */
public final class SMPServiceGroupManagerMongoDB extends AbstractManagerMongoDB implements ISMPServiceGroupManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPServiceGroupManagerMongoDB.class);
  private static final String BSON_ID = "id";
  private static final String BSON_OWNER_ID = "ownerid";
  private static final String BSON_PARTICIPANT_ID = "participantid";
  private static final String BSON_EXTENSION = "extension";

  private final CallbackList <ISMPServiceGroupCallback> m_aCBs = new CallbackList <> ();

  public SMPServiceGroupManagerMongoDB ()
  {
    super ("smp-servicegroup");
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
  }

  @NonNull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceGroupCallback> serviceGroupCallbacks ()
  {
    return m_aCBs;
  }

  @NonNull
  @ReturnsMutableCopy
  public static Document toBson (@NonNull final ISMPServiceGroup aValue)
  {
    final Document ret = new Document ().append (BSON_ID, aValue.getID ())
                                        .append (BSON_OWNER_ID, aValue.getOwnerID ())
                                        .append (BSON_PARTICIPANT_ID, toBson (aValue.getParticipantIdentifier ()));
    if (aValue.getExtensions ().extensions ().isNotEmpty ())
      ret.append (BSON_EXTENSION, aValue.getExtensions ().getExtensionsAsJsonString ());
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public static SMPServiceGroup toDomain (@NonNull final Document aDoc)
  {
    final String sOwnerID = aDoc.getString (BSON_OWNER_ID);
    final IParticipantIdentifier aParticipantIdentifier = toParticipantID (aDoc.get (BSON_PARTICIPANT_ID,
                                                                                     Document.class));
    final String sExtension = aDoc.getString (BSON_EXTENSION);
    return new SMPServiceGroup (sOwnerID, aParticipantIdentifier, sExtension);
  }

  @NonNull
  public SMPServiceGroup createSMPServiceGroup (@NonNull @Nonempty final String sOwnerID,
                                                @NonNull final IParticipantIdentifier aParticipantID,
                                                @Nullable final String sExtension,
                                                final boolean bCreateInSML) throws SMPServerException
  {
    ValueEnforcer.notEmpty (sOwnerID, "OwnerID");
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup (" +
                    sOwnerID +
                    ", " +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    (StringHelper.isNotEmpty (sExtension) ? "with extension" : "without extension") +
                    ", " +
                    bCreateInSML +
                    ")");

    final SMPServiceGroup aSMPServiceGroup = new SMPServiceGroup (sOwnerID, aParticipantID, sExtension);

    // It's a new service group - throws exception in case of an error
    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    if (bCreateInSML)
      try
      {
        aHook.createServiceGroup (aParticipantID);
      }
      catch (final RegistrationHookException ex)
      {
        throw new SMPSMLException ("Failed to create '" + aParticipantID.getURIEncoded () + "' in SML", ex);
      }

    try
    {
      if (!getCollection ().insertOne (toBson (aSMPServiceGroup)).wasAcknowledged ())
        throw new IllegalStateException ("Failed to insert into MongoDB Collection");
    }
    catch (final RuntimeException ex)
    {
      // An error occurred - remove from SML again
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createSMPServiceGroup - failure in storing");

      if (bCreateInSML)
        try
        {
          aHook.undoCreateServiceGroup (aParticipantID);
        }
        catch (final RegistrationHookException ex2)
        {
          LOGGER.error ("Failed to undoCreateServiceGroup (" + aParticipantID.getURIEncoded () + ")", ex2);
        }
      throw ex;
    }

    AuditHelper.onAuditCreateSuccess (SMPServiceGroup.OT,
                                      aSMPServiceGroup.getID (),
                                      sOwnerID,
                                      aParticipantID.getURIEncoded (),
                                      sExtension,
                                      Boolean.valueOf (bCreateInSML));
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupCreated (aSMPServiceGroup, bCreateInSML));

    return aSMPServiceGroup;
  }

  @NonNull
  public EChange updateSMPServiceGroup (@NonNull final IParticipantIdentifier aParticipantID,
                                        @NonNull @Nonempty final String sNewOwnerID,
                                        @Nullable final String sExtension) throws SMPServerException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notEmpty (sNewOwnerID, "NewOwnerID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup (" +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    sNewOwnerID +
                    ", " +
                    (StringHelper.isNotEmpty (sExtension) ? "with extension" : "without extension") +
                    ")");

    final String sServiceGroupID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    final Document aOldDoc = getCollection ().findOneAndUpdate (new Document (BSON_ID, sServiceGroupID),
                                                                Updates.combine (Updates.set (BSON_OWNER_ID,
                                                                                              sNewOwnerID),
                                                                                 Updates.set (BSON_EXTENSION,
                                                                                              sExtension)));
    if (aOldDoc == null)
    {
      AuditHelper.onAuditModifyFailure (SMPServiceGroup.OT, "set-all", sServiceGroupID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (SMPServiceGroup.OT, "set-all", sServiceGroupID, sNewOwnerID, sExtension);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupUpdated (aParticipantID));

    return EChange.CHANGED;
  }

  @NonNull
  public EChange deleteSMPServiceGroup (@NonNull final IParticipantIdentifier aParticipantID,
                                        final boolean bDeleteInSML) throws SMPServerException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup (" + aParticipantID.getURIEncoded () + ", " + bDeleteInSML + ")");

    // Check first in memory, to avoid unnecessary deletion
    final ISMPServiceGroup aServiceGroup = getSMPServiceGroupOfID (aParticipantID);
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    if (bDeleteInSML)
    {
      // Delete in SML - throws exception in case of error
      try
      {
        aHook.deleteServiceGroup (aParticipantID);
      }
      catch (final RegistrationHookException ex)
      {
        throw new SMPSMLException ("Failed to delete '" + aParticipantID.getURIEncoded () + "' in SML", ex);
      }
    }

    // Delete all redirects (must be done before the SG is deleted)
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    aRedirectMgr.deleteAllSMPRedirectsOfServiceGroup (aParticipantID);

    // Delete all service information (must be done before the SG is deleted)
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    aServiceInfoMgr.deleteAllSMPServiceInformationOfServiceGroup (aParticipantID);

    final String sServiceGroupID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    final DeleteResult aDR = getCollection ().deleteOne (new Document (BSON_ID, sServiceGroupID));
    if (!aDR.wasAcknowledged () || aDR.getDeletedCount () == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceGroup.OT, aParticipantID, "no-such-id");
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPServiceGroup - failure");

      // restore in SML
      if (bDeleteInSML)
      {
        // Undo deletion in SML!
        try
        {
          aHook.undoDeleteServiceGroup (aParticipantID);
        }
        catch (final RegistrationHookException ex)
        {
          LOGGER.error ("Failed to undoDeleteServiceGroup (" + aParticipantID.getURIEncoded () + ")", ex);
        }
      }

      throw new SMPNotFoundException ("No such service group '" + aParticipantID.getURIEncoded () + "'");
    }

    AuditHelper.onAuditDeleteSuccess (SMPServiceGroup.OT, aParticipantID, Boolean.valueOf (bDeleteInSML));
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupDeleted (aParticipantID, bDeleteInSML));

    return EChange.CHANGED;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    final ICommonsList <ISMPServiceGroup> ret = new CommonsArrayList <> ();
    getCollection ().find ().forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllSMPServiceGroupIDs ()
  {
    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    getCollection ().find ().forEach (x -> ret.add (x.getString (BSON_OWNER_ID)));
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (@NonNull final String sOwnerID)
  {
    final ICommonsList <ISMPServiceGroup> ret = new CommonsArrayList <> ();
    getCollection ().find (new Document (BSON_OWNER_ID, sOwnerID)).forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @Nonnegative
  public long getSMPServiceGroupCountOfOwner (@NonNull final String sOwnerID)
  {
    return getCollection ().countDocuments (new Document (BSON_OWNER_ID, sOwnerID));
  }

  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return null;

    final String sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    return getCollection ().find (new Document (BSON_ID, sID)).map (SMPServiceGroupManagerMongoDB::toDomain).first ();
  }

  public boolean containsSMPServiceGroupWithID (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return false;

    final String sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    return getCollection ().find (new Document (BSON_ID, sID)).first () != null;
  }

  @Nonnegative
  public long getSMPServiceGroupCount ()
  {
    return getCollection ().countDocuments ();
  }
}
