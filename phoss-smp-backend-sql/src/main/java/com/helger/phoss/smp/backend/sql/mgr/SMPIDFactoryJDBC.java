/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.backend.sql.mgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.id.factory.AbstractPersistingLongIDFactory;
import com.helger.base.numeric.mutable.MutableBoolean;
import com.helger.base.numeric.mutable.MutableLong;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringParser;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;

/**
 * A special ID factory tailored towards the usage with a JDBC database
 *
 * @author Philip Helger
 */
public class SMPIDFactoryJDBC extends AbstractPersistingLongIDFactory
{
  /** The default number of values to reserve with a single IO action */
  public static final int DEFAULT_RESERVE_COUNT = 20;

  /** The ID of the key column in the "smp-settings" table */
  public static final String SETTINGS_KEY_LATEST_ID = "latest-id";

  /** The maximum number of attempts to reserve a new block of IDs */
  public static final int MAX_RESERVATION_ATTEMPTS = 10;

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPIDFactoryJDBC.class);

  private final long m_nInitialCount;

  /**
   * Constructor
   *
   * @param nInitialCount
   *        The count to be used if no database entry is available. This is purely for migrating
   *        existing counter from file based to DB based.
   */
  public SMPIDFactoryJDBC (@Nonnegative final long nInitialCount)
  {
    super (DEFAULT_RESERVE_COUNT);
    ValueEnforcer.isGE0 (nInitialCount, "InitialCount");
    m_nInitialCount = nInitialCount;
  }

  @Override
  protected long readAndUpdateIDCounter (@Nonnegative final int nReserveCount)
  {
    final DBExecutor aExecutor = new SMPDBExecutor ();

    // Every attempt uses a transaction of its own, because a retry inside the same transaction
    // would read the same stale value again on a database defaulting to REPEATABLE READ
    for (int nAttempt = 1; nAttempt <= MAX_RESERVATION_ATTEMPTS; ++nAttempt)
    {
      final MutableLong aReadValue = new MutableLong (0);
      final MutableBoolean aReserved = new MutableBoolean (false);

      final ESuccess eSuccess = aExecutor.performInTransaction (() -> {
        // Read existing value
        final String sExistingValue = SMPSettingsManagerJDBC.getSettingsValueFromDB (aExecutor, SETTINGS_KEY_LATEST_ID);
        final long nRead = StringParser.parseLong (sExistingValue, m_nInitialCount);
        final long nNewValue = nRead + nReserveCount;

        // Write new value, but only if no other SMP instance reserved a block in the meantime.
        // An unconditional update would hand out the same block of IDs twice, because the lock
        // of this factory is limited to the current JVM.
        if (SMPSettingsManagerJDBC.compareAndSetSettingsValueInDB (aExecutor,
                                                                   SETTINGS_KEY_LATEST_ID,
                                                                   sExistingValue,
                                                                   Long.toString (nNewValue)).isChanged ())
        {
          aReadValue.set (nRead);
          aReserved.set (true);

          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Updated SQL ID from " + sExistingValue + " to " + nNewValue);
        }
      });

      if (eSuccess.isSuccess () && aReserved.booleanValue ())
        return aReadValue.longValue ();

      LOGGER.warn ("Failed to reserve a block of " +
                   nReserveCount +
                   " IDs in attempt " +
                   nAttempt +
                   " of " +
                   MAX_RESERVATION_ATTEMPTS);
    }

    // Returning a value anyway would hand out IDs of a block that was never persisted
    throw new IllegalStateException ("Failed to reserve a block of " +
                                     nReserveCount +
                                     " IDs in " +
                                     MAX_RESERVATION_ATTEMPTS +
                                     " attempts");
  }

  @Override
  public boolean equals (final Object o)
  {
    // New member, no change
    return super.equals (o);
  }

  @Override
  public int hashCode ()
  {
    // New member, no change
    return super.hashCode ();
  }
}
