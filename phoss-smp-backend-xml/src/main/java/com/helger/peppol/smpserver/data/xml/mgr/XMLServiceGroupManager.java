/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
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

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.dao.DAOException;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupCallback;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPSMLException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.smlhook.IRegistrationHook;
import com.helger.phoss.smp.smlhook.RegistrationHookException;
import com.helger.phoss.smp.smlhook.RegistrationHookFactory;
import com.helger.photon.basic.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.basic.audit.AuditHelper;

/**
 * Implementation of {@link ISMPServiceGroupManager} for the XML backend.
 *
 * @author Philip Helger
 */
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
                                                @Nonnull final IParticipantIdentifier aParticipantID,
                                                @Nullable final String sExtension) throws SMPServerException
  {
    ValueEnforcer.notEmpty (sOwnerID, "OwnerID");
    ValueEnforcer.notNull (aParticipantID, "ParticpantID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup (" +
                    sOwnerID +
                    ", " +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

    final SMPServiceGroup aSMPServiceGroup = new SMPServiceGroup (sOwnerID, aParticipantID, sExtension);

    // It's a new service group - throws exception in case of an error
    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    try
    {
      aHook.createServiceGroup (aParticipantID);
    }
    catch (final RegistrationHookException ex)
    {
      throw new SMPSMLException ("Failed to create '" + aParticipantID.getURIEncoded () + "' in SML", ex);
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      internalCreateItem (aSMPServiceGroup);
    }
    catch (final RuntimeException ex)
    {
      // An error occurred - remove from SML again
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createSMPServiceGroup - failure in storing");
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
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditCreateSuccess (SMPServiceGroup.OT,
                                      aSMPServiceGroup.getID (),
                                      sOwnerID,
                                      aParticipantID.getURIEncoded (),
                                      sExtension);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupCreated (aSMPServiceGroup));

    return aSMPServiceGroup;
  }

  @Nonnull
  public EChange updateSMPServiceGroup (@Nonnull final IParticipantIdentifier aParticipantID,
                                        @Nonnull @Nonempty final String sNewOwnerID,
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
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

    final String sServiceGroupID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    final SMPServiceGroup aSMPServiceGroup = getOfID (sServiceGroupID);
    if (aSMPServiceGroup == null)
    {
      AuditHelper.onAuditModifyFailure (SMPServiceGroup.OT, "no-such-id", sServiceGroupID);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("updateSMPServiceGroup - failure");
      throw new SMPNotFoundException ("No such service group '" + sServiceGroupID + "'");
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

    AuditHelper.onAuditModifySuccess (SMPServiceGroup.OT, "all", sServiceGroupID, sNewOwnerID, sExtension);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupUpdated (aParticipantID));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nonnull final IParticipantIdentifier aParticipantID) throws SMPServerException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup (" + aParticipantID.getURIEncoded () + ")");

    final String sServiceGroupID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    final SMPServiceGroup aSMPServiceGroup = getOfID (sServiceGroupID);
    if (aSMPServiceGroup == null)
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceGroup.OT, "no-such-id", aParticipantID);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPServiceGroup - failure");
      throw new SMPNotFoundException ("No such service group '" + aParticipantID.getURIEncoded () + "'");
    }

    // Delete in SML - throws exception in case of error
    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    try
    {
      aHook.deleteServiceGroup (aParticipantID);
    }
    catch (final RegistrationHookException ex)
    {
      throw new SMPSMLException ("Failed to delete '" + aParticipantID.getURIEncoded () + "' in SML", ex);
    }

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

        // Restore in SML again
        try
        {
          aHook.undoDeleteServiceGroup (aParticipantID);
        }
        catch (final RegistrationHookException ex2)
        {
          LOGGER.error ("Failed to undoDeleteServiceGroup (" + aParticipantID.getURIEncoded () + ")", ex2);
        }
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
      try
      {
        aHook.undoDeleteServiceGroup (aParticipantID);
      }
      catch (final RegistrationHookException ex2)
      {
        LOGGER.error ("Failed to undoDeleteServiceGroup (" + aParticipantID.getURIEncoded () + ")", ex2);
      }
      throw ex;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditDeleteSuccess (SMPServiceGroup.OT, aSMPServiceGroup.getID ());
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup - success");

    m_aCBs.forEach (x -> x.onSMPServiceGroupDeleted (aParticipantID));

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

  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return null;

    final String sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    return getOfID (sID);
  }

  public boolean containsSMPServiceGroupWithID (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return false;

    final String sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    return containsWithID (sID);
  }

  @Nonnegative
  public int getSMPServiceGroupCount ()
  {
    return size ();
  }
}
