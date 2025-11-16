/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.sml;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.dao.DAOException;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

public final class SMLInfoManagerXML extends AbstractPhotonMapBasedWALDAO <ISMLInfo, SMLInfo> implements ISMLInfoManager
{
  public SMLInfoManagerXML (@NonNull @Nonempty final String sFilename) throws DAOException
  {
    super (SMLInfo.class, sFilename);
  }

  @Override
  @NonNull
  protected EChange onInit ()
  {
    // Add the default transport profiles
    for (final ESML e : ESML.values ())
      internalCreateItem (SMLInfo.builder (e).build ());
    return EChange.CHANGED;
  }

  @NonNull
  public ISMLInfo createSMLInfo (@NonNull @Nonempty final String sDisplayName,
                                 @NonNull @Nonempty final String sDNSZone,
                                 @NonNull @Nonempty final String sManagementServiceURL,
                                 @NonNull final String sURLSuffixManageSMP,
                                 @NonNull final String sURLSuffixManageParticipant,
                                 final boolean bClientCertificateRequired)
  {
    final SMLInfo aSMLInfo = SMLInfo.builder ()
                                    .idNewPersistent ()
                                    .displayName (sDisplayName)
                                    .dnsZone (sDNSZone)
                                    .managementServiceURL (sManagementServiceURL)
                                    .urlSuffixManageSMP (sURLSuffixManageSMP)
                                    .urlSuffixManageParticipant (sURLSuffixManageParticipant)
                                    .clientCertificateRequired (bClientCertificateRequired)
                                    .build ();

    m_aRWLock.writeLocked ( () -> { internalCreateItem (aSMLInfo); });
    AuditHelper.onAuditCreateSuccess (SMLInfo.OT,
                                      aSMLInfo.getID (),
                                      sDisplayName,
                                      sDNSZone,
                                      sManagementServiceURL,
                                      sURLSuffixManageSMP,
                                      sURLSuffixManageParticipant,
                                      Boolean.valueOf (bClientCertificateRequired));
    return aSMLInfo;
  }

  @NonNull
  public EChange updateSMLInfo (@Nullable final String sSMLInfoID,
                                @NonNull @Nonempty final String sDisplayName,
                                @NonNull @Nonempty final String sDNSZone,
                                @NonNull @Nonempty final String sManagementServiceURL,
                                @NonNull final String sURLSuffixManageSMP,
                                @NonNull final String sURLSuffixManageParticipant,
                                final boolean bClientCertificateRequired)
  {
    final SMLInfo aSMLInfo = getOfID (sSMLInfoID);
    if (aSMLInfo == null)
    {
      AuditHelper.onAuditModifyFailure (SMLInfo.OT, "set-all", sSMLInfoID, "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMLInfo aNewSMLInfo = SMLInfo.builder (aSMLInfo)
                                         .displayName (sDisplayName)
                                         .dnsZone (sDNSZone)
                                         .managementServiceURL (sManagementServiceURL)
                                         .urlSuffixManageSMP (sURLSuffixManageSMP)
                                         .urlSuffixManageParticipant (sURLSuffixManageParticipant)
                                         .clientCertificateRequired (bClientCertificateRequired)
                                         .build ();
      if (aSMLInfo.equals (aNewSMLInfo))
        return EChange.UNCHANGED;

      internalUpdateItem (aNewSMLInfo);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (SMLInfo.OT,
                                      "set-all",
                                      sSMLInfoID,
                                      sDisplayName,
                                      sDNSZone,
                                      sManagementServiceURL,
                                      sURLSuffixManageSMP,
                                      sURLSuffixManageParticipant,
                                      Boolean.valueOf (bClientCertificateRequired));
    return EChange.CHANGED;
  }

  @Nullable
  public EChange deleteSMLInfo (@Nullable final String sSMLInfoID)
  {
    if (StringHelper.isEmpty (sSMLInfoID))
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMLInfo aSMLInfo = internalDeleteItem (sSMLInfoID);
      if (aSMLInfo == null)
      {
        AuditHelper.onAuditDeleteFailure (SMLInfo.OT, sSMLInfoID, "no-such-id");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (SMLInfo.OT, sSMLInfoID);
    return EChange.CHANGED;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMLInfo> getAllSMLInfos ()
  {
    return getAll ();
  }

  @Nullable
  public ISMLInfo getSMLInfoOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  public boolean containsSMLInfoWithID (@Nullable final String sID)
  {
    return containsWithID (sID);
  }

  @Nullable
  public ISMLInfo findFirstWithManageParticipantIdentifierEndpointAddress (@Nullable final String sAddress)
  {
    if (StringHelper.isEmpty (sAddress))
      return null;
    return findFirst (x -> x.getManageParticipantIdentifierEndpointAddress ().toExternalForm ().equals (sAddress));
  }
}
