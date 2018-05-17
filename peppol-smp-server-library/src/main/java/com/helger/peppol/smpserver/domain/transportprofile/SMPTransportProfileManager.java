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
package com.helger.peppol.smpserver.domain.transportprofile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.dao.DAOException;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smp.SMPTransportProfile;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirect;
import com.helger.photon.basic.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.basic.audit.AuditHelper;

public final class SMPTransportProfileManager extends
                                              AbstractPhotonMapBasedWALDAO <ISMPTransportProfile, SMPTransportProfile>
                                              implements
                                              ISMPTransportProfileManager
{
  public SMPTransportProfileManager (@Nonnull @Nonempty final String sFilename) throws DAOException
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

    final SMPTransportProfile aSMPTransportProfile = new SMPTransportProfile (sID, sName, bIsDeprecated);

    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aSMPTransportProfile);
    });
    AuditHelper.onAuditCreateSuccess (SMPTransportProfile.OT, sID, sName);
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
      AuditHelper.onAuditModifyFailure (SMPTransportProfile.OT, sSMPTransportProfileID, "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aSMPTransportProfile.setName (sName));
      eChange = eChange.or (aSMPTransportProfile.setDeprecated (bIsDeprecated));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;

      internalUpdateItem (aSMPTransportProfile);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (SMPTransportProfile.OT, "all", sSMPTransportProfileID, sName);
    return EChange.CHANGED;
  }

  @Nullable
  public EChange removeSMPTransportProfile (@Nullable final String sSMPTransportProfileID)
  {
    if (StringHelper.hasNoText (sSMPTransportProfileID))
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPTransportProfile aSMPTransportProfile = internalDeleteItem (sSMPTransportProfileID);
      if (aSMPTransportProfile == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPRedirect.OT, "no-such-id", sSMPTransportProfileID);
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (SMPRedirect.OT, sSMPTransportProfileID);
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
}
