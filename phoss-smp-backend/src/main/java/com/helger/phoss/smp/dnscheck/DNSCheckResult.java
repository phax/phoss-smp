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
package com.helger.phoss.smp.dnscheck;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * The outcome of a DNS check over a set of participants.
 *
 * @author Philip Helger
 * @since 8.5.1
 */
@Immutable
public final class DNSCheckResult
{
  private final LocalDateTime m_aStartDT;
  private final LocalDateTime m_aEndDT;
  private final ICommonsList <DNSCheckEntry> m_aEntries;

  public DNSCheckResult (@NonNull final LocalDateTime aStartDT,
                         @NonNull final LocalDateTime aEndDT,
                         @NonNull final List <DNSCheckEntry> aEntries)
  {
    ValueEnforcer.notNull (aStartDT, "StartDT");
    ValueEnforcer.notNull (aEndDT, "EndDT");
    ValueEnforcer.notNull (aEntries, "Entries");
    m_aStartDT = aStartDT;
    m_aEndDT = aEndDT;
    m_aEntries = new CommonsArrayList <> (aEntries);
  }

  @NonNull
  public LocalDateTime getStartDateTime ()
  {
    return m_aStartDT;
  }

  @NonNull
  public LocalDateTime getEndDateTime ()
  {
    return m_aEndDT;
  }

  @NonNull
  public Duration getDuration ()
  {
    return Duration.between (m_aStartDT, m_aEndDT);
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <DNSCheckEntry> getAllEntries ()
  {
    return m_aEntries.getClone ();
  }

  @Nonnegative
  public int getEntryCount ()
  {
    return m_aEntries.size ();
  }

  @Nonnegative
  public int getCountOfState (@NonNull final EDNSCheckState eState)
  {
    ValueEnforcer.notNull (eState, "State");
    int ret = 0;
    for (final DNSCheckEntry aEntry : m_aEntries)
      if (aEntry.getState () == eState)
        ret++;
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("StartDT", m_aStartDT)
                                       .append ("EndDT", m_aEndDT)
                                       .append ("EntryCount", m_aEntries.size ())
                                       .getToString ();
  }
}
