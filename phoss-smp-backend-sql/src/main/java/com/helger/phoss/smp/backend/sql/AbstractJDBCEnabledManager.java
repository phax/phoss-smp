/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.sql;

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

import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.settings.exchange.configfile.ConfigFile;

public abstract class AbstractJDBCEnabledManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractJDBCEnabledManager.class);
  private final DBExecutor m_aDBExec;

  public AbstractJDBCEnabledManager ()
  {
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
