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
package com.helger.phoss.smp.domain.transportprofile;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.dao.DAOException;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ESMPTransportProfileState;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smp.SMPTransportProfile;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class SMPTransportProfileManagerXML extends
                                                 AbstractPhotonMapBasedWALDAO <ISMPTransportProfile, SMPTransportProfile>
                                                 implements
                                                 ISMPTransportProfileManager
{
  public SMPTransportProfileManagerXML (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPTransportProfile.class, sFilename);
  }

  @Override
  @Nonnull
  protected EChange onInit ()
  {
    // Add the default transport profiles
    for (final ESMPTransportProfile eTransportProfile : ESMPTransportProfile.values ())
      internalCreateItem (new SMPTransportProfile (eTransportProfile));
    return EChange.CHANGED;
  }

  @Nullable
  public ISMPTransportProfile createSMPTransportProfile (@Nonnull @Nonempty final String sID,
                                                         @Nonnull @Nonempty final String sName,
                                                         final boolean bIsDeprecated)
  {
    // Double ID needs to be taken care of
    if (containsWithID (sID))
      return null;

    final SMPTransportProfile aSMPTransportProfile = new SMPTransportProfile (sID,
                                                                              sName,
                                                                              bIsDeprecated ? ESMPTransportProfileState.DEPRECATED
                                                                                            : ESMPTransportProfileState.ACTIVE);

    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aSMPTransportProfile);
    });
    AuditHelper.onAuditCreateSuccess (SMPTransportProfile.OT, sID, sName, Boolean.valueOf (bIsDeprecated));
    return aSMPTransportProfile;
  }

  @Nonnull
  public EChange updateSMPTransportProfile (@Nullable final String sSMPTransportProfileID,
                                            @Nonnull @Nonempty final String sName,
                                            final boolean bIsDeprecated)
  {
    final SMPTransportProfile aSMPTransportProfile = getOfID (sSMPTransportProfileID);
    if (aSMPTransportProfile == null)
    {
      AuditHelper.onAuditModifyFailure (SMPTransportProfile.OT, "set-all", sSMPTransportProfileID, "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aSMPTransportProfile.setName (sName));
      eChange = eChange.or (aSMPTransportProfile.setState (bIsDeprecated ? ESMPTransportProfileState.DEPRECATED
                                                                         : ESMPTransportProfileState.ACTIVE));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;

      internalUpdateItem (aSMPTransportProfile);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (SMPTransportProfile.OT,
                                      "set-all",
                                      sSMPTransportProfileID,
                                      sName,
                                      Boolean.valueOf (bIsDeprecated));
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteSMPTransportProfile (@Nullable final String sSMPTransportProfileID)
  {
    if (StringHelper.isEmpty (sSMPTransportProfileID))
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPTransportProfile aSMPTransportProfile = internalDeleteItem (sSMPTransportProfileID);
      if (aSMPTransportProfile == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPTransportProfile.OT, sSMPTransportProfileID, "no-such-id");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (SMPTransportProfile.OT, sSMPTransportProfileID);
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPTransportProfile> getAllSMPTransportProfiles ()
  {
    return getAll ();
  }

  @Nullable
  public ISMPTransportProfile getSMPTransportProfileOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  public boolean containsSMPTransportProfileWithID (@Nullable final String sID)
  {
    return containsWithID (sID);
  }

  @Nonnegative
  public long getSMPTransportProfileCount ()
  {
    return size ();
  }
}
