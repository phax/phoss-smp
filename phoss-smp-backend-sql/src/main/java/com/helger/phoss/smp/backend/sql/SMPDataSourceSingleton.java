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

import java.util.EnumSet;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringImplode;
import com.helger.db.api.EDatabaseSystemType;
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
  private static final EnumSet <EDatabaseSystemType> ALLOWED_DB_TYPES = EnumSet.of (EDatabaseSystemType.DB2,
                                                                                    EDatabaseSystemType.MYSQL,
                                                                                    EDatabaseSystemType.ORACLE,
                                                                                    EDatabaseSystemType.POSTGRESQL,
                                                                                    EDatabaseSystemType.SQLSERVER);
  private static final EDatabaseSystemType DB_TYPE;

  static
  {
    final String sDBType = SMPJDBCConfiguration.getTargetDatabaseType ();
    DB_TYPE = EDatabaseSystemType.getFromIDCaseInsensitiveOrNull (sDBType);
    if (DB_TYPE == null || !ALLOWED_DB_TYPES.contains (DB_TYPE))
      throw new IllegalStateException ("The database type MUST be provided and MUST be one of " +
                                       StringImplode.imploder ()
                                                    .source (ALLOWED_DB_TYPES, EDatabaseSystemType::getID)
                                                    .separator (", ")
                                                    .build () +
                                       " - provided value is '" +
                                       sDBType +
                                       "'");
  }

  /**
   * @return The database system determined from the configuration file. Never <code>null</code>.
   */
  @NonNull
  public static EDatabaseSystemType getDatabaseType ()
  {
    return DB_TYPE;
  }

  private final SMPDataSourceProvider m_aDSP = new SMPDataSourceProvider ();

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public SMPDataSourceSingleton ()
  {}

  @NonNull
  public static SMPDataSourceSingleton getInstance ()
  {
    return getGlobalSingleton (SMPDataSourceSingleton.class);
  }

  @Override
  protected void onBeforeDestroy (@NonNull final IScope aScopeToBeDestroyed) throws Exception
  {
    // Close the DataSource provider
    StreamHelper.close (m_aDSP);
  }

  /**
   * @return The singleton DataSource provider to use. Uses the configuration file to determine the
   *         settings.
   */
  @NonNull
  public SMPDataSourceProvider getDataSourceProvider ()
  {
    return m_aDSP;
  }
}
