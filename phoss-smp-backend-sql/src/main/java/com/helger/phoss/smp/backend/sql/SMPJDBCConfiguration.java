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
  public static final String CONFIG_TARGET_DATABASE = "target-database";
  @Deprecated
  // Since ("5.2.4")
  public static final String CONFIG_JDBC_READ_CONNECTIONS_MAX = "jdbc.read-connections.max";
  @Since ("5.0.6")
  public static final String CONFIG_JDBC_EXECUTION_TIME_WARNING_ENABLE = "jdbc.execution-time-warning.enabled";
  @Since ("5.0.6")
  public static final String CONFIG_JDBC_EXECUTION_TIME_WARNING_MS = "jdbc.execution-time-warning.ms";

  @PresentForCodeCoverage
  private static final SMPJDBCConfiguration s_aInstance = new SMPJDBCConfiguration ();

  private SMPJDBCConfiguration ()
  {}
}
