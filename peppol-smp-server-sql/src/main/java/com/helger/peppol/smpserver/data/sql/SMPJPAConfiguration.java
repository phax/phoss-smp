/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.data.sql;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.eclipse.persistence.config.PersistenceUnitProperties;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.commons.debug.GlobalDebug;

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

  @PresentForCodeCoverage
  private static final SMPJPAConfiguration s_aInstance = new SMPJPAConfiguration ();

  private SMPJPAConfiguration ()
  {}

  // Write SQL file only in debug mode, so that the production version can be
  // read-only!
  @Nonnull
  public static String getDefaultDDLGenerationMode ()
  {
    return GlobalDebug.isDebugMode () ? PersistenceUnitProperties.DDL_SQL_SCRIPT_GENERATION
                                      : PersistenceUnitProperties.NONE;
  }
}
