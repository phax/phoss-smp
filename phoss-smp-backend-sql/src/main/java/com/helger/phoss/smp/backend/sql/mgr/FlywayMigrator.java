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
package com.helger.phoss.smp.backend.sql.mgr;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.api.logging.LogLevel;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.internal.info.MigrationInfoImpl;
import org.flywaydb.core.internal.jdbc.DriverDataSource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.phoss.smp.backend.sql.SMPFlywayConfiguration;
import com.helger.phoss.smp.backend.sql.SMPJDBCConfiguration;
import com.helger.phoss.smp.backend.sql.migration.V10__MigrateRolesToDB;
import com.helger.phoss.smp.backend.sql.migration.V11__MigrateUsersToDB;
import com.helger.phoss.smp.backend.sql.migration.V12__MigrateUserGroupsToDB;
import com.helger.phoss.smp.backend.sql.migration.V14__MigrateSettingsToDB;
import com.helger.phoss.smp.backend.sql.migration.V15__MigrateDBUsersToPhotonUsers;
import com.helger.phoss.smp.backend.sql.migration.V21__MigrateUserTokensToDB;
import com.helger.phoss.smp.backend.sql.migration.V2__MigrateDBUsersToPhotonUsers;
import com.helger.phoss.smp.backend.sql.migration.V5__MigrateTransportProfilesToDB;
import com.helger.photon.audit.AuditHelper;

/**
 * This class has the sole purpose of encapsulating the org.flywaydb classes, so that it's usage can
 * be turned off (for whatever reason).
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

  void runFlyway (@NonNull final EDatabaseSystemType eDBType)
  {
    ValueEnforcer.notNull (eDBType, "DBType");

    LOGGER.info ("Starting to run Flyway for DB type " + eDBType);

    final Callback aCallbackLogging = new BaseCallback ()
    {
      public void handle (@NonNull final Event aEvent, @Nullable final Context aContext)
      {
        LOGGER.info ("Flyway: Event " + aEvent.getId ());
        if (aEvent == Event.AFTER_EACH_MIGRATE && aContext != null)
        {
          final MigrationInfo aMI = aContext.getMigrationInfo ();
          if (aMI instanceof MigrationInfoImpl)
          {
            final ResolvedMigration aRM = ((MigrationInfoImpl) aMI).getResolvedMigration ();
            if (aRM != null)
              LOGGER.info ("  Performed migration: " + aRM);
          }
        }
      }
    };
    final Callback aCallbackAudit = new BaseCallback ()
    {
      public void handle (@NonNull final Event aEvent, @Nullable final Context aContext)
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

    // The JDBC driver is the same as for main connection
    final FluentConfiguration aFlywayConfig = Flyway.configure ()
                                                    .dataSource (new DriverDataSource (FlywayMigrator.class.getClassLoader (),
                                                                                       SMPJDBCConfiguration.getJdbcDriver (),
                                                                                       SMPFlywayConfiguration.getFlywayJdbcUrl (),
                                                                                       SMPFlywayConfiguration.getFlywayJdbcUser (),
                                                                                       SMPFlywayConfiguration.getFlywayJdbcPassword ()));

    // Required for creating DB tables
    aFlywayConfig.baselineOnMigrate (true);

    // Disable validation, because DDL comments are also taken into
    // consideration
    aFlywayConfig.validateOnMigrate (false);

    // Version 1 is the baseline
    aFlywayConfig.baselineVersion (Integer.toString (SMPFlywayConfiguration.getFlywayBaselineVersion ()))
                 .baselineDescription ("SMP 5.2.x database layout, MySQL only");

    // Separate directory per DB type
    aFlywayConfig.locations ("db/migrate-" + eDBType.getID ());

    // Avoid scanning the ClassPath by enumerating them explicitly
    aFlywayConfig.javaMigrations (new V2__MigrateDBUsersToPhotonUsers (),
                                  new V5__MigrateTransportProfilesToDB (),
                                  new V10__MigrateRolesToDB (),
                                  new V11__MigrateUsersToDB (),
                                  new V12__MigrateUserGroupsToDB (),
                                  new V14__MigrateSettingsToDB (),
                                  new V15__MigrateDBUsersToPhotonUsers (),
                                  new V21__MigrateUserTokensToDB ());

    // Callbacks
    aFlywayConfig.callbacks (aCallbackLogging, aCallbackAudit);

    // Flyway to handle the DB schema?
    final String sSchema = SMPJDBCConfiguration.getJdbcSchema ();
    if (StringHelper.isNotEmpty (sSchema))
    {
      // Use the schema only, if it is explicitly configured
      // The default schema name is ["$user", public] and as such unusable
      aFlywayConfig.schemas (sSchema);
    }
    // If no schema is specified, schema create should also be disabled
    aFlywayConfig.createSchemas (SMPJDBCConfiguration.isJdbcSchemaCreate ());

    // Enable for more verbosity
    if (false)
      LogFactory.setLogLevel (LogLevel.DEBUG);

    final Flyway aFlyway = aFlywayConfig.load ();
    if (false)
      aFlyway.validate ();

    // In case of a failed migration only
    if (false)
    {
      aFlyway.info ();
      aFlyway.repair ();
    }

    aFlyway.migrate ();

    LOGGER.info ("Finished running Flyway");
  }
}
