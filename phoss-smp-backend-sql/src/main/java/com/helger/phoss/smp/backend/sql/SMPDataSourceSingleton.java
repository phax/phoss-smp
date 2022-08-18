/*
 * Copyright (C) 2019-2022 Philip Helger and contributors
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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.string.StringHelper;
import com.helger.phoss.smp.SMPConfigSource;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * DataSource provider singleton
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class SMPDataSourceSingleton extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPDataSourceSingleton.class);
  private static final EDatabaseType DB_TYPE;

  static
  {
    final String sDBType = SMPConfigSource.getConfig ().getAsString (SMPJDBCConfiguration.CONFIG_TARGET_DATABASE);
    DB_TYPE = EDatabaseType.getFromIDOrNull (sDBType);
    if (DB_TYPE == null)
      throw new IllegalStateException ("The database type MUST be provided and MUST be one of " +
                                       StringHelper.imploder ()
                                                   .source (EDatabaseType.values (), EDatabaseType::getID)
                                                   .separator (", ")
                                                   .build () +
                                       " - provided value is '" +
                                       sDBType +
                                       "'");
    if (DB_TYPE == EDatabaseType.DB2)
    {
      // TODO DB2
      if (GlobalDebug.isProductionMode ())
        throw new IllegalStateException ("DB2 is not yet ready for production");
      LOGGER.warn ("The DB2 version is NOT YET ready for use!");
    }
  }

  /**
   * @return The database system determined from the configuration file. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static EDatabaseType getDatabaseType ()
  {
    return DB_TYPE;
  }

  private final SMPDataSourceProvider m_aDSP = new SMPDataSourceProvider ();

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated
  @UsedViaReflection
  public SMPDataSourceSingleton ()
  {}

  @Nonnull
  public static SMPDataSourceSingleton getInstance ()
  {
    return getGlobalSingleton (SMPDataSourceSingleton.class);
  }

  @Override
  protected void onBeforeDestroy (@Nonnull final IScope aScopeToBeDestroyed) throws Exception
  {
    // Close the DataSource provider
    StreamHelper.close (m_aDSP);
  }

  /**
   * @return The singleton DataSource provider to use. Uses the configuration
   *         file to determine the settings.
   */
  @Nonnull
  public SMPDataSourceProvider getDataSourceProvider ()
  {
    return m_aDSP;
  }
}
