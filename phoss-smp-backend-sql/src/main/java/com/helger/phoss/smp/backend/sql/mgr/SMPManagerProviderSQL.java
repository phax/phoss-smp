/**
 * Copyright (C) 2019-2021 Philip Helger and contributors
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
import javax.annotation.Nullable;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.internal.jdbc.DriverDataSource;

import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.dao.DAOException;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.backend.sql.EDatabaseType;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.phoss.smp.backend.sql.SMPDataSourceSingleton;
import com.helger.phoss.smp.backend.sql.SMPJDBCConfiguration;
import com.helger.phoss.smp.backend.sql.migration.V2__MigrateDBUsersToPhotonUsers;
import com.helger.phoss.smp.domain.ISMPManagerProvider;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.sml.SMLInfoManagerXML;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.domain.transportprofile.SMPTransportProfileManagerXML;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.phoss.smp.settings.SMPSettingsManagerXML;
import com.helger.settings.exchange.configfile.ConfigFile;

/**
 * A JDBC based implementation of the {@link ISMPManagerProvider} interface.
 *
 * @author Philip Helger
 * @since 5.3.0
 */
public final class SMPManagerProviderSQL implements ISMPManagerProvider
{
  private static final String SML_INFO_XML = "sml-info.xml";
  private static final String SMP_SETTINGS_XML = "smp-settings.xml";
  private static final String SMP_TRANSPORT_PROFILES_XML = "transportprofiles.xml";

  private final EDatabaseType m_eDBType;

  public SMPManagerProviderSQL ()
  {
    m_eDBType = SMPDataSourceSingleton.getDatabaseType ();
  }

  public void beforeInitManagers ()
  {
    final ConfigFile aCF = SMPServerConfiguration.getConfigFile ();
    final FluentConfiguration aConfig = Flyway.configure ()
                                              .dataSource (new DriverDataSource (getClass ().getClassLoader (),
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
                                              .locations ("db/migrate-" + m_eDBType.getID ())
                                              /*
                                               * Avoid scanning the ClassPath by
                                               * enumerating them explicitly
                                               */
                                              .javaMigrations (new V2__MigrateDBUsersToPhotonUsers ());

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

    // Register this here, so that the SMPMetaManager is available
    DBExecutor.setConnectionStatusChangeCallback ( (eOld, eNew) -> {
      // false: don't trigger callback, because the source is DBExecutor
      SMPMetaManager.getInstance ().setBackendConnectionState (eNew, false);
    });

    // Allow communicating in the other direction as well
    SMPMetaManager.getInstance ().setBackendConnectionStateChangeCallback (eNew -> DBExecutor.resetConnectionEstablished ());
  }

  @Nonnull
  public ETriState getBackendConnectionEstablishedDefaultState ()
  {
    return ETriState.UNDEFINED;
  }

  // TODO currently also file based
  @Nonnull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    try
    {
      return new SMLInfoManagerXML (SML_INFO_XML);
    }
    catch (final DAOException ex)
    {
      throw new IllegalStateException (ex.getMessage (), ex);
    }
  }

  // TODO currently also file based
  @Nonnull
  public ISMPSettingsManager createSettingsMgr ()
  {
    try
    {
      return new SMPSettingsManagerXML (SMP_SETTINGS_XML);
    }
    catch (final DAOException ex)
    {
      throw new IllegalStateException (ex.getMessage (), ex);
    }
  }

  // TODO currently also file based
  @Nonnull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    try
    {
      return new SMPTransportProfileManagerXML (SMP_TRANSPORT_PROFILES_XML);
    }
    catch (final DAOException ex)
    {
      throw new IllegalStateException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    final SMPServiceGroupManagerJDBC ret = new SMPServiceGroupManagerJDBC (SMPDBExecutor::new);
    // Enable cache by default
    ret.setCacheEnabled (SMPServerConfiguration.getConfigFile ().getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_CACHE_SG_ENABLED, true));
    return ret;
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPRedirectManagerJDBC (SMPDBExecutor::new, aServiceGroupMgr);
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                                    @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPServiceInformationManagerJDBC (SMPDBExecutor::new, aServiceGroupMgr);
  }

  @Nullable
  public ISMPBusinessCardManager createBusinessCardMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                        @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPBusinessCardManagerJDBC (SMPDBExecutor::new);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("DBType", m_eDBType).getToString ();
  }
}
