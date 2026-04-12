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
package com.helger.phoss.smp.backend.sql;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.config.IConfig;
import com.helger.db.api.config.JdbcConfigurationConfig;

/**
 * SMP-specific JDBC configuration that extends {@link JdbcConfigurationConfig} with a fallback for
 * the old <code>target-database</code> configuration key (the new canonical key is
 * <code>jdbc.database-type</code>) and SMP-specific configuration properties.
 *
 * @author Philip Helger
 * @since 8.1.4
 */
public class SMPJdbcConfiguration extends JdbcConfigurationConfig
{
  /**
   * The JDBC configuration prefix.
   */
  public static final String CONFIG_PREFIX = "jdbc.";

  /**
   * Old configuration key for database type that has no "jdbc." prefix.
   */
  private static final String OLD_CONFIG_TARGET_DATABASE = "target-database";

  private static final String CONFIG_JDBC_CACHE_SG_ENABLED = "jdbc.cache.sg.enabled";
  private static final boolean DEFAULT_JDBC_CACHE_SG_ENABLED = true;

  private static final String CONFIG_SMP_STATUS_SQL_ENABLED = "smp.status.sql.enabled";
  private static final boolean DEFAULT_SMP_STATUS_SQL_ENABLED = true;

  /**
   * Constructor
   *
   * @param aConfig
   *        The configuration object to use. May not be <code>null</code>.
   */
  public SMPJdbcConfiguration (@NonNull final IConfig aConfig)
  {
    super (aConfig, CONFIG_PREFIX);
  }

  @Override
  @Nullable
  public String getJdbcDatabaseType ()
  {
    // Try the new key "jdbc.database-type" first
    String ret = super.getJdbcDatabaseType ();
    if (ret == null)
    {
      // Fallback to the old key "target-database"
      ret = getConfig ().getAsString (OLD_CONFIG_TARGET_DATABASE);
    }
    return ret;
  }

  /**
   * @return <code>true</code> if the SMP ServiceGroup cache is enabled, <code>false</code> if not.
   *         Default is <code>true</code>.
   */
  public boolean isJdbcServiceGroupCacheEnabled ()
  {
    return getConfig ().getAsBoolean (CONFIG_JDBC_CACHE_SG_ENABLED, DEFAULT_JDBC_CACHE_SG_ENABLED);
  }

  /**
   * @return <code>true</code> if the SQL status is enabled, <code>false</code> if not. Default is
   *         <code>true</code>.
   */
  public boolean isStatusEnabled ()
  {
    return getConfig ().getAsBoolean (CONFIG_SMP_STATUS_SQL_ENABLED, DEFAULT_SMP_STATUS_SQL_ENABLED);
  }
}
