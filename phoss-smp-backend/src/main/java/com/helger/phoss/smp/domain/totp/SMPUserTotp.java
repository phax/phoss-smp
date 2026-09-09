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

import java.time.LocalDateTime;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.state.EChange;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.type.ObjectType;
import com.helger.datetime.helper.PDTFactory;

/**
 * Default implementation of {@link ISMPUserTotp}.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
@NotThreadSafe
public class SMPUserTotp implements ISMPUserTotp
{
  public static final ObjectType OT = new ObjectType ("SmpUserTotp");

  private final String m_sUserID;
  private final String m_sSecret;
  private boolean m_bEnabled;
  private final LocalDateTime m_aRegistrationDT;
  private Long m_aLastUsedTimeSlot;

  public SMPUserTotp (@NonNull @Nonempty final String sUserID,
                      @NonNull @Nonempty final String sSecret,
                      final boolean bEnabled,
                      @NonNull final LocalDateTime aRegistrationDT,
                      @Nullable final Long aLastUsedTimeSlot)
  {
    ValueEnforcer.notEmpty (sUserID, "UserID");
    ValueEnforcer.notEmpty (sSecret, "Secret");
    ValueEnforcer.notNull (aRegistrationDT, "RegistrationDateTime");

    m_sUserID = sUserID;
    m_sSecret = sSecret;
    m_bEnabled = bEnabled;
    m_aRegistrationDT = aRegistrationDT;
    m_aLastUsedTimeSlot = aLastUsedTimeSlot;
  }

  @NonNull
  @Nonempty
  public final String getID ()
  {
    return m_sUserID;
  }

  @NonNull
  @Nonempty
  public final String getSecret ()
  {
    return m_sSecret;
  }

  public final boolean isEnabled ()
  {
    return m_bEnabled;
  }

  @NonNull
  public final EChange setEnabled (final boolean bEnabled)
  {
    if (bEnabled == m_bEnabled)
      return EChange.UNCHANGED;
    m_bEnabled = bEnabled;
    return EChange.CHANGED;
  }

  @NonNull
  public final LocalDateTime getRegistrationDateTime ()
  {
    return m_aRegistrationDT;
  }

  @Nullable
  public final Long getLastUsedTimeSlot ()
  {
    return m_aLastUsedTimeSlot;
  }

  @NonNull
  public final EChange setLastUsedTimeSlot (@Nullable final Long aLastUsedTimeSlot)
  {
    if (Objects.equals (aLastUsedTimeSlot, m_aLastUsedTimeSlot))
      return EChange.UNCHANGED;
    m_aLastUsedTimeSlot = aLastUsedTimeSlot;
    return EChange.CHANGED;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPUserTotp rhs = (SMPUserTotp) o;
    return m_sUserID.equals (rhs.m_sUserID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sUserID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    // Deliberately never log the secret itself
    return new ToStringGenerator (this).append ("UserID", m_sUserID)
                                       .append ("SecretLength", m_sSecret.length ())
                                       .append ("Enabled", m_bEnabled)
                                       .append ("RegistrationDateTime", m_aRegistrationDT)
                                       .append ("LastUsedTimeSlot", m_aLastUsedTimeSlot)
                                       .getToString ();
  }

  /**
   * Create a new, not yet confirmed enrollment for the provided user.
   *
   * @param sUserID
   *        The ID of the user. May neither be <code>null</code> nor empty.
   * @param sSecret
   *        The Base32 encoded secret. May neither be <code>null</code> nor empty.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static SMPUserTotp createPending (@NonNull @Nonempty final String sUserID,
                                           @NonNull @Nonempty final String sSecret)
  {
    return new SMPUserTotp (sUserID, sSecret, false, PDTFactory.getCurrentLocalDateTime (), null);
  }
}
