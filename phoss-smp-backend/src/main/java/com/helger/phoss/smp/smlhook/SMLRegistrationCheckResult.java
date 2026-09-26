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
package com.helger.phoss.smp.smlhook;

import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.datetime.helper.PDTFactory;

/**
 * The immutable result of reading this SMPs own registration from the SML, as defined in chapter
 * 3.1.3.2 of the SML specification.
 *
 * @author Philip Helger
 * @since 8.5.1
 */
@Immutable
public final class SMLRegistrationCheckResult
{
  private final LocalDateTime m_aCheckDT;
  private final ESMLRegistrationState m_eState;
  private final String m_sSMPID;
  private final String m_sLogicalAddress;
  private final String m_sPhysicalAddress;
  private final String m_sErrorMessage;

  private SMLRegistrationCheckResult (@NonNull final ESMLRegistrationState eState,
                                      @NonNull @Nonempty final String sSMPID,
                                      @Nullable final String sLogicalAddress,
                                      @Nullable final String sPhysicalAddress,
                                      @Nullable final String sErrorMessage)
  {
    ValueEnforcer.notNull (eState, "State");
    ValueEnforcer.notEmpty (sSMPID, "SMPID");

    m_aCheckDT = PDTFactory.getCurrentLocalDateTime ();
    m_eState = eState;
    m_sSMPID = sSMPID;
    m_sLogicalAddress = sLogicalAddress;
    m_sPhysicalAddress = sPhysicalAddress;
    m_sErrorMessage = sErrorMessage;
  }

  /**
   * @param sSMPID
   *        The SMP ID that was queried. May neither be <code>null</code> nor empty.
   * @param sLogicalAddress
   *        The logical address the SML holds for this SMP. May be <code>null</code>.
   * @param sPhysicalAddress
   *        The physical address the SML holds for this SMP. May be <code>null</code>.
   * @return A result in state {@link ESMLRegistrationState#REGISTERED}. Never <code>null</code>.
   */
  @NonNull
  public static SMLRegistrationCheckResult createRegistered (@NonNull @Nonempty final String sSMPID,
                                                             @Nullable final String sLogicalAddress,
                                                             @Nullable final String sPhysicalAddress)
  {
    return new SMLRegistrationCheckResult (ESMLRegistrationState.REGISTERED,
                                           sSMPID,
                                           sLogicalAddress,
                                           sPhysicalAddress,
                                           null);
  }

  /**
   * @param sSMPID
   *        The SMP ID that was queried. May neither be <code>null</code> nor empty.
   * @return A result in state {@link ESMLRegistrationState#NOT_REGISTERED}. Never
   *         <code>null</code>.
   */
  @NonNull
  public static SMLRegistrationCheckResult createNotRegistered (@NonNull @Nonempty final String sSMPID)
  {
    return new SMLRegistrationCheckResult (ESMLRegistrationState.NOT_REGISTERED, sSMPID, null, null, null);
  }

  /**
   * @param sSMPID
   *        The SMP ID that was queried. May neither be <code>null</code> nor empty.
   * @param sErrorMessage
   *        The technical reason why the SML could not be queried. May be <code>null</code>.
   * @return A result in state {@link ESMLRegistrationState#CHECK_FAILED}. Never <code>null</code>.
   */
  @NonNull
  public static SMLRegistrationCheckResult createCheckFailed (@NonNull @Nonempty final String sSMPID,
                                                              @Nullable final String sErrorMessage)
  {
    return new SMLRegistrationCheckResult (ESMLRegistrationState.CHECK_FAILED, sSMPID, null, null, sErrorMessage);
  }

  /**
   * @return The date and time at which this result was created. Never <code>null</code>.
   */
  @NonNull
  public LocalDateTime getCheckDateTime ()
  {
    return m_aCheckDT;
  }

  /**
   * @return The outcome of the check. Never <code>null</code>.
   */
  @NonNull
  public ESMLRegistrationState getState ()
  {
    return m_eState;
  }

  /**
   * @return The SMP ID that was queried. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getSMPID ()
  {
    return m_sSMPID;
  }

  /**
   * @return The logical address the SML holds for this SMP. <code>null</code> unless the state is
   *         {@link ESMLRegistrationState#REGISTERED}.
   */
  @Nullable
  public String getLogicalAddress ()
  {
    return m_sLogicalAddress;
  }

  /**
   * @return The physical address the SML holds for this SMP. <code>null</code> unless the state is
   *         {@link ESMLRegistrationState#REGISTERED}.
   */
  @Nullable
  public String getPhysicalAddress ()
  {
    return m_sPhysicalAddress;
  }

  /**
   * @return The technical reason why the SML could not be queried. <code>null</code> unless the
   *         state is {@link ESMLRegistrationState#CHECK_FAILED}.
   */
  @Nullable
  public String getErrorMessage ()
  {
    return m_sErrorMessage;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("CheckDT", m_aCheckDT)
                                       .append ("State", m_eState)
                                       .append ("SMPID", m_sSMPID)
                                       .appendIfNotNull ("LogicalAddress", m_sLogicalAddress)
                                       .appendIfNotNull ("PhysicalAddress", m_sPhysicalAddress)
                                       .appendIfNotNull ("ErrorMessage", m_sErrorMessage)
                                       .getToString ();
  }
}
