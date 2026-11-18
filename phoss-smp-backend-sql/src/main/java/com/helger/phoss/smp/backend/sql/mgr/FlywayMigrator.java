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
package com.helger.phoss.smp.backend.sql.mgr;

import java.util.concurrent.CompletableFuture;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.internal.info.MigrationInfoImpl;
import org.flywaydb.core.internal.jdbc.DriverDataSource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.base.string.StringHelper;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.api.config.IJdbcConfiguration;
import com.helger.db.flyway.FlywayMigrationRunner;
import com.helger.db.flyway.IFlywayConfiguration;
import com.helger.phoss.smp.backend.sql.migration.V10__MigrateRolesToDB;
import com.helger.phoss.smp.backend.sql.migration.V11__MigrateUsersToDB;
import com.helger.phoss.smp.backend.sql.migration.V12__MigrateUserGroupsToDB;
import com.helger.phoss.smp.backend.sql.migration.V14__MigrateSettingsToDB;
import com.helger.phoss.smp.backend.sql.migration.V15__MigrateDBUsersToPhotonUsers;
import com.helger.phoss.smp.backend.sql.migration.V21__MigrateUserTokensToDB;
import com.helger.phoss.smp.backend.sql.migration.V25__MigrateSMLInfoToDB;
import com.helger.phoss.smp.backend.sql.migration.V27__MigrateSystemMigrationsToDB;
import com.helger.phoss.smp.backend.sql.migration.V29__MigrateSystemMessageToDB;
import com.helger.phoss.smp.backend.sql.migration.V2__MigrateDBUsersToPhotonUsers;
import com.helger.phoss.smp.backend.sql.migration.V31__MigrateLongRunningJobsToDB;
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
  // Indirection level to not load org.flyway classes by default
  @Immutable
  public static final class Singleton
  {
    static final FlywayMigrator INSTANCE = new FlywayMigrator ();

    private Singleton ()
    {}
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (FlywayMigrator.class);

  private FlywayMigrator ()
  {}

  @NonNull
  private static String _getLocation (@NonNull final EDatabaseSystemType eDBType)
  {
    return "db/migrate-" + eDBType.getID ();
  }

  @NonNull
  private static JavaMigration @NonNull [] _getAllJavaMigrations ()
  {
    // Avoid scanning the ClassPath by enumerating Java migrations explicitly
    return new JavaMigration [] { new V2__MigrateDBUsersToPhotonUsers (),
                                  new V5__MigrateTransportProfilesToDB (),
                                  new V10__MigrateRolesToDB (),
                                  new V11__MigrateUsersToDB (),
                                  new V12__MigrateUserGroupsToDB (),
                                  new V14__MigrateSettingsToDB (),
                                  new V15__MigrateDBUsersToPhotonUsers (),
                                  new V21__MigrateUserTokensToDB (),
                                  new V25__MigrateSMLInfoToDB (),
                                  new V27__MigrateSystemMigrationsToDB (),
                                  new V29__MigrateSystemMessageToDB (),
                                  new V31__MigrateLongRunningJobsToDB () };
  }

  void runFlyway (@NonNull final EDatabaseSystemType eDBType,
                  @NonNull final IJdbcConfiguration aJdbcConfig,
                  @NonNull final IFlywayConfiguration aFlywayConfig)
  {
    ValueEnforcer.notNull (eDBType, "DBType");
    ValueEnforcer.notNull (aJdbcConfig, "JdbcConfig");
    ValueEnforcer.notNull (aFlywayConfig, "FlywayConfig");

    // SMP-specific audit callback
    final Callback aCallbackAudit = new BaseCallback ()
    {
      public void handle (@NonNull final Event aEvent, @Nullable final Context aContext)
      {
        if (aEvent == Event.AFTER_EACH_MIGRATE && aContext != null)
        {
          final var aMI = aContext.getMigrationInfo ();
          if (aMI instanceof final MigrationInfoImpl aMII)
          {
            final ResolvedMigration aRM = aMII.getResolvedMigration ();
            // Version 6 establishes the audit table - so don't audit anything
            // before that version
            if (aRM != null && aRM.getVersion ().isAtLeast ("7"))
            {
              // if migration affects the table smp_audit directly (live V36 migration),
              // then this insert gets stuck because the migration uses transaction that is in
              // progress
              // causing a deadlock. So we run the audit insert in a separate thread to avoid this.

              CompletableFuture.supplyAsync (() -> {
                try
                {
                  AuditHelper.onAuditExecuteSuccess ("sql-migration-success",
                                                     aRM.getVersion ().toString (),
                                                     aRM.getDescription (),
                                                     aRM.getScript (),
                                                     aRM.getType ().name (),
                                                     aRM.getPhysicalLocation ());
                }
                catch (final Exception e)
                {
                  LOGGER.error ("Error during audit logging: " + e.getMessage ());
                }
                return Boolean.TRUE;
              });
            }
          }
        }
      }
    };

    FlywayMigrationRunner.runFlyway (aJdbcConfig,
                                     aFlywayConfig,
                                     _getLocation (eDBType),
                                     _getAllJavaMigrations (),
                                     new Callback [] { FlywayMigrationRunner.CALLBACK_LOGGING, aCallbackAudit });
  }

  /**
   * Check that the database schema contains all migrations known to this version, without modifying
   * the database. This is used in read-only mode instead of {@link #runFlyway}. The Flyway
   * configuration is built like in <code>FlywayMigrationRunner</code> of ph-db-flyway, but only the
   * read-only <code>info</code> operation is invoked. Checksums are deliberately not validated.
   *
   * @param eDBType
   *        Database type. May not be <code>null</code>.
   * @param aJdbcConfig
   *        JDBC configuration. May not be <code>null</code>.
   * @param aFlywayConfig
   *        Flyway configuration. May not be <code>null</code>.
   * @throws InitializationException
   *         If at least one migration is pending
   * @since 8.8.0
   */
  void checkNoPendingMigrations (@NonNull final EDatabaseSystemType eDBType,
                                 @NonNull final IJdbcConfiguration aJdbcConfig,
                                 @NonNull final IFlywayConfiguration aFlywayConfig)
  {
    ValueEnforcer.notNull (eDBType, "DBType");
    ValueEnforcer.notNull (aJdbcConfig, "JdbcConfig");
    ValueEnforcer.notNull (aFlywayConfig, "FlywayConfig");

    final String sLocation = _getLocation (eDBType);
    LOGGER.info ("Checking for pending Flyway migrations with location '" + sLocation + "'");

    final FluentConfiguration aActualFlywayConfig = Flyway.configure ()
                                                          .dataSource (new DriverDataSource (FlywayMigrator.class.getClassLoader (),
                                                                                             aJdbcConfig.getJdbcDriver (),
                                                                                             aFlywayConfig.getFlywayJdbcUrl (),
                                                                                             aFlywayConfig.getFlywayJdbcUser (),
                                                                                             aFlywayConfig.getFlywayJdbcPassword ()));
    aActualFlywayConfig.loggers ("slf4j");
    aActualFlywayConfig.baselineVersion (Integer.toString (aFlywayConfig.getFlywayBaselineVersion ()));
    aActualFlywayConfig.locations (sLocation);
    aActualFlywayConfig.javaMigrations (_getAllJavaMigrations ());

    // Use the schema only, if it is explicitly configured
    final String sSchema = aJdbcConfig.getJdbcSchema ();
    if (StringHelper.isNotEmpty (sSchema))
      aActualFlywayConfig.schemas (sSchema);

    // Custom history table name
    final String sHistoryTable = aFlywayConfig.getFlywayHistoryTable ();
    if (StringHelper.isNotEmpty (sHistoryTable))
      aActualFlywayConfig.table (sHistoryTable);

    final MigrationInfo [] aPending = aActualFlywayConfig.load ().info ().pending ();
    if (aPending.length > 0)
    {
      final StringBuilder aSB = new StringBuilder ();
      for (final MigrationInfo aMI : aPending)
      {
        if (aSB.length () > 0)
          aSB.append (", ");
        aSB.append (aMI.getVersion ()).append (" (").append (aMI.getDescription ()).append (')');
      }
      throw new InitializationException ("The database schema is not up to date. " +
                                         aPending.length +
                                         " Flyway migration(s) are pending: " +
                                         aSB.toString () +
                                         ". Start a writable SMP instance on the same database first.");
    }

    LOGGER.info ("No Flyway migrations are pending");
  }
}
