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
package com.helger.phoss.smp.exchange;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.datetime.helper.PDTFactory;

/**
 * A process wide lock that ensures, that only a single Service Group export runs at a time.
 * Exporting all Service Groups of an SMP is an expensive operation, and running several of them in
 * parallel can easily exhaust the available heap memory.
 *
 * @author Philip Helger
 * @since 8.2.1
 */
@ThreadSafe
public final class ServiceGroupExportLock
{
  private static final AtomicBoolean RUNNING = new AtomicBoolean (false);
  private static final AtomicReference <LocalDateTime> START_DT = new AtomicReference <> ();
  private static final AtomicReference <String> USER_ID = new AtomicReference <> ();

  private ServiceGroupExportLock ()
  {}

  /**
   * Try to acquire the lock. If this method returns <code>true</code>, {@link #release()} must be
   * called afterwards - preferably in a <code>finally</code> block.
   *
   * @param sUserID
   *        The ID of the user starting the export. May be <code>null</code>.
   * @return <code>true</code> if the lock was acquired, <code>false</code> if another export is
   *         already running.
   */
  public static boolean tryAcquire (@Nullable final String sUserID)
  {
    if (!RUNNING.compareAndSet (false, true))
      return false;

    START_DT.set (PDTFactory.getCurrentLocalDateTime ());
    USER_ID.set (sUserID);
    return true;
  }

  /**
   * Release the lock previously acquired via {@link #tryAcquire(String)}.
   */
  public static void release ()
  {
    USER_ID.set (null);
    START_DT.set (null);
    RUNNING.set (false);
  }

  /**
   * @return <code>true</code> if an export is currently running, <code>false</code> if not.
   */
  public static boolean isExportRunning ()
  {
    return RUNNING.get ();
  }

  /**
   * @return The date and time at which the currently running export was started. May be
   *         <code>null</code> if no export is running.
   */
  @Nullable
  public static LocalDateTime getExportStartDateTime ()
  {
    return START_DT.get ();
  }

  /**
   * @return The ID of the user that started the currently running export. May be <code>null</code>.
   */
  @Nullable
  public static String getExportUserID ()
  {
    return USER_ID.get ();
  }
}
