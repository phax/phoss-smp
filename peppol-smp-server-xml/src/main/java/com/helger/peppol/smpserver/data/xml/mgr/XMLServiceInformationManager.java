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

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.dao.DAOException;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;
import com.helger.photon.basic.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.basic.audit.AuditHelper;

/**
 * Manager for all {@link SMPServiceInformation} objects.
 *
 * @author Philip Helger
 */
public final class XMLServiceInformationManager extends
                                                AbstractPhotonMapBasedWALDAO <ISMPServiceInformation, SMPServiceInformation>
                                                implements
                                                ISMPServiceInformationManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (XMLServiceInformationManager.class);

  public XMLServiceInformationManager (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPServiceInformation.class, sFilename);
  }

  @Nullable
  public ISMPServiceInformation findServiceInformation (@Nullable final ISMPServiceGroup aServiceGroup,
                                                        @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                                        @Nullable final IProcessIdentifier aProcessID,
                                                        @Nullable final ISMPTransportProfile aTransportProfile)
  {
    final ISMPServiceInformation aServiceInfo = getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                       aDocTypeID);
    if (aServiceInfo != null)
    {
      final ISMPProcess aProcess = aServiceInfo.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        final ISMPEndpoint aEndpoint = aProcess.getEndpointOfTransportProfile (aTransportProfile);
        if (aEndpoint != null)
          return aServiceInfo;
      }
    }
    return null;
  }

  @Nonnull
  public ESuccess mergeSMPServiceInformation (@Nonnull final ISMPServiceInformation aSMPServiceInformationObj)
  {
    final SMPServiceInformation aSMPServiceInformation = (SMPServiceInformation) aSMPServiceInformationObj;
    ValueEnforcer.notNull (aSMPServiceInformation, "ServiceInformation");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("mergeSMPServiceInformation (" + aSMPServiceInformationObj + ")");

    // Check for an update
    boolean bChangedExisting = false;
    final SMPServiceInformation aOldInformation = (SMPServiceInformation) getSMPServiceInformationOfServiceGroupAndDocumentType (aSMPServiceInformation.getServiceGroup (),
                                                                                                                                 aSMPServiceInformation.getDocumentTypeIdentifier ());
    if (aOldInformation != null)
    {
      // If a service information is present, it must be the provided object!
      // This is not true for the REST API
      if (aOldInformation == aSMPServiceInformation)
        bChangedExisting = true;
    }

    if (bChangedExisting)
    {
      // Edit existing
      m_aRWLock.writeLocked ( () -> {
        internalUpdateItem (aOldInformation);
      });

      AuditHelper.onAuditModifySuccess (SMPServiceInformation.OT,
                                        aOldInformation.getID (),
                                        aOldInformation.getServiceGroupID (),
                                        aOldInformation.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aOldInformation.getAllProcesses (),
                                        aOldInformation.getExtensionAsString ());

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("mergeSMPServiceInformation - success - updated");
    }
    else
    {
      // (Optionally delete the old one and) create the new one
      boolean bRemovedOld = false;
      m_aRWLock.writeLock ().lock ();
      try
      {
        if (aOldInformation != null)
          bRemovedOld = internalDeleteItem (aOldInformation.getID ()) == aOldInformation;

        internalCreateItem (aSMPServiceInformation);
      }
      finally
      {
        m_aRWLock.writeLock ().unlock ();
      }

      if (bRemovedOld)
      {
        AuditHelper.onAuditDeleteSuccess (SMPServiceInformation.OT,
                                          aOldInformation.getID (),
                                          aOldInformation.getServiceGroupID (),
                                          aOldInformation.getDocumentTypeIdentifier ().getURIEncoded ());
      }
      else
        if (aOldInformation != null)
        {
          AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT,
                                            aOldInformation.getID (),
                                            aOldInformation.getServiceGroupID (),
                                            aOldInformation.getDocumentTypeIdentifier ().getURIEncoded ());
        }

      AuditHelper.onAuditCreateSuccess (SMPServiceInformation.OT,
                                        aSMPServiceInformation.getID (),
                                        aSMPServiceInformation.getServiceGroupID (),
                                        aSMPServiceInformation.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aSMPServiceInformation.getAllProcesses (),
                                        aSMPServiceInformation.getExtensionAsString ());
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("mergeSMPServiceInformation - success - created");
    }
    return ESuccess.SUCCESS;
  }

  @Nonnull
  public EChange deleteSMPServiceInformation (@Nullable final ISMPServiceInformation aSMPServiceInformation)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceInformation (" + aSMPServiceInformation + ")");

    if (aSMPServiceInformation == null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPServiceInformation - failure");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPServiceInformation aRealServiceInformation = internalDeleteItem (aSMPServiceInformation.getID ());
      if (aRealServiceInformation == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, "no-such-id", aSMPServiceInformation.getID ());
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("deleteSMPServiceInformation - failure");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditDeleteSuccess (SMPServiceInformation.OT, aSMPServiceInformation.getID ());
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceInformation - success");

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    EChange eChange = EChange.UNCHANGED;
    for (final ISMPServiceInformation aSMPServiceInformation : getAllSMPServiceInformationOfServiceGroup (aServiceGroup))
      eChange = eChange.or (deleteSMPServiceInformation (aSMPServiceInformation));
    return eChange;
  }

  @Nonnull
  public EChange deleteSMPProcess (@Nullable final ISMPServiceInformation aSMPServiceInformation,
                                   @Nullable final ISMPProcess aProcess)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPProcess (" + aSMPServiceInformation + ", " + aProcess + ")");

    if (aSMPServiceInformation == null || aProcess == null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPProcess - failure");
      return EChange.UNCHANGED;
    }

    // Find implementation object
    final SMPServiceInformation aRealServiceInformation = getOfID (aSMPServiceInformation.getID ());
    if (aRealServiceInformation == null)
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, "no-such-id", aSMPServiceInformation.getID ());
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPProcess - failure");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      // Main deletion in write lock
      if (aRealServiceInformation.deleteProcess (aProcess).isUnchanged ())
      {
        AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT,
                                          "no-such-process",
                                          aSMPServiceInformation.getID (),
                                          aProcess.getProcessIdentifier ().getURIEncoded ());
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("deleteSMPProcess - failure");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (SMPServiceInformation.OT,
                                      aSMPServiceInformation.getID (),
                                      aProcess.getProcessIdentifier ().getURIEncoded ());
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPProcess - success");
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation ()
  {
    return getAll ();
  }

  @Nonnegative
  public int getSMPServiceInformationCount ()
  {
    return size ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    if (aServiceGroup != null)
      findAll (x -> x.getServiceGroupID ().equals (aServiceGroup.getID ()), ret::add);
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final ICommonsList <IDocumentTypeIdentifier> ret = new CommonsArrayList <> ();
    if (aServiceGroup != null)
    {
      findAllMapped (aSI -> aSI.getServiceGroupID ().equals (aServiceGroup.getID ()),
                     aSI -> aSI.getDocumentTypeIdentifier (),
                     ret::add);
    }
    return ret;
  }

  @Nullable
  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                                       @Nullable final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    if (aServiceGroup == null)
      return null;
    if (aDocumentTypeIdentifier == null)
      return null;

    final ICommonsList <ISMPServiceInformation> ret = getAll (aSI -> aSI.getServiceGroupID ()
                                                                        .equals (aServiceGroup.getID ()) &&
                                                                     aSI.getDocumentTypeIdentifier ()
                                                                        .hasSameContent (aDocumentTypeIdentifier));

    if (ret.isEmpty ())
      return null;
    if (ret.size () > 1)
      LOGGER.warn ("Found more than one entry for service group '" +
                   aServiceGroup.getID () +
                   "' and document type '" +
                   aDocumentTypeIdentifier.getValue () +
                   "'. This seems to be a bug! Using the first one.");
    return ret.getFirst ();
  }
}
