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

import com.helger.base.enforce.ValueEnforcer;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.db.api.config.IJdbcConfiguration;
import com.helger.db.flyway.FlywayConfiguration;
import com.helger.db.flyway.FlywayConfigurationBuilderConfig;

/**
 * The specific Flyway Configuration builder for phoss SMP. It extends the generic
 * {@link FlywayConfigurationBuilderConfig} with fallback logic for JDBC connection data and the old
 * <code>jdbc.schema-create</code> configuration key.
 *
 * @author Philip Helger
 * @since 8.1.4
 */
public class SMPFlywayConfigurationBuilder extends FlywayConfigurationBuilderConfig
{
  /**
   * The Flyway configuration prefix.
   */
  public static final String FLYWAY_CONFIG_PREFIX = "smp.flyway.";

  /**
   * Old configuration key for schema creation.
   */
  private static final String OLD_CONFIG_JDBC_SCHEMA_CREATE = "jdbc.schema-create";

  /**
   * Constructor
   *
   * @param aConfig
   *        The configuration object. May not be <code>null</code>.
   * @param aJdbcConfig
   *        The JDBC configuration to act as a potential fallback for JDBC connection data. May not
   *        be <code>null</code>.
   */
  public SMPFlywayConfigurationBuilder (@NonNull final IConfigWithFallback aConfig,
                                        @NonNull final IJdbcConfiguration aJdbcConfig)
  {
    super (aConfig, FLYWAY_CONFIG_PREFIX);
    ValueEnforcer.notNull (aJdbcConfig, "JdbcConfig");

    // Fallback to main JDBC configuration values
    if (jdbcUrl () == null)
      jdbcUrl (aJdbcConfig.getJdbcUrl ());
    if (jdbcUser () == null)
      jdbcUser (aJdbcConfig.getJdbcUser ());
    if (jdbcPassword () == null)
      jdbcPassword (aJdbcConfig.getJdbcPassword ());

    // Fallback for schema-create: if the new key "smp.flyway.jdbc.schema-create" is not set,
    // check the old key "jdbc.schema-create"
    if (aConfig.getConfiguredValue (getConfigKeySchemaCreate ()) == null)
    {
      schemaCreate (aConfig.getAsBoolean (OLD_CONFIG_JDBC_SCHEMA_CREATE,
                                          FlywayConfiguration.DEFAULT_FLYWAY_JDBC_SCHEMA_CREATE));
    }
  }
}
