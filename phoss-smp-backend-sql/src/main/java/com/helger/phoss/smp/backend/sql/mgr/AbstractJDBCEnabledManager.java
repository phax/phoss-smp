/**
 * Copyright (C) 2019-2020 Philip Helger and contributors
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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.backend.sql.EDatabaseType;
import com.helger.phoss.smp.backend.sql.SMPDataSourceSingleton;
import com.helger.phoss.smp.backend.sql.SMPJDBCConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.settings.exchange.configfile.ConfigFile;

public abstract class AbstractJDBCEnabledManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractJDBCEnabledManager.class);
  protected final EDatabaseType m_eDBType;
  private final DBExecutor m_aDBExec;

  public AbstractJDBCEnabledManager (@Nonnull final EDatabaseType eDBType)
  {
    ValueEnforcer.notNull (eDBType, "DBType");

    m_eDBType = eDBType;

    final ConfigFile aCF = SMPServerConfiguration.getConfigFile ();

    // Create executor once for all manages
    m_aDBExec = new DBExecutor (SMPDataSourceSingleton.getInstance ().getDataSourceProvider ());

    // This is ONLY for debugging
    m_aDBExec.setDebugConnections (aCF.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_DEBUG_CONNECTIONS, false));
    m_aDBExec.setDebugTransactions (aCF.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_DEBUG_TRANSACTIONS, false));
    m_aDBExec.setDebugSQLStatements (aCF.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_DEBUG_SQL, false));

    m_aDBExec.setConnectionStatusChangeCallback ( (eOld, eNew) -> {
      SMPMetaManager.getInstance ().setBackendConnectionEstablished (eNew);
    });

    if (aCF.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_EXECUTION_TIME_WARNING_ENABLE,
                          SMPJDBCConfiguration.DEFAULT_JDBC_EXECUTION_TIME_WARNING_ENABLE))
    {
      final long nMillis = aCF.getAsLong (SMPJDBCConfiguration.CONFIG_JDBC_EXECUTION_TIME_WARNING_MS,
                                          DBExecutor.DEFAULT_EXECUTION_DURATION_WARN_MS);
      if (nMillis > 0)
        m_aDBExec.setExecutionDurationWarnMS (nMillis);
      else
        LOGGER.warn ("Ignoring setting '" + SMPJDBCConfiguration.CONFIG_JDBC_EXECUTION_TIME_WARNING_MS + "' because it is invalid.");
    }
    else
    {
      // Zero means none
      m_aDBExec.setExecutionDurationWarnMS (0);
    }
  }

  @Nonnull
  public final EDatabaseType getDBType ()
  {
    return m_eDBType;
  }

  @Nonnull
  protected final DBExecutor executor ()
  {
    return m_aDBExec;
  }

  @Nullable
  public static Time toTime (@Nullable final LocalTime aLT)
  {
    return aLT == null ? null : Time.valueOf (aLT);
  }

  @Nullable
  public static Date toDate (@Nullable final LocalDate aLD)
  {
    return aLD == null ? null : Date.valueOf (aLD);
  }

  @Nullable
  public static Timestamp toTimestamp (@Nullable final LocalDateTime aLDT)
  {
    return aLDT == null ? null : Timestamp.valueOf (aLDT);
  }
}
