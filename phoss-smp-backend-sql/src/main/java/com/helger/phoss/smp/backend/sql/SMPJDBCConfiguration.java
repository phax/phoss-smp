/**
 * Copyright (C) 2019-2021 Philip Helger and contributors
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

import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.commons.annotation.Since;

/**
 * Default JPA configuration file properties
 *
 * @author Philip Helger
 */
@Immutable
public final class SMPJDBCConfiguration
{
  public static final String CONFIG_JDBC_DRIVER = "jdbc.driver";
  public static final String CONFIG_JDBC_URL = "jdbc.url";
  public static final String CONFIG_JDBC_USER = "jdbc.user";
  public static final String CONFIG_JDBC_PASSWORD = "jdbc.password";
  @Since ("5.3.0")
  public static final String CONFIG_JDBC_SCHEMA = "jdbc.schema";
  @Since ("5.3.0")
  public static final String CONFIG_JDBC_SCHEMA_CREATE = "jdbc.schema-create";
  public static final String CONFIG_TARGET_DATABASE = "target-database";

  @Since ("5.0.6")
  public static final String CONFIG_JDBC_EXECUTION_TIME_WARNING_ENABLE = "jdbc.execution-time-warning.enabled";
  public static final boolean DEFAULT_JDBC_EXECUTION_TIME_WARNING_ENABLE = true;

  @Since ("5.0.6")
  public static final String CONFIG_JDBC_EXECUTION_TIME_WARNING_MS = "jdbc.execution-time-warning.ms";

  // Experimental
  @Since ("5.3.0")
  public static final String CONFIG_JDBC_CACHE_SG_ENABLED = "jdbc.cache.sg.enabled";
  @Since ("5.3.0")
  public static final String CONFIG_JDBC_DEBUG_CONNECTIONS = "jdbc.debug.connections";
  @Since ("5.3.0")
  public static final String CONFIG_JDBC_DEBUG_TRANSACTIONS = "jdbc.debug.transactions";
  @Since ("5.3.0")
  public static final String CONFIG_JDBC_DEBUG_SQL = "jdbc.debug.sql";
  @Since ("5.3.3")
  public static final String CONFIG_SMP_FLYWAY_ENABLED = "smp.flyway.enabled";

  @PresentForCodeCoverage
  private static final SMPJDBCConfiguration INSTANCE = new SMPJDBCConfiguration ();

  private SMPJDBCConfiguration ()
  {}
}
