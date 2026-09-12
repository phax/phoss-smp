/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
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

import java.util.function.Consumer;
import java.util.function.Predicate;

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
import com.helger.base.equals.EqualsHelper;
import com.helger.base.numeric.mutable.MutableLong;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsSet;
import com.helger.collection.paging.IPagingSpec;
import com.helger.dao.DAOException;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.serviceinfo.ESMPServiceInformationColumn;
import com.helger.phoss.smp.domain.serviceinfo.EndpointUsageInfo;
import com.helger.phoss.smp.domain.serviceinfo.IEndpointUsageInfo;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpointHelper;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.security.SMPCertificateHelper;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.core.paging.TableColumnHelper;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

/**
 * Manager for all {@link SMPServiceInformation} objects.
 *
 * @author Philip Helger
 */
public final class SMPServiceInformationManagerXML extends
                                                   AbstractPhotonMapBasedWALDAO <ISMPServiceInformation, SMPServiceInformation>
                                                   implements
                                                   ISMPServiceInformationManager
{
  private static final ESMPServiceInformationColumn [] COLUMNS = ESMPServiceInformationColumn.values ();

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPServiceInformationManagerXML.class);

  private final CallbackList <ISMPServiceInformationCallback> m_aCBs = new CallbackList <> ();
  private final ISMPAccessPointManager m_aAccessPointMgr;

  public SMPServiceInformationManagerXML (@NonNull @Nonempty final String sFilename,
                                          @NonNull final ISMPAccessPointManager aAccessPointMgr) throws DAOException
  {
    super (SMPServiceInformation.class, sFilename);
    ValueEnforcer.notNull (aAccessPointMgr, "AccessPointMgr");
    m_aAccessPointMgr = aAccessPointMgr;
  }

  @NonNull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceInformationCallback> serviceInformationCallbacks ()
  {
    return m_aCBs;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPEndpoint> getAllSMPEndpoints (@Nullable final IParticipantIdentifier aParticipantID,
                                                         @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                                         @Nullable final IProcessIdentifier aProcessID,
                                                         @Nullable final String sTransportProfileID)
  {
    final ICommonsList <ISMPEndpoint> ret = new CommonsArrayList <> ();
    final ISMPServiceInformation aServiceInfo = getSMPServiceInformationOfServiceGroupAndDocumentType (aParticipantID,
                                                                                                       aDocTypeID);
    if (aServiceInfo != null)
    {
      final ISMPProcess aProcess = aServiceInfo.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        ret.addAll (aProcess.getAllEndpointsOfTransportProfile (sTransportProfileID));
      }
    }
    return ret;
  }

  @NonNull
  public ESuccess mergeSMPServiceInformation (@NonNull final ISMPServiceInformation aSMPServiceInformationObj)
  {
    final SMPServiceInformation aSMPServiceInformation = (SMPServiceInformation) aSMPServiceInformationObj;
    ValueEnforcer.notNull (aSMPServiceInformation, "ServiceInformation");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("mergeSMPServiceInformation (" + aSMPServiceInformationObj + ")");

    // Resolve the Access Point references of all endpoints to the managed objects
    SMPEndpointHelper.resolveAccessPoints (m_aAccessPointMgr, aSMPServiceInformation);

    // Check for an update
    boolean bChangeExisting = false;
    final SMPServiceInformation aOldInformation = (SMPServiceInformation) getSMPServiceInformationOfServiceGroupAndDocumentType (aSMPServiceInformation.getServiceGroupParticipantIdentifier (),
                                                                                                                                 aSMPServiceInformation.getDocumentTypeIdentifier ());
    if (aOldInformation != null)
    {
      // If a service information is present, it must be the provided object!
      // This is not true for the REST API
      if (EqualsHelper.identityEqual (aOldInformation, aSMPServiceInformation))
        bChangeExisting = true;
    }

    if (bChangeExisting)
    {
      // Edit existing
      m_aRWLock.writeLocked (() -> { internalUpdateItem (aOldInformation); });

      AuditHelper.onAuditModifySuccess (SMPServiceInformation.OT,
                                        "set-all",
                                        aOldInformation.getID (),
                                        aOldInformation.getServiceGroupID (),
                                        aOldInformation.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aOldInformation.getAllProcesses (),
                                        aOldInformation.getExtensions ().getExtensionsAsJsonString ());

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("mergeSMPServiceInformation - success - updated");

      m_aCBs.forEach (x -> x.onSMPServiceInformationUpdated (aSMPServiceInformation));
    }
    else
    {
      // (Optionally delete the old one and) create the new one
      boolean bRemovedOld = false;
      m_aRWLock.writeLock ().lock ();
      try
      {
        if (aOldInformation != null)
        {
          // Delete only if present
          final SMPServiceInformation aDeletedInformation = internalDeleteItem (aOldInformation.getID ());
          bRemovedOld = EqualsHelper.identityEqual (aDeletedInformation, aOldInformation);
        }

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
                                        aSMPServiceInformation.getExtensions ().getExtensionsAsJsonString ());
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("mergeSMPServiceInformation - success - created");

      if (aOldInformation != null)
        m_aCBs.forEach (x -> x.onSMPServiceInformationUpdated (aSMPServiceInformation));
      else
        m_aCBs.forEach (x -> x.onSMPServiceInformationCreated (aSMPServiceInformation));
    }
    return ESuccess.SUCCESS;
  }

  @NonNull
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
        AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, aSMPServiceInformation.getID (), "no-such-id");
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

    m_aCBs.forEach (x -> x.onSMPServiceInformationDeleted (aSMPServiceInformation));

    return EChange.CHANGED;
  }

  @NonNull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    EChange eChange = EChange.UNCHANGED;
    for (final ISMPServiceInformation aSMPServiceInformation : getAllSMPServiceInformationOfServiceGroup (aParticipantID))
      eChange = eChange.or (deleteSMPServiceInformation (aSMPServiceInformation));
    return eChange;
  }

  @NonNull
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
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, aSMPServiceInformation.getID (), "no-such-id");
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPProcess - failure - no such service information");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      // Main deletion in write lock
      if (aRealServiceInformation.deleteProcess (aProcess.getProcessIdentifier ()).isUnchanged ())
      {
        AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT,
                                          aSMPServiceInformation.getID (),
                                          aProcess.getProcessIdentifier ().getURIEncoded (),
                                          "no-such-process");
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("deleteSMPProcess - failure - no such process");
        return EChange.UNCHANGED;
      }

      // Save changes
      internalUpdateItem (aRealServiceInformation);
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

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation ()
  {
    return getAll ();
  }

  @NonNull
  @ReturnsMutableCopy
  @Override
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation (@NonNull final IPagingSpec aPagingSpec,
                                                                            @Nullable final String sSearchText)
  {
    return getAllPaged (TableColumnHelper.getSearchPredicate (COLUMNS, sSearchText),
                        aPagingSpec,
                        TableColumnHelper.getComparator (COLUMNS, aPagingSpec));
  }

  @Override
  public long getSMPServiceInformationCount (@Nullable final String sSearchText)
  {
    final Predicate <ISMPServiceInformation> aFilter = TableColumnHelper.getSearchPredicate (COLUMNS, sSearchText);
    return aFilter == null ? getSMPServiceInformationCount () : getCount (aFilter);
  }

  public void forEachSMPServiceInformation (@NonNull final Consumer <? super ISMPServiceInformation> aConsumer)
  {
    forEachValue (aConsumer);
  }

  @Nonnegative
  public long getSMPServiceInformationCount ()
  {
    return size ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformationOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    if (aParticipantID != null)
    {
      final String sServiceGroupID = aParticipantID.getURIEncoded ();
      findAll (x -> x.getServiceGroupID ().equals (sServiceGroupID), ret::add);
    }
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    final ICommonsList <IDocumentTypeIdentifier> ret = new CommonsArrayList <> ();
    if (aParticipantID != null)
    {
      final String sServiceGroupID = aParticipantID.getURIEncoded ();
      findAllMapped (aSI -> aSI.getServiceGroupID ().equals (sServiceGroupID),
                     ISMPServiceInformation::getDocumentTypeIdentifier,
                     ret::add);
    }
    return ret;
  }

  @Nullable
  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final IParticipantIdentifier aParticipantID,
                                                                                       @Nullable final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    if (aParticipantID == null)
      return null;
    if (aDocumentTypeIdentifier == null)
      return null;

    final String sServiceGroupID = aParticipantID.getURIEncoded ();
    final ICommonsList <ISMPServiceInformation> ret = getAll (aSI -> aSI.getServiceGroupID ()
                                                                        .equals (sServiceGroupID) &&
                                                                     aSI.getDocumentTypeIdentifier ()
                                                                        .hasSameContent (aDocumentTypeIdentifier));

    if (ret.isEmpty ())
      return null;
    if (ret.size () > 1)
      LOGGER.warn ("Found more than one entry for service group '" +
                   sServiceGroupID +
                   "' and document type '" +
                   aDocumentTypeIdentifier.getValue () +
                   "'. This seems to be a bug! Using the first one.");
    return ret.getFirstOrNull ();
  }

  public boolean containsAnyEndpointWithTransportProfile (@Nullable final String sTransportProfileID)
  {
    if (StringHelper.isEmpty (sTransportProfileID))
      return false;

    return containsAny (x -> x.containsAnyEndpointWithTransportProfile (sTransportProfileID));
  }

  @Nonnegative
  public long getEndpointCount ()
  {
    final MutableLong ret = new MutableLong (0);
    forEachValue (aSI -> { ret.inc (aSI.getTotalEndpointCount ()); });
    return ret.longValue ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsMap <String, IEndpointUsageInfo> getEndpointURLUsageMap ()
  {
    final ICommonsMap <String, IEndpointUsageInfo> ret = new CommonsHashMap <> ();
    forEachValue (aSI -> {
      for (final ISMPProcess aProcess : aSI.getAllProcesses ())
        for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
          if (aEndpoint.hasEndpointReference ())
          {
            final IEndpointUsageInfo aInfo = ret.computeIfAbsent (aEndpoint.getEndpointReference (),
                                                                  k -> new EndpointUsageInfo ());
            ((EndpointUsageInfo) aInfo).incrementForServiceGroupID (aSI.getServiceGroupID ());
          }
    });
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsMap <String, IEndpointUsageInfo> getEndpointCertificateUsageMap ()
  {
    final ICommonsMap <String, IEndpointUsageInfo> ret = new CommonsHashMap <> ();
    forEachValue (aSI -> {
      for (final ISMPProcess aProcess : aSI.getAllProcesses ())
        for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        {
          final String sNormalizedCert = SMPCertificateHelper.getNormalizedCert (aEndpoint.getCertificate ());
          final IEndpointUsageInfo aInfo = ret.computeIfAbsent (sNormalizedCert, k -> new EndpointUsageInfo ());
          ((EndpointUsageInfo) aInfo).incrementForServiceGroupID (aSI.getServiceGroupID ());
        }
    });
    return ret;
  }

  /**
   * Count the endpoints referencing the Access Point with the provided ID.
   *
   * @param sAccessPointID
   *        The Access Point ID to look for. May be <code>null</code>.
   * @return The number of matching endpoints.
   */
  @Nonnegative
  public long getEndpointCountUsingAccessPoint (@Nullable final String sAccessPointID)
  {
    if (StringHelper.isEmpty (sAccessPointID))
      return 0;

    final MutableLong ret = new MutableLong (0);
    forEachValue (aSI -> {
      for (final ISMPProcess aProcess : aSI.getAllProcesses ())
        for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
          if (sAccessPointID.equals (aEndpoint.getAccessPointID ()))
            ret.inc ();
    });
    return ret.longValue ();
  }

  /**
   * Modify all endpoints matching the provided filter with the provided modifier.
   *
   * @param aServiceGroupID
   *        Optional service group filter. May be <code>null</code>.
   * @param aFilter
   *        The filter to select the endpoints to be changed. May not be <code>null</code>.
   * @param aModifier
   *        The modifier to be invoked on all matching endpoints. May not be <code>null</code>.
   * @return The number of changed endpoints.
   */
  @Nonnegative
  private long _updateEndpoints (@Nullable final IParticipantIdentifier aServiceGroupID,
                                 @NonNull final Predicate <ISMPEndpoint> aFilter,
                                 @NonNull final Consumer <SMPEndpoint> aModifier)
  {
    final MutableLong aEndpointsChanged = new MutableLong (0);
    performWithoutAutoSave ( () -> {
      for (final ISMPServiceInformation aSI : getAllSMPServiceInformation ())
      {
        if (aServiceGroupID != null && !aSI.getServiceGroupParticipantIdentifier ().hasSameContent (aServiceGroupID))
          continue;

        boolean bSIChanged = false;
        for (final ISMPProcess aProcess : aSI.getAllProcesses ())
          for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
            if (aEndpoint instanceof SMPEndpoint && aFilter.test (aEndpoint))
            {
              aModifier.accept ((SMPEndpoint) aEndpoint);
              bSIChanged = true;
              aEndpointsChanged.inc ();
            }
        if (bSIChanged)
          m_aRWLock.writeLocked ( () -> { internalUpdateItem ((SMPServiceInformation) aSI); });
      }
    });
    return aEndpointsChanged.longValue ();
  }

  @Nonnegative
  public long updateAllEndpointURLs (@Nullable final IParticipantIdentifier aServiceGroupID,
                                     @NonNull final String sOldURL,
                                     @NonNull final String sNewURL)
  {
    ValueEnforcer.notNull (sOldURL, "OldURL");
    ValueEnforcer.notNull (sNewURL, "NewURL");

    if (sOldURL.equals (sNewURL))
      return 0;

    // Endpoints containing the URL directly
    long nChanged = _updateEndpoints (aServiceGroupID,
                                      x -> !x.hasAccessPoint () && sOldURL.equals (x.getEndpointReference ()),
                                      x -> x.setEndpointReference (sNewURL));

    // Endpoints referencing an Access Point with that URL - changing the Access Point affects all
    // of them at once, so this is only possible if no service group filter is present
    for (final ISMPAccessPoint aAP : m_aAccessPointMgr.getAllAccessPoints ())
      if (aAP.hasSameEndpointReference (sOldURL))
      {
        final long nUsingAP = getEndpointCountUsingAccessPoint (aAP.getID ());
        if (nUsingAP == 0)
          continue;

        if (aServiceGroupID != null)
        {
          LOGGER.warn ("The endpoints referencing the Access Point '" +
                       aAP.getName () +
                       "' are not changed, because an Access Point is shared across Service Groups");
          continue;
        }

        if (m_aAccessPointMgr.updateAccessPoint (aAP.getID (), aAP.getName (), sNewURL, aAP.getCertificate ())
                             .isChanged ())
          nChanged += nUsingAP;
      }

    return nChanged;
  }

  @Nonnegative
  public long updateAllEndpointCertificates (@NonNull final String sOldCert, @NonNull final String sNewCert)
  {
    ValueEnforcer.notNull (sOldCert, "OldCert");
    ValueEnforcer.notNull (sNewCert, "NewCert");

    final String sOldCertNormalized = SMPCertificateHelper.getNormalizedCert (sOldCert);

    // Endpoints containing the certificate directly
    long nChanged = _updateEndpoints (null,
                                      x -> !x.hasAccessPoint () &&
                                           sOldCertNormalized.equals (SMPCertificateHelper.getNormalizedCert (x.getCertificate ())),
                                      x -> x.setCertificate (sNewCert));

    // Endpoints referencing an Access Point with that certificate - no endpoint is touched at all,
    // because the certificate is an attribute of the Access Point
    for (final String sAPID : m_aAccessPointMgr.getAllAccessPointIDsWithCertificate (sOldCertNormalized))
    {
      final long nUsingAP = getEndpointCountUsingAccessPoint (sAPID);
      if (m_aAccessPointMgr.updateAccessPointCertificate (sAPID, sNewCert).isChanged ())
        nChanged += nUsingAP;
    }

    return nChanged;
  }

  @Nonnegative
  public long useAccessPointForMatchingEndpoints (@NonNull final String sAccessPointID,
                                                  final boolean bRequireSameEndpointReference)
  {
    ValueEnforcer.notNull (sAccessPointID, "AccessPointID");

    final ISMPAccessPoint aAP = m_aAccessPointMgr.getAccessPointOfID (sAccessPointID);
    if (aAP == null || !aAP.hasCertificate ())
      return 0;

    final String sAPCertNormalized = SMPCertificateHelper.getNormalizedCert (aAP.getCertificate ());
    return _updateEndpoints (null,
                             x -> !x.hasAccessPoint () &&
                                  sAPCertNormalized.equals (SMPCertificateHelper.getNormalizedCert (x.getCertificate ())) &&
                                  (!bRequireSameEndpointReference || aAP.hasSameEndpointReference (x.getEndpointReference ())),
                             x -> x.setAccessPoint (aAP));
  }
}
