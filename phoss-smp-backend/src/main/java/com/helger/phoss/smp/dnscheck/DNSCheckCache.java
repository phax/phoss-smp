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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.photon.security.lock.SingleRunLock;

/**
 * Holds the result of the last DNS check that was performed as a background job, so that the
 * <em>Service Groups</em> page can render it without performing any DNS lookup itself.<br>
 * The result is deliberately only kept in memory and is lost on a restart - it is a diagnostic
 * snapshot of something that is looked up again anyway, not persistent state.
 *
 * @author Philip Helger
 * @since 8.5.1
 */
@ThreadSafe
public final class DNSCheckCache
{
  /**
   * The process wide lock that ensures only a single DNS check runs at a time. This is deliberately
   * not the lock of the SML operations - a DNS check performs no SML call at all.
   */
  public static final SingleRunLock LOCK = new SingleRunLock ("DNS state check");

  private static final Logger LOGGER = LoggerFactory.getLogger (DNSCheckCache.class);
  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();

  @GuardedBy ("RW_LOCK")
  private static DNSCheckResult s_aResult;

  private DNSCheckCache ()
  {}

  /**
   * @return The result of the last DNS check that was performed as a background job, or
   *         <code>null</code> if none was performed since the last restart.
   */
  @Nullable
  public static DNSCheckResult getResult ()
  {
    return RW_LOCK.readLockedGet ( () -> s_aResult);
  }

  /**
   * @param aResult
   *        The result of a DNS check that was just performed. May not be <code>null</code>.
   */
  public static void setResult (@NonNull final DNSCheckResult aResult)
  {
    ValueEnforcer.notNull (aResult, "Result");
    RW_LOCK.writeLocked ( () -> s_aResult = aResult);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("The DNS check cache was updated with " + aResult.getEntryCount () + " entries");
  }

  /**
   * Drop the stored result.
   */
  public static void clearCache ()
  {
    RW_LOCK.writeLocked ( () -> s_aResult = null);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("The DNS check cache was cleared");
  }
}
