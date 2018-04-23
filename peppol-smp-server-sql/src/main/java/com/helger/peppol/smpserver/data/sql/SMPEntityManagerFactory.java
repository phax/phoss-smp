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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.persistence.PersistenceException;

import org.eclipse.persistence.config.PersistenceUnitProperties;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.db.jpa.AbstractGlobalEntityManagerFactory;
import com.helger.db.jpa.JPAEnabledManager;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.scope.IScope;
import com.helger.settings.exchange.configfile.ConfigFile;

/**
 * Specific SMP JPA entity manager factory
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class SMPEntityManagerFactory extends AbstractGlobalEntityManagerFactory
{
  @Nonnull
  @ReturnsMutableCopy
  private static Map <String, Object> _createPropertiesMap ()
  {
    // Standard configuration file
    final ConfigFile aConfigFile = SMPServerConfiguration.getConfigFile ();

    final ICommonsMap <String, Object> ret = new CommonsHashMap <> ();
    // Read all properties from the standard configuration file
    // Connection pooling
    ret.put (PersistenceUnitProperties.CONNECTION_POOL_MAX,
             aConfigFile.getAsString (SMPJPAConfiguration.CONFIG_JDBC_READ_CONNECTIONS_MAX));

    // EclipseLink should create the database schema automatically
    // Values: Values: none/create-tables/drop-and-create-tables
    ret.put (PersistenceUnitProperties.DDL_GENERATION, PersistenceUnitProperties.DROP_AND_CREATE);
    ret.put (PersistenceUnitProperties.DDL_GENERATION_MODE,
             aConfigFile.getAsString (SMPJPAConfiguration.CONFIG_DDL_GENERATION_MODE,
                                      SMPJPAConfiguration.getDefaultDDLGenerationMode ()));
    ret.put (PersistenceUnitProperties.CREATE_JDBC_DDL_FILE, "db-create-smp.sql");
    ret.put (PersistenceUnitProperties.DROP_JDBC_DDL_FILE, "db-drop-smp.sql");

    // Use an isolated cache
    // (http://code.google.com/p/peppol-silicone/issues/detail?id=6)
    ret.put (PersistenceUnitProperties.CACHE_SHARED_DEFAULT, "false");

    // Enable this line for SQL debug logging
    if (false)
      ret.put (PersistenceUnitProperties.LOGGING_LEVEL, "finer");

    return ret;
  }

  @Deprecated
  @UsedViaReflection
  public SMPEntityManagerFactory ()
  {
    super (SMPServerConfiguration.getConfigFile ().getAsString (SMPJPAConfiguration.CONFIG_JDBC_DRIVER),
           SMPServerConfiguration.getConfigFile ().getAsString (SMPJPAConfiguration.CONFIG_JDBC_URL),
           SMPServerConfiguration.getConfigFile ().getAsString (SMPJPAConfiguration.CONFIG_JDBC_USER),
           SMPServerConfiguration.getConfigFile ().getAsString (SMPJPAConfiguration.CONFIG_JDBC_PASSWORD),
           SMPServerConfiguration.getConfigFile ().getAsString (SMPJPAConfiguration.CONFIG_TARGET_DATABASE),
           "peppol-smp",
           _createPropertiesMap ());

    // Set execution time stuff
    JPAEnabledManager.setDefaultExecutionWarnTimeEnabled (SMPServerConfiguration.getConfigFile ()
                                                                                .getAsBoolean (SMPJPAConfiguration.CONFIG_JDBC_EXECUTION_TIME_WARNING_ENABLE,
                                                                                               JPAEnabledManager.DEFAULT_EXECUTION_WARN_ENABLED));
    JPAEnabledManager.setDefaultExecutionWarnTime (SMPServerConfiguration.getConfigFile ()
                                                                         .getAsInt (SMPJPAConfiguration.CONFIG_JDBC_EXECUTION_TIME_WARNING_MS,
                                                                                    JPAEnabledManager.DEFAULT_EXECUTION_WARN_TIME_MS));
  }

  @Nonnull
  public static SMPEntityManagerFactory getInstance ()
  {
    return getGlobalSingleton (SMPEntityManagerFactory.class);
  }

  @Override
  @OverridingMethodsMustInvokeSuper
  protected void onDestroy (@Nonnull final IScope aScopeInDestruction) throws Exception
  {
    try
    {
      super.onDestroy (aScopeInDestruction);
    }
    catch (final PersistenceException ex)
    {
      // Ignore
    }
  }
}
