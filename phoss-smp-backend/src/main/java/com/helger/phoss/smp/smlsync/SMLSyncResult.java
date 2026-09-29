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
package com.helger.phoss.smp.smlsync;

import java.time.Duration;
import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;

/**
 * The immutable summary of a single reconciliation of the local Service Groups with the SML.<br>
 * The participant identifiers themselves are deliberately not contained - they are written to the
 * report file, because there may be hundreds of thousands of them.
 *
 * @author Philip Helger
 * @since 8.6.0
 */
@Immutable
public final class SMLSyncResult
{
  private final LocalDateTime m_aStartDT;
  private final LocalDateTime m_aEndDT;
  private final String m_sSMLID;
  private final String m_sSMPID;
  private final int m_nSMLPageCount;
  private final int m_nSMLParticipantCount;
  private final int m_nLocalParticipantCount;
  private final int m_nMissingInSMLCount;
  private final int m_nOrphansInSMLCount;

  public SMLSyncResult (@NonNull final LocalDateTime aStartDT,
                        @NonNull final LocalDateTime aEndDT,
                        @NonNull @Nonempty final String sSMLID,
                        @NonNull @Nonempty final String sSMPID,
                        @Nonnegative final int nSMLPageCount,
                        @Nonnegative final int nSMLParticipantCount,
                        @Nonnegative final int nLocalParticipantCount,
                        @Nonnegative final int nMissingInSMLCount,
                        @Nonnegative final int nOrphansInSMLCount)
  {
    ValueEnforcer.notNull (aStartDT, "StartDT");
    ValueEnforcer.notNull (aEndDT, "EndDT");
    ValueEnforcer.notEmpty (sSMLID, "SMLID");
    ValueEnforcer.notEmpty (sSMPID, "SMPID");
    ValueEnforcer.isGE0 (nSMLPageCount, "SMLPageCount");
    ValueEnforcer.isGE0 (nSMLParticipantCount, "SMLParticipantCount");
    ValueEnforcer.isGE0 (nLocalParticipantCount, "LocalParticipantCount");
    ValueEnforcer.isGE0 (nMissingInSMLCount, "MissingInSMLCount");
    ValueEnforcer.isGE0 (nOrphansInSMLCount, "OrphansInSMLCount");

    m_aStartDT = aStartDT;
    m_aEndDT = aEndDT;
    m_sSMLID = sSMLID;
    m_sSMPID = sSMPID;
    m_nSMLPageCount = nSMLPageCount;
    m_nSMLParticipantCount = nSMLParticipantCount;
    m_nLocalParticipantCount = nLocalParticipantCount;
    m_nMissingInSMLCount = nMissingInSMLCount;
    m_nOrphansInSMLCount = nOrphansInSMLCount;
  }

  /**
   * @return The date and time at which the reconciliation was started. Never <code>null</code>.
   */
  @NonNull
  public LocalDateTime getStartDateTime ()
  {
    return m_aStartDT;
  }

  /**
   * @return The date and time at which the reconciliation was finished. Never <code>null</code>.
   */
  @NonNull
  public LocalDateTime getEndDateTime ()
  {
    return m_aEndDT;
  }

  /**
   * @return The duration of the reconciliation. Never <code>null</code>.
   */
  @NonNull
  public Duration getDuration ()
  {
    return Duration.between (m_aStartDT, m_aEndDT);
  }

  /**
   * @return The ID of the SML that was queried. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getSMLID ()
  {
    return m_sSMLID;
  }

  /**
   * @return The ID of the SMP that was queried. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getSMPID ()
  {
    return m_sSMPID;
  }

  /**
   * @return The number of pages that were read from the SML. Always &ge; 0.
   */
  @Nonnegative
  public int getSMLPageCount ()
  {
    return m_nSMLPageCount;
  }

  /**
   * @return The number of participants the SML holds for this SMP. Always &ge; 0.
   */
  @Nonnegative
  public int getSMLParticipantCount ()
  {
    return m_nSMLParticipantCount;
  }

  /**
   * @return The number of participants this SMP holds locally. Always &ge; 0.
   */
  @Nonnegative
  public int getLocalParticipantCount ()
  {
    return m_nLocalParticipantCount;
  }

  /**
   * @return The number of participants that exist locally but are not registered at the SML. Those
   *         participants cannot be resolved via DNS. Always &ge; 0.
   */
  @Nonnegative
  public int getMissingInSMLCount ()
  {
    return m_nMissingInSMLCount;
  }

  /**
   * @return The number of participants the SML holds for this SMP that do not exist locally. DNS
   *         resolves them to this SMP, but this SMP answers HTTP 404 for them. Always &ge; 0.
   */
  @Nonnegative
  public int getOrphansInSMLCount ()
  {
    return m_nOrphansInSMLCount;
  }

  /**
   * @return <code>true</code> if no difference at all was found between this SMP and the SML.
   */
  public boolean isInSync ()
  {
    return m_nMissingInSMLCount == 0 && m_nOrphansInSMLCount == 0;
  }

  /**
   * @return <code>true</code> if every local participant is missing at the SML, while at least one
   *         local participant exists. That is the signature of an SMP that was unregistered and
   *         re-registered at the SML, because unregistering an SMP deletes all of its participants
   *         - and not of a broken SML connection.
   */
  public boolean isAllLocalParticipantsMissing ()
  {
    return m_nLocalParticipantCount > 0 && m_nMissingInSMLCount == m_nLocalParticipantCount;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("StartDT", m_aStartDT)
                                       .append ("EndDT", m_aEndDT)
                                       .append ("SMLID", m_sSMLID)
                                       .append ("SMPID", m_sSMPID)
                                       .append ("SMLPageCount", m_nSMLPageCount)
                                       .append ("SMLParticipantCount", m_nSMLParticipantCount)
                                       .append ("LocalParticipantCount", m_nLocalParticipantCount)
                                       .append ("MissingInSMLCount", m_nMissingInSMLCount)
                                       .append ("OrphansInSMLCount", m_nOrphansInSMLCount)
                                       .getToString ();
  }
}
