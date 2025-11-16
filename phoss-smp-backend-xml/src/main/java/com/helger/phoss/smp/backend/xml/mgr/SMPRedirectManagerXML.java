/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.xml.mgr;

import java.security.cert.X509Certificate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.ELockType;
import com.helger.annotation.concurrent.IsLocked;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.dao.DAOException;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectCallback;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.redirect.SMPRedirect;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

/**
 * Manager for all {@link SMPRedirect} objects.
 *
 * @author Philip Helger
 */
public final class SMPRedirectManagerXML extends AbstractPhotonMapBasedWALDAO <ISMPRedirect, SMPRedirect> implements
                                         ISMPRedirectManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPRedirectManagerXML.class);

  private final CallbackList <ISMPRedirectCallback> m_aCallbacks = new CallbackList <> ();

  public SMPRedirectManagerXML (@NonNull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPRedirect.class, sFilename);
  }

  @NonNull
  @ReturnsMutableObject
  public CallbackList <ISMPRedirectCallback> redirectCallbacks ()
  {
    return m_aCallbacks;
  }

  @NonNull
  @IsLocked (ELockType.WRITE)
  private ISMPRedirect _createSMPRedirect (@NonNull final SMPRedirect aSMPRedirect)
  {
    m_aRWLock.writeLocked ( () -> { internalCreateItem (aSMPRedirect); });
    AuditHelper.onAuditCreateSuccess (SMPRedirect.OT,
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded (),
                                      aSMPRedirect.getTargetHref (),
                                      aSMPRedirect.getSubjectUniqueIdentifier (),
                                      aSMPRedirect.getCertificate (),
                                      aSMPRedirect.getExtensions ().getExtensionsAsJsonString ());
    return aSMPRedirect;
  }

  @NonNull
  @IsLocked (ELockType.WRITE)
  private ISMPRedirect _updateSMPRedirect (@NonNull final SMPRedirect aSMPRedirect)
  {
    m_aRWLock.writeLocked ( () -> { internalUpdateItem (aSMPRedirect); });
    AuditHelper.onAuditModifySuccess (SMPRedirect.OT,
                                      "set-all",
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded (),
                                      aSMPRedirect.getTargetHref (),
                                      aSMPRedirect.getSubjectUniqueIdentifier (),
                                      aSMPRedirect.getCertificate (),
                                      aSMPRedirect.getExtensions ().getExtensionsAsJsonString ());
    return aSMPRedirect;
  }

  @NonNull
  public ISMPRedirect createOrUpdateSMPRedirect (@NonNull final IParticipantIdentifier aParticipantID,
                                                 @NonNull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                 @NonNull @Nonempty final String sTargetHref,
                                                 @NonNull @Nonempty final String sSubjectUniqueIdentifier,
                                                 @Nullable final X509Certificate aCertificate,
                                                 @Nullable final String sExtension)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aDocumentTypeIdentifier, "DocumentTypeIdentifier");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createOrUpdateSMPRedirect (" +
                    aParticipantID +
                    ", " +
                    aDocumentTypeIdentifier +
                    ", " +
                    sTargetHref +
                    ", " +
                    sSubjectUniqueIdentifier +
                    ", " +
                    aCertificate +
                    ", " +
                    (StringHelper.isNotEmpty (sExtension) ? "with extension" : "without extension") +
                    ")");

    final ISMPRedirect aOldRedirect = getSMPRedirectOfServiceGroupAndDocumentType (aParticipantID,
                                                                                   aDocumentTypeIdentifier);
    SMPRedirect aNewRedirect;
    if (aOldRedirect == null)
    {
      // Create new ID
      aNewRedirect = new SMPRedirect (aParticipantID,
                                      aDocumentTypeIdentifier,
                                      sTargetHref,
                                      sSubjectUniqueIdentifier,
                                      aCertificate,
                                      sExtension);
      _createSMPRedirect (aNewRedirect);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSMPRedirect - success - created");

      m_aCallbacks.forEach (x -> x.onSMPRedirectCreated (aNewRedirect));
    }
    else
    {
      // Reuse old ID
      aNewRedirect = new SMPRedirect (aParticipantID,
                                      aDocumentTypeIdentifier,
                                      sTargetHref,
                                      sSubjectUniqueIdentifier,
                                      aCertificate,
                                      sExtension);
      _updateSMPRedirect (aNewRedirect);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSMPRedirect - success - updated");

      m_aCallbacks.forEach (x -> x.onSMPRedirectUpdated (aNewRedirect));
    }
    return aNewRedirect;
  }

  @NonNull
  public EChange deleteSMPRedirect (@Nullable final ISMPRedirect aSMPRedirect)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPRedirect (" + aSMPRedirect + ")");

    if (aSMPRedirect == null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPRedirect - failure");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPRedirect aRealRedirect = internalDeleteItem (aSMPRedirect.getID ());
      if (aRealRedirect == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPRedirect.OT, aSMPRedirect.getID (), "no-such-id");
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("deleteSMPRedirect - failure");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    m_aCallbacks.forEach (x -> x.onSMPRedirectDeleted (aSMPRedirect));

    AuditHelper.onAuditDeleteSuccess (SMPRedirect.OT,
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded ());
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPRedirect - success");
    return EChange.CHANGED;
  }

  @NonNull
  public EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return EChange.UNCHANGED;

    EChange eChange = EChange.UNCHANGED;
    for (final ISMPRedirect aRedirect : getAllSMPRedirectsOfServiceGroup (aParticipantID.getURIEncoded ()))
      eChange = eChange.or (deleteSMPRedirect (aRedirect));
    return eChange;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirects ()
  {
    return getAll ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    return getAllSMPRedirectsOfServiceGroup (aParticipantID == null ? null : aParticipantID.getURIEncoded ());
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final String sServiceGroupID)
  {
    final ICommonsList <ISMPRedirect> ret = new CommonsArrayList <> ();
    if (StringHelper.isNotEmpty (sServiceGroupID))
      findAll (x -> x.getServiceGroupID ().equals (sServiceGroupID), ret::add);
    return ret;
  }

  @Nonnegative
  public long getSMPRedirectCount ()
  {
    return size ();
  }

  @Nullable
  public ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (@Nullable final IParticipantIdentifier aParticipantID,
                                                                   @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    if (aParticipantID == null)
      return null;
    if (aDocTypeID == null)
      return null;

    return findFirst (x -> x.getServiceGroupID ().equals (aParticipantID.getURIEncoded ()) &&
                           aDocTypeID.hasSameContent (x.getDocumentTypeIdentifier ()));
  }
}
