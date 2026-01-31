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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ETriState;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.dao.DAOException;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.phoss.smp.backend.sql.SMPDataSourceSingleton;
import com.helger.phoss.smp.backend.sql.SMPFlywayConfiguration;
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

  private final EDatabaseSystemType m_eDBType;

  public SMPManagerProviderSQL ()
  {
    m_eDBType = SMPDataSourceSingleton.getDatabaseType ();
  }

  @Override
  public void beforeInitManagers ()
  {
    // Set the special PhotonSecurityManager factory
    // Must be before Flyway, so that auditing of Flyway actions (may) work
    PhotonSecurityManagerFactoryJDBC.install (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_CUSTOMIZER);

    // Flyway migration is enabled by default
    if (SMPFlywayConfiguration.isFlywayEnabled ())
      FlywayMigrator.Singleton.INSTANCE.runFlyway (m_eDBType);
    else
      LOGGER.warn ("Flyway Migration is disabled according to the configuration item " +
                   SMPFlywayConfiguration.CONFIG_SMP_FLYWAY_ENABLED);

    // Register this here, so that the SMPMetaManager is available
    DBExecutor.setConnectionStatusChangeCallback ( (eOld, eNew) ->
    // false: don't trigger callback, because the source is DBExecutor
    SMPMetaManager.getInstance ().setBackendConnectionState (eNew, false));

    // Allow communicating in the other direction as well
    SMPMetaManager.getInstance ()
                  .setBackendConnectionStateChangeCallback (eNew -> DBExecutor.resetConnectionEstablished ());
  }

  @NonNull
  public ETriState getBackendConnectionEstablishedDefaultState ()
  {
    return ETriState.UNDEFINED;
  }

  // TODO currently also file based
  @NonNull
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

  @NonNull
  public ISMPSettingsManager createSettingsMgr ()
  {
    return new SMPSettingsManagerJDBC (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_PREFIX);
  }

  @NonNull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    return new SMPTransportProfileManagerJDBC (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_PREFIX);
  }

  @NonNull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    final SMPServiceGroupManagerJDBC ret = new SMPServiceGroupManagerJDBC (SMPDBExecutor::new,
                                                                           SMPDBExecutor.TABLE_NAME_PREFIX);
    // Enable cache by default
    ret.setCacheEnabled (SMPJDBCConfiguration.isJdbcServiceGroupCacheEnabled ());
    return ret;
  }

  @NonNull
  public ISMPRedirectManager createRedirectMgr (@NonNull final IIdentifierFactory aIdentifierFactory)
  {
    return new SMPRedirectManagerJDBC (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_PREFIX);
  }

  @NonNull
  public ISMPServiceInformationManager createServiceInformationMgr (@NonNull final IIdentifierFactory aIdentifierFactory)
  {
    return new SMPServiceInformationManagerJDBC (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_PREFIX);
  }

  @NonNull
  public ISMPParticipantMigrationManager createParticipantMigrationMgr ()
  {
    return new SMPParticipantMigrationManagerJDBC (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_PREFIX);
  }

  @Nullable
  public ISMPBusinessCardManager createBusinessCardMgr (@NonNull final IIdentifierFactory aIdentifierFactory,
                                                        @NonNull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPBusinessCardManagerJDBC (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_PREFIX);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("DBType", m_eDBType).getToString ();
  }
}
