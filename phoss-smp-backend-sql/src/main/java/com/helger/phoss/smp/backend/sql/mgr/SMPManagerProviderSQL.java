/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.sql.mgr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.internal.jdbc.DriverDataSource;

import com.helger.commons.state.ETriState;
import com.helger.commons.string.ToStringGenerator;
import com.helger.dao.DAOException;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.backend.sql.SMPJDBCConfiguration;
import com.helger.phoss.smp.backend.sql.migration.V002__MigrateDBUsersToPhotonUsers;
import com.helger.phoss.smp.domain.ISMPManagerProvider;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.sml.SMLInfoManagerXML;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.domain.transportprofile.SMPTransportProfileManagerXML;
import com.helger.phoss.smp.domain.user.ISMPUserManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
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

  public SMPManagerProviderSQL ()
  {}

  public void beforeInitManagers ()
  {
    final ConfigFile aCF = SMPServerConfiguration.getConfigFile ();
    final Flyway aFlyway = Flyway.configure ()
                                 .dataSource (new DriverDataSource (getClass ().getClassLoader (),
                                                                    aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_DRIVER),
                                                                    aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_URL),
                                                                    aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_USER),
                                                                    aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_PASSWORD)))
                                 // Required for creating DB table
                                 .baselineOnMigrate (true)
                                 .baselineVersion ("1")
                                 .baselineDescription ("SMP 5.2.x database layout, MySQL only")
                                 /*
                                  * Avoid scanning the ClassPath by enumerating
                                  * them explicitly
                                  */
                                 .javaMigrations (new V002__MigrateDBUsersToPhotonUsers ())
                                 .load ();
    aFlyway.migrate ();
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
  public ISMPUserManager createUserMgr ()
  {
    // Use ph-oton
    return new SMPUserManagerPhoton ();
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    final SMPServiceGroupManagerJDBC ret = new SMPServiceGroupManagerJDBC ();
    // Enable cache by default
    ret.setCacheEnabled (SMPServerConfiguration.getConfigFile ().getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_CACHE_SG_ENABLED, true));
    return ret;
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPRedirectManagerJDBC (aServiceGroupMgr);
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                                    @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPServiceInformationManagerJDBC (aServiceGroupMgr);
  }

  @Nullable
  public ISMPBusinessCardManager createBusinessCardMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                        @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPBusinessCardManagerJDBC ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
