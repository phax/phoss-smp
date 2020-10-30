package com.helger.phoss.smp.backend.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.settings.exchange.configfile.ConfigFile;

/**
 * The SMP specific DB Executor
 *
 * @author Philip Helger
 * @since 5.3.1
 */
public final class SMPDBExecutor extends DBExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPDBExecutor.class);

  public SMPDBExecutor ()
  {
    super (SMPDataSourceSingleton.getInstance ().getDataSourceProvider ());

    final ConfigFile aCF = SMPServerConfiguration.getConfigFile ();

    // This is ONLY for debugging
    setDebugConnections (aCF.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_DEBUG_CONNECTIONS, false));
    setDebugTransactions (aCF.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_DEBUG_TRANSACTIONS, false));
    setDebugSQLStatements (aCF.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_DEBUG_SQL, false));

    setConnectionStatusChangeCallback ( (eOld, eNew) -> {
      // false: don't trigger callback, because the source is DBExecutor
      SMPMetaManager.getInstance ().setBackendConnectionEstablished (eNew, false);
    });

    // Cannot be done here, because of initialization order
    if (false)
    {
      // Allow communicating in the other direction as well
      SMPMetaManager.getInstance ().setBackendConnectionStatusChangeCallback (eNew -> this.resetConnectionEstablished ());
    }

    if (aCF.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_EXECUTION_TIME_WARNING_ENABLE,
                          SMPJDBCConfiguration.DEFAULT_JDBC_EXECUTION_TIME_WARNING_ENABLE))
    {
      final long nMillis = aCF.getAsLong (SMPJDBCConfiguration.CONFIG_JDBC_EXECUTION_TIME_WARNING_MS,
                                          DBExecutor.DEFAULT_EXECUTION_DURATION_WARN_MS);
      if (nMillis > 0)
        setExecutionDurationWarnMS (nMillis);
      else
        LOGGER.warn ("Ignoring setting '" + SMPJDBCConfiguration.CONFIG_JDBC_EXECUTION_TIME_WARNING_MS + "' because it is invalid.");
    }
    else
    {
      // Zero means none
      setExecutionDurationWarnMS (0);
    }
  }
}
