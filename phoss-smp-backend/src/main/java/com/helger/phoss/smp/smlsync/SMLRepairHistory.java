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

import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.datetime.helper.PDTFactory;

/**
 * Remembers which repair was already performed from which reconciliation report.<br>
 * This exists because the counts shown for a report never change: a report is a snapshot of a point
 * in time and a repair deliberately does not rewrite it. Without this, a report that was fully
 * repaired still offers the same numbers, which reads like the repair did not work.<br>
 * The history is only kept in memory and is lost on a restart - it is a hint for the user
 * interface, not state anything depends on.
 *
 * @author Philip Helger
 * @since 8.6.0
 */
@ThreadSafe
public final class SMLRepairHistory
{
  /**
   * What a single repair did.
   *
   * @author Philip Helger
   */
  @Immutable
  public static final class Entry
  {
    private final LocalDateTime m_aDT;
    private final int m_nSucceeded;
    private final int m_nAlreadyDone;
    private final int m_nFailed;

    private Entry (final int nSucceeded, final int nAlreadyDone, final int nFailed)
    {
      m_aDT = PDTFactory.getCurrentLocalDateTime ();
      m_nSucceeded = nSucceeded;
      m_nAlreadyDone = nAlreadyDone;
      m_nFailed = nFailed;
    }

    @NonNull
    public LocalDateTime getDateTime ()
    {
      return m_aDT;
    }

    @Nonnegative
    public int getSucceededCount ()
    {
      return m_nSucceeded;
    }

    @Nonnegative
    public int getAlreadyDoneCount ()
    {
      return m_nAlreadyDone;
    }

    @Nonnegative
    public int getFailedCount ()
    {
      return m_nFailed;
    }

    /**
     * @return <code>true</code> if nothing failed, so that repeating this repair is pointless.
     */
    public boolean isComplete ()
    {
      return m_nFailed == 0;
    }
  }

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();

  @GuardedBy ("RW_LOCK")
  private static final ICommonsMap <String, Entry> MAP = new CommonsHashMap <> ();

  private SMLRepairHistory ()
  {}

  @NonNull
  @Nonempty
  private static String _getKey (@NonNull @Nonempty final String sReportFilename,
                                 @NonNull final ESMLRepairAction eAction)
  {
    return sReportFilename + "|" + eAction.getID ();
  }

  /**
   * Remember that a repair was performed.
   *
   * @param sReportFilename
   *        The name of the report the repair was based on. May neither be <code>null</code> nor
   *        empty.
   * @param eAction
   *        The action that was performed. May not be <code>null</code>.
   * @param nSucceeded
   *        The number of participants that were repaired.
   * @param nAlreadyDone
   *        The number of participants that were already in the wanted state.
   * @param nFailed
   *        The number of participants that could not be repaired.
   */
  public static void recordRepair (@NonNull @Nonempty final String sReportFilename,
                                   @NonNull final ESMLRepairAction eAction,
                                   @Nonnegative final int nSucceeded,
                                   @Nonnegative final int nAlreadyDone,
                                   @Nonnegative final int nFailed)
  {
    ValueEnforcer.notEmpty (sReportFilename, "ReportFilename");
    ValueEnforcer.notNull (eAction, "Action");

    final String sKey = _getKey (sReportFilename, eAction);
    RW_LOCK.writeLocked (() -> MAP.put (sKey, new Entry (nSucceeded, nAlreadyDone, nFailed)));
  }

  /**
   * @param sReportFilename
   *        The name of the report. May neither be <code>null</code> nor empty.
   * @param eAction
   *        The action. May not be <code>null</code>.
   * @return The last repair of that report with that action, or <code>null</code> if none was
   *         performed since the last restart.
   */
  @Nullable
  public static Entry getLastRepair (@NonNull @Nonempty final String sReportFilename,
                                     @NonNull final ESMLRepairAction eAction)
  {
    ValueEnforcer.notEmpty (sReportFilename, "ReportFilename");
    ValueEnforcer.notNull (eAction, "Action");

    final String sKey = _getKey (sReportFilename, eAction);
    return RW_LOCK.readLockedGet (() -> MAP.get (sKey));
  }

  /**
   * Drop the remembered repairs.
   */
  public static void clearCache ()
  {
    RW_LOCK.writeLocked (MAP::clear);
  }
}
