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
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.state.ETriState;
import com.helger.commons.string.ToStringGenerator;
import com.helger.dao.DAOException;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.backend.sql.EDatabaseType;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.phoss.smp.backend.sql.SMPDataSourceSingleton;
import com.helger.phoss.smp.backend.sql.SMPJDBCConfiguration;
import com.helger.phoss.smp.domain.ISMPManagerProvider;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.sml.SMLInfoManagerXML;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.photon.jdbc.PhotonSecurityManagerFactoryJDBC;
import com.helger.settings.exchange.configfile.ConfigFile;

/**
 * A JDBC based implementation of the {@link ISMPManagerProvider} interface.
 *
 * @author Philip Helger
 * @since 5.3.0
 */
public final class SMPManagerProviderSQL implements ISMPManagerProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPManagerProviderSQL.class);

  private static final String SML_INFO_XML = "sml-info.xml";

  private final EDatabaseType m_eDBType;

  public SMPManagerProviderSQL ()
  {
    m_eDBType = SMPDataSourceSingleton.getDatabaseType ();
  }

  @Override
  public void beforeInitManagers ()
  {
    final ConfigFile aCF = SMPServerConfiguration.getConfigFile ();

    // Set the special PhotonSecurityManager factory
    // Must be before Flyway, so that auditing of Flyway actions (may) work
    PhotonSecurityManagerFactoryJDBC.install (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_CUSTOMIZER);

    // Flyway migration is enabled by default
    if (aCF.getAsBoolean (SMPJDBCConfiguration.CONFIG_SMP_FLYWAY_ENABLED, true))
      FlywayMigrator.Singleton.INSTANCE.runFlyway (m_eDBType);
    else
      LOGGER.warn ("Flyway Migration is disabled according to the configuration item " + SMPJDBCConfiguration.CONFIG_SMP_FLYWAY_ENABLED);

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

  @Nonnull
  public ISMPSettingsManager createSettingsMgr ()
  {
    return new SMPSettingsManagerJDBC (SMPDBExecutor::new);
  }

  @Nonnull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    return new SMPTransportProfileManagerJDBC (SMPDBExecutor::new);
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

  @Nonnull
  public ISMPParticipantMigrationManager createParticipantMigrationMgr ()
  {
    return new SMPParticipantMigrationManagerJDBC (SMPDBExecutor::new);
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
