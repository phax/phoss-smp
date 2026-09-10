/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.totp;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.dao.DAOException;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

/**
 * XML file based implementation of the {@link ISMPUserTotpManager} interface.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
public final class SMPUserTotpManagerXML extends AbstractPhotonMapBasedWALDAO <ISMPUserTotp, SMPUserTotp> implements
                                         ISMPUserTotpManager
{
  public SMPUserTotpManagerXML (@NonNull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPUserTotp.class, sFilename);
  }

  @NonNull
  public ISMPUserTotp createOrReplaceTotp (@NonNull @Nonempty final String sUserID,
                                           @NonNull @Nonempty final String sSecret)
  {
    final SMPUserTotp aTotp = SMPUserTotp.createPending (sUserID, sSecret);

    m_aRWLock.writeLocked (() -> {
      if (containsWithID (sUserID))
        internalDeleteItem (sUserID);
      internalCreateItem (aTotp);
    });

    SMPUserTotpEnabledCache.clearCache (sUserID);

    // Never audit the secret itself
    AuditHelper.onAuditCreateSuccess (SMPUserTotp.OT, sUserID);
    return aTotp;
  }

  @NonNull
  public EChange setTotpEnabled (@Nullable final String sUserID, final boolean bEnabled)
  {
    final SMPUserTotp aTotp = getOfID (sUserID);
    if (aTotp == null)
    {
      AuditHelper.onAuditModifyFailure (SMPUserTotp.OT, "set-enabled", sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (aTotp.setEnabled (bEnabled).isUnchanged ())
        return EChange.UNCHANGED;
      internalUpdateItem (aTotp);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    SMPUserTotpEnabledCache.clearCache (sUserID);

    AuditHelper.onAuditModifySuccess (SMPUserTotp.OT, "set-enabled", sUserID, Boolean.valueOf (bEnabled));
    return EChange.CHANGED;
  }

  @NonNull
  public EChange setTotpLastUsedTimeSlot (@Nullable final String sUserID, final long nTimeSlot)
  {
    final SMPUserTotp aTotp = getOfID (sUserID);
    if (aTotp == null)
    {
      AuditHelper.onAuditModifyFailure (SMPUserTotp.OT, "set-last-used-time-slot", sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      // Check and set inside the write lock, so that two parallel submissions of the same code
      // cannot both succeed
      final Long aLastUsedTimeSlot = aTotp.getLastUsedTimeSlot ();
      if (aLastUsedTimeSlot != null && nTimeSlot <= aLastUsedTimeSlot.longValue ())
        return EChange.UNCHANGED;

      aTotp.setLastUsedTimeSlot (Long.valueOf (nTimeSlot));
      internalUpdateItem (aTotp);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditModifySuccess (SMPUserTotp.OT, "set-last-used-time-slot", sUserID, Long.valueOf (nTimeSlot));
    return EChange.CHANGED;
  }

  @NonNull
  public EChange setRecoveryCodeHashes (@Nullable final String sUserID,
                                        @Nullable final ICommonsList <String> aRecoveryCodeHashes)
  {
    final SMPUserTotp aTotp = getOfID (sUserID);
    if (aTotp == null)
    {
      AuditHelper.onAuditModifyFailure (SMPUserTotp.OT, "set-recovery-codes", sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (aTotp.setAllRecoveryCodeHashes (aRecoveryCodeHashes).isUnchanged ())
        return EChange.UNCHANGED;
      internalUpdateItem (aTotp);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    // Never audit the recovery codes themselves
    AuditHelper.onAuditModifySuccess (SMPUserTotp.OT,
                                      "set-recovery-codes",
                                      sUserID,
                                      Integer.valueOf (aRecoveryCodeHashes == null ? 0 : aRecoveryCodeHashes.size ()));
    return EChange.CHANGED;
  }

  @NonNull
  public EChange consumeRecoveryCodeHash (@Nullable final String sUserID, @Nullable final String sRecoveryCodeHash)
  {
    if (StringHelper.isEmpty (sRecoveryCodeHash))
      return EChange.UNCHANGED;

    final SMPUserTotp aTotp = getOfID (sUserID);
    if (aTotp == null)
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      // Remove inside the write lock, so that the same recovery code cannot be used twice in
      // parallel
      if (aTotp.removeRecoveryCodeHash (sRecoveryCodeHash).isUnchanged ())
        return EChange.UNCHANGED;
      internalUpdateItem (aTotp);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditModifySuccess (SMPUserTotp.OT, "consume-recovery-code", sUserID);
    return EChange.CHANGED;
  }

  @NonNull
  public EChange deleteTotp (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (internalDeleteItem (sUserID) == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPUserTotp.OT, sUserID, "no-such-id");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    SMPUserTotpEnabledCache.clearCache (sUserID);

    AuditHelper.onAuditDeleteSuccess (SMPUserTotp.OT, sUserID);
    return EChange.CHANGED;
  }

  @Nullable
  public ISMPUserTotp getTotpOfUserID (@Nullable final String sUserID)
  {
    return getOfID (sUserID);
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPUserTotp> getAllTotps ()
  {
    return getAll ();
  }
}
