/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
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

import javax.annotation.Nonnegative;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.id.factory.AbstractPersistingLongIDFactory;
import com.helger.commons.mutable.MutableLong;
import com.helger.commons.string.StringParser;
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

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPIDFactoryJDBC.class);

  private final long m_nInitialCount;

  /**
   * Constructor
   *
   * @param nInitialCount
   *        The count to be used if no database entry is available. This is
   *        purely for migrating existing counter from file based to DB based.
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
    final MutableLong aReadValue = new MutableLong (0);

    final DBExecutor aExecutor = new SMPDBExecutor ();
    aExecutor.performInTransaction ( () -> {
      // Read existing value
      final String sExistingValue = SMPSettingsManagerJDBC.getSettingsValueFromDB (aExecutor, SETTINGS_KEY_LATEST_ID);
      final long nRead = StringParser.parseLong (sExistingValue, m_nInitialCount);
      aReadValue.set (nRead);

      // Write new value
      final long nNewValue = nRead + nReserveCount;
      SMPSettingsManagerJDBC.setSettingsValueInDB (aExecutor, SETTINGS_KEY_LATEST_ID, Long.toString (nNewValue));

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Updated SQL ID from " + sExistingValue + " to " + nNewValue);
    });

    return aReadValue.longValue ();
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
