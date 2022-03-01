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
package com.helger.phoss.smp.backend.sql.mgr;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.internal.info.MigrationInfoImpl;
import org.flywaydb.core.internal.jdbc.DriverDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.StringHelper;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.backend.sql.EDatabaseType;
import com.helger.phoss.smp.backend.sql.SMPJDBCConfiguration;
import com.helger.phoss.smp.backend.sql.migration.V10__MigrateRolesToDB;
import com.helger.phoss.smp.backend.sql.migration.V11__MigrateUsersToDB;
import com.helger.phoss.smp.backend.sql.migration.V12__MigrateUserGroupsToDB;
import com.helger.phoss.smp.backend.sql.migration.V14__MigrateSettingsToDB;
import com.helger.phoss.smp.backend.sql.migration.V15__MigrateDBUsersToPhotonUsers;
import com.helger.phoss.smp.backend.sql.migration.V2__MigrateDBUsersToPhotonUsers;
import com.helger.phoss.smp.backend.sql.migration.V5__MigrateTransportProfilesToDB;
import com.helger.photon.audit.AuditHelper;
import com.helger.settings.exchange.configfile.ConfigFile;

/**
 * This class has the sole purpose of encapsulating the org.flywaydb classes, so
 * that it's usage can be turned off (for whatever reason).
 *
 * @author Philip Helger
 */
final class FlywayMigrator
{
  private static final Logger LOGGER = LoggerFactory.getLogger (FlywayMigrator.Singleton.class);

  // Indirection level to not load org.flyway classes by default
  @Immutable
  public static final class Singleton
  {
    static final FlywayMigrator INSTANCE = new FlywayMigrator ();

    private Singleton ()
    {}
  }

  private FlywayMigrator ()
  {}

  void runFlyway (@Nonnull final EDatabaseType eDBType)
  {
    ValueEnforcer.notNull (eDBType, "DBType");

    LOGGER.info ("Starting to run Flyway for DB type " + eDBType);

    final ConfigFile aCF = SMPServerConfiguration.getConfigFile ();
    final Callback aCallbackLogging = new BaseCallback ()
    {
      public void handle (@Nonnull final Event aEvent, @Nonnull final Context aContext)
      {
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Flyway: Event " + aEvent.getId ());

        if (aEvent == Event.AFTER_EACH_MIGRATE && aContext != null)
        {
          final MigrationInfo aMI = aContext.getMigrationInfo ();
          if (aMI instanceof MigrationInfoImpl)
          {
            final ResolvedMigration aRM = ((MigrationInfoImpl) aMI).getResolvedMigration ();
            if (aRM != null)
              if (LOGGER.isInfoEnabled ())
                LOGGER.info ("  Performed migration: " + aRM);
          }
        }
      }
    };
    final Callback aCallbackAudit = new BaseCallback ()
    {
      public void handle (@Nonnull final Event aEvent, @Nonnull final Context aContext)
      {
        if (aEvent == Event.AFTER_EACH_MIGRATE && aContext != null)
        {
          final MigrationInfo aMI = aContext.getMigrationInfo ();
          if (aMI instanceof MigrationInfoImpl)
          {
            final ResolvedMigration aRM = ((MigrationInfoImpl) aMI).getResolvedMigration ();
            // Version 6 establishes the audit table - so don't audit anything
            // before that version
            if (aRM != null && aRM.getVersion ().isAtLeast ("7"))
              AuditHelper.onAuditExecuteSuccess ("sql-migration-success",
                                                 aRM.getVersion ().toString (),
                                                 aRM.getDescription (),
                                                 aRM.getScript (),
                                                 aRM.getType ().name (),
                                                 aRM.getPhysicalLocation ());
          }
        }
      }
    };

    final FluentConfiguration aConfig = Flyway.configure ()
                                              .dataSource (new DriverDataSource (FlywayMigrator.class.getClassLoader (),
                                                                                 aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_DRIVER),
                                                                                 aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_URL),
                                                                                 aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_USER),
                                                                                 aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_PASSWORD)))
                                              // Required for creating DB table
                                              .baselineOnMigrate (true)
                                              // Disable validation, because
                                              // DDL comments are also taken
                                              // into consideration
                                              .validateOnMigrate (false)
                                              // Version 1 is the baseline
                                              .baselineVersion ("1")
                                              .baselineDescription ("SMP 5.2.x database layout, MySQL only")
                                              // Separate directory per DB type
                                              .locations ("db/migrate-" + eDBType.getID ())
                                              /*
                                               * Avoid scanning the ClassPath by
                                               * enumerating them explicitly
                                               */
                                              .javaMigrations (new V2__MigrateDBUsersToPhotonUsers (),
                                                               new V5__MigrateTransportProfilesToDB (),
                                                               new V10__MigrateRolesToDB (),
                                                               new V11__MigrateUsersToDB (),
                                                               new V12__MigrateUserGroupsToDB (),
                                                               new V14__MigrateSettingsToDB (),
                                                               new V15__MigrateDBUsersToPhotonUsers ())
                                              .callbacks (aCallbackLogging, aCallbackAudit);

    // Flyway to handle the DB schema?
    final String sSchema = aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_SCHEMA);
    if (StringHelper.hasText (sSchema))
    {
      // Use the schema only, if it is explicitly configured
      // The default schema name is ["$user", public] and as such unusable
      aConfig.schemas (sSchema);
    }

    // If no schema is specified, schema create should also be disabled
    final boolean bCreateSchema = aCF.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_SCHEMA_CREATE, false);
    aConfig.createSchemas (bCreateSchema);

    final Flyway aFlyway = aConfig.load ();
    if (false)
      aFlyway.validate ();
    aFlyway.migrate ();

    LOGGER.info ("Finished running Flyway");
  }
}
