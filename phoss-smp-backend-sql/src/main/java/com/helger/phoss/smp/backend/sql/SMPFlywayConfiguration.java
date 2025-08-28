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
package com.helger.phoss.smp.backend.sql;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.misc.Since;
import com.helger.annotation.style.PresentForCodeCoverage;
import com.helger.config.IConfig;
import com.helger.phoss.smp.config.SMPConfigProvider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * SMP Flyway configuration
 *
 * @author Philip Helger
 * @since 7.0.4
 */
@Immutable
public final class SMPFlywayConfiguration
{
  @Since ("5.4.0")
  public static final String CONFIG_SMP_FLYWAY_ENABLED = "smp.flyway.enabled";
  private static final boolean DEFAULT_SMP_FLYWAY_ENABLED = true;

  @Since ("7.0.4")
  private static final String CONFIG_SMP_FLYWAY_JDBC_USER = "smp.flyway.jdbc.user";
  @Since ("7.0.4")
  private static final String CONFIG_SMP_FLYWAY_JDBC_PASSWORD = "smp.flyway.jdbc.password";
  @Since ("7.0.4")
  private static final String CONFIG_SMP_FLYWAY_JDBC_URL = "smp.flyway.jdbc.url";

  @Since ("6.0.0")
  private static final String CONFIG_SMP_FLYWAY_BASELINE_VERSION = "smp.flyway.baseline.version";
  private static final int DEFAULT_SMP_FLYWAY_BASELINE_VERSION = 0;

  @PresentForCodeCoverage
  private static final SMPFlywayConfiguration INSTANCE = new SMPFlywayConfiguration ();

  private SMPFlywayConfiguration ()
  {}

  @Nonnull
  private static IConfig _getConfig ()
  {
    return SMPConfigProvider.getConfig ();
  }

  public static boolean isFlywayEnabled ()
  {
    return _getConfig ().getAsBoolean (CONFIG_SMP_FLYWAY_ENABLED, DEFAULT_SMP_FLYWAY_ENABLED);
  }

  public static int getFlywayBaselineVersion ()
  {
    return _getConfig ().getAsInt (CONFIG_SMP_FLYWAY_BASELINE_VERSION, DEFAULT_SMP_FLYWAY_BASELINE_VERSION);
  }

  @Nullable
  public static String getFlywayJdbcUser ()
  {
    final String ret = _getConfig ().getAsString (CONFIG_SMP_FLYWAY_JDBC_USER);
    return ret != null ? ret : SMPJDBCConfiguration.getJdbcUser ();
  }

  @Nullable
  public static String getFlywayJdbcPassword ()
  {
    final String ret = _getConfig ().getAsString (CONFIG_SMP_FLYWAY_JDBC_PASSWORD);
    return ret != null ? ret : SMPJDBCConfiguration.getJdbcPassword ();
  }

  @Nullable
  public static String getFlywayJdbcUrl ()
  {
    final String ret = _getConfig ().getAsString (CONFIG_SMP_FLYWAY_JDBC_URL);
    return ret != null ? ret : SMPJDBCConfiguration.getJdbcUrl ();
  }
}
