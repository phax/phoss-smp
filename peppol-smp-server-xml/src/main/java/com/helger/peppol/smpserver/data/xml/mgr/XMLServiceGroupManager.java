/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.xml.mgr;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.dao.DAOException;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupCallback;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.smlhook.IRegistrationHook;
import com.helger.peppol.smpserver.smlhook.RegistrationHookFactory;
import com.helger.photon.basic.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.basic.audit.AuditHelper;

public final class XMLServiceGroupManager extends AbstractPhotonMapBasedWALDAO <ISMPServiceGroup, SMPServiceGroup>
                                          implements
                                          ISMPServiceGroupManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (XMLServiceGroupManager.class);

  private final CallbackList <ISMPServiceGroupCallback> m_aCBs = new CallbackList <> ();

  public XMLServiceGroupManager (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPServiceGroup.class, sFilename);
  }

  @Nonnull
  @ReturnsMutableObject ("by design")
  public CallbackList <ISMPServiceGroupCallback> serviceGroupCallbacks ()
  {
    return m_aCBs;
  }

  @Nonnull
  public SMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty final String sOwnerID,
                                                @Nonnull final IParticipantIdentifier aParticipantIdentifier,
                                                @Nullable final String sExtension)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup (" +
                    sOwnerID +
                    ", " +
                    aParticipantIdentifier.getURIEncoded () +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

    final SMPServiceGroup aSMPServiceGroup = new SMPServiceGroup (sOwnerID, aParticipantIdentifier, sExtension);

    // It's a new service group - throws exception in case of an error
    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    aHook.createServiceGroup (aParticipantIdentifier);

    m_aRWLock.writeLock ().lock ();
    try
    {
      internalCreateItem (aSMPServiceGroup);
    }
    catch (final RuntimeException ex)
    {
      // An error occurred - remove from SML again
      aHook.undoCreateServiceGroup (aParticipantIdentifier);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createSMPServiceGroup - failure");
      throw ex;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditCreateSuccess (SMPServiceGroup.OT,
                                      aSMPServiceGroup.getID (),
                                      sOwnerID,
                                      aParticipantIdentifier.getURIEncoded (),
                                      sExtension);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupCreated (aSMPServiceGroup));

    return aSMPServiceGroup;
  }

  @Nonnull
  public EChange updateSMPServiceGroup (@Nullable final String sSMPServiceGroupID,
                                        @Nonnull @Nonempty final String sNewOwnerID,
                                        @Nullable final String sExtension)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup (" +
                    sSMPServiceGroupID +
                    ", " +
                    sNewOwnerID +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

    final SMPServiceGroup aSMPServiceGroup = getOfID (sSMPServiceGroupID);
    if (aSMPServiceGroup == null)
    {
      AuditHelper.onAuditModifyFailure (SMPServiceGroup.OT, sSMPServiceGroupID, "no-such-id");
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("updateSMPServiceGroup - failure");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aSMPServiceGroup.setOwnerID (sNewOwnerID));
      eChange = eChange.or (aSMPServiceGroup.setExtensionAsString (sExtension));
      if (eChange.isUnchanged ())
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("updateSMPServiceGroup - unchanged");
        return EChange.UNCHANGED;
      }
      internalUpdateItem (aSMPServiceGroup);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditModifySuccess (SMPServiceGroup.OT, "all", sSMPServiceGroupID, sNewOwnerID, sExtension);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupUpdated (sSMPServiceGroupID));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup (" +
                    (aParticipantID == null ? "null" : aParticipantID.getURIEncoded ()) +
                    ")");

    final String sServiceGroupID = aParticipantID == null ? null
                                                          : SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    final SMPServiceGroup aSMPServiceGroup = getOfID (sServiceGroupID);
    if (aSMPServiceGroup == null)
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceGroup.OT, "no-such-id", aParticipantID);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPServiceGroup - failure");
      return EChange.UNCHANGED;
    }

    // Delete in SML - throws exception in case of error
    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    aHook.deleteServiceGroup (aSMPServiceGroup.getParticpantIdentifier ());

    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    ICommonsList <ISMPRedirect> aOldRedirects = null;
    ICommonsList <ISMPServiceInformation> aOldServiceInformation = null;

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (internalDeleteItem (aSMPServiceGroup.getID ()) == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPServiceGroup.OT, "no-such-id", aSMPServiceGroup.getID ());
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("deleteSMPServiceGroup - failure");
        return EChange.UNCHANGED;
      }

      // Remember all redirects (in case of an error) and delete them
      aOldRedirects = aRedirectMgr.getAllSMPRedirectsOfServiceGroup (aSMPServiceGroup);
      aRedirectMgr.deleteAllSMPRedirectsOfServiceGroup (aSMPServiceGroup);

      // Remember all service information (in case of an error) and delete them
      aOldServiceInformation = aServiceInfoMgr.getAllSMPServiceInformationOfServiceGroup (aSMPServiceGroup);
      aServiceInfoMgr.deleteAllSMPServiceInformationOfServiceGroup (aSMPServiceGroup);
    }
    catch (final RuntimeException ex)
    {
      // Deletion failed - shit

      // Try to rollback the actions
      if (!containsWithID (aSMPServiceGroup.getID ()))
        internalCreateItem (aSMPServiceGroup);

      // Restore redirects (if any)
      if (aOldRedirects != null)
        for (final ISMPRedirect aOldRedirect : aOldRedirects)
        {
          // ignore return value - we cannot do anything anyway
          aRedirectMgr.createOrUpdateSMPRedirect (aSMPServiceGroup,
                                                  aOldRedirect.getDocumentTypeIdentifier (),
                                                  aOldRedirect.getTargetHref (),
                                                  aOldRedirect.getSubjectUniqueIdentifier (),
                                                  aOldRedirect.getExtensionAsString ());
        }

      // Restore service information (if any)
      if (aOldServiceInformation != null)
        for (final ISMPServiceInformation aOldServiceInfo : aOldServiceInformation)
        {
          // ignore return value - we cannot do anything anyway
          aServiceInfoMgr.mergeSMPServiceInformation (aOldServiceInfo);
        }

      // An error occurred - restore in SML again
      aHook.undoDeleteServiceGroup (aSMPServiceGroup.getParticpantIdentifier ());
      throw ex;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditDeleteSuccess (SMPServiceGroup.OT, aSMPServiceGroup.getID ());
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupDeleted (sServiceGroupID));

    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    return getAll ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (@Nonnull final String sOwnerID)
  {
    return getAll (x -> x.getOwnerID ().equals (sOwnerID));
  }

  @Nonnegative
  public int getSMPServiceGroupCountOfOwner (@Nonnull final String sOwnerID)
  {
    return getCount (x -> x.getOwnerID ().equals (sOwnerID));
  }

  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (aParticipantIdentifier == null)
      return null;

    final String sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantIdentifier);
    return getOfID (sID);
  }

  public boolean containsSMPServiceGroupWithID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (aParticipantIdentifier == null)
      return false;

    final String sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantIdentifier);
    return containsWithID (sID);
  }

  @Nonnegative
  public int getSMPServiceGroupCount ()
  {
    return size ();
  }
}
