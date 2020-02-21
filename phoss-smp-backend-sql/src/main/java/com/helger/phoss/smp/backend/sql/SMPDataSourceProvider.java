package com.helger.phoss.smp.backend.sql;

import com.helger.db.jdbc.AbstractConnector;
import com.helger.phoss.smp.SMPServerConfiguration;

final class SMPDataSourceProvider extends AbstractConnector
{
  @Override
  protected String getJDBCDriverClassName ()
  {
    return SMPServerConfiguration.getConfigFile ().getAsString (SMPJPAConfiguration.CONFIG_JDBC_DRIVER);
  }

  @Override
  protected String getUserName ()
  {
    return SMPServerConfiguration.getConfigFile ().getAsString (SMPJPAConfiguration.CONFIG_JDBC_USER);
  }

  @Override
  protected String getPassword ()
  {
    return SMPServerConfiguration.getConfigFile ().getAsString (SMPJPAConfiguration.CONFIG_JDBC_PASSWORD);
  }

  @Override
  protected String getDatabaseName ()
  {
    return SMPServerConfiguration.getConfigFile ().getAsString (SMPJPAConfiguration.CONFIG_TARGET_DATABASE);
  }

  @Override
  public String getConnectionUrl ()
  {
    return SMPServerConfiguration.getConfigFile ().getAsString (SMPJPAConfiguration.CONFIG_JDBC_URL);
  }
}
