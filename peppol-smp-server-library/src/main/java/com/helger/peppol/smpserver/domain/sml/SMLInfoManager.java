/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.domain.sml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.dao.DAOException;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.photon.basic.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.basic.audit.AuditHelper;

public final class SMLInfoManager extends AbstractPhotonMapBasedWALDAO <ISMLInfo, SMLInfo> implements ISMLInfoManager
{
  public SMLInfoManager (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMLInfo.class, sFilename);
  }

  @Override
  @Nonnull
  protected EChange onInit ()
  {
    // Add the default transport profiles
    for (final ESML e : ESML.values ())
      internalCreateItem (new SMLInfo (e));
    return EChange.CHANGED;
  }

  @Nonnull
  public ISMLInfo createSMLInfo (@Nonnull @Nonempty final String sDisplayName,
                                 @Nonnull @Nonempty final String sDNSZone,
                                 @Nonnull @Nonempty final String sManagementServiceURL,
                                 final boolean bClientCertificateRequired)
  {
    final SMLInfo aSMLInfo = new SMLInfo (sDisplayName, sDNSZone, sManagementServiceURL, bClientCertificateRequired);

    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aSMLInfo);
    });
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
    final SMLInfo aSMLInfo = getOfID (sSMLInfoID);
    if (aSMLInfo == null)
    {
      AuditHelper.onAuditModifyFailure (SMLInfo.OT, sSMLInfoID, "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aSMLInfo.setDisplayName (sDisplayName));
      eChange = eChange.or (aSMLInfo.setDNSZone (sDNSZone));
      eChange = eChange.or (aSMLInfo.setManagementServiceURL (sManagementServiceURL));
      eChange = eChange.or (aSMLInfo.setClientCertificateRequired (bClientCertificateRequired));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;

      internalUpdateItem (aSMLInfo);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (SMLInfo.OT,
                                      "all",
                                      sSMLInfoID,
                                      sDisplayName,
                                      sDNSZone,
                                      sManagementServiceURL,
                                      Boolean.valueOf (bClientCertificateRequired));
    return EChange.CHANGED;
  }

  @Nullable
  public EChange removeSMLInfo (@Nullable final String sSMLInfoID)
  {
    if (StringHelper.hasNoText (sSMLInfoID))
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMLInfo aSMLInfo = internalDeleteItem (sSMLInfoID);
      if (aSMLInfo == null)
      {
        AuditHelper.onAuditDeleteFailure (SMLInfo.OT, "no-such-id", sSMLInfoID);
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

  @Nonnull
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
}
