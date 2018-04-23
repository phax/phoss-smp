/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.data.sql;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.eclipse.persistence.config.PersistenceUnitProperties;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.commons.annotation.Since;

/**
 * Default JPA configuration file properties
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Immutable
public final class SMPJPAConfiguration
{
  public static final String CONFIG_JDBC_DRIVER = "jdbc.driver";
  public static final String CONFIG_JDBC_URL = "jdbc.url";
  public static final String CONFIG_JDBC_USER = "jdbc.user";
  public static final String CONFIG_JDBC_PASSWORD = "jdbc.password";
  public static final String CONFIG_TARGET_DATABASE = "target-database";
  public static final String CONFIG_JDBC_READ_CONNECTIONS_MAX = "jdbc.read-connections.max";
  public static final String CONFIG_DDL_GENERATION_MODE = PersistenceUnitProperties.DDL_GENERATION_MODE;
  @Since ("5.0.6")
  public static final String CONFIG_JDBC_EXECUTION_TIME_WARNING_ENABLE = "jdbc.execution-time-warning.enabled";
  @Since ("5.0.6")
  public static final String CONFIG_JDBC_EXECUTION_TIME_WARNING_MS = "jdbc.execution-time-warning.ms";

  @PresentForCodeCoverage
  private static final SMPJPAConfiguration s_aInstance = new SMPJPAConfiguration ();

  private SMPJPAConfiguration ()
  {}

  // Write SQL file only in debug mode, so that the production version can be
  // read-only!
  @Nonnull
  public static String getDefaultDDLGenerationMode ()
  {
    return PersistenceUnitProperties.NONE;
  }
}
