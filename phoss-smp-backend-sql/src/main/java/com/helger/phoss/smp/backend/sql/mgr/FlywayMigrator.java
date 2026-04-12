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

import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.internal.info.MigrationInfoImpl;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
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

  private FlywayMigrator ()
  {}

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

    // Avoid scanning the ClassPath by enumerating Java migrations explicitly
    final JavaMigration [] aJavaMigrations = { new V2__MigrateDBUsersToPhotonUsers (),
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

    FlywayMigrationRunner.runFlyway (aJdbcConfig,
                                     aFlywayConfig,
                                     "db/migrate-" + eDBType.getID (),
                                     aJavaMigrations,
                                     new Callback [] { FlywayMigrationRunner.CALLBACK_LOGGING, aCallbackAudit });
  }
}
