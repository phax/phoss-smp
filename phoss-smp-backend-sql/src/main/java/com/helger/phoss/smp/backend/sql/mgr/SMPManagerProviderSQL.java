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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.string.ToStringGenerator;
import com.helger.dao.DAOException;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.backend.sql.model.DBUser;
import com.helger.phoss.smp.domain.ISMPManagerProvider;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.sml.SMLInfoManagerXML;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.domain.transportprofile.SMPTransportProfileManagerXML;
import com.helger.phoss.smp.domain.user.ISMPUser;
import com.helger.phoss.smp.domain.user.ISMPUserManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.migration.CSMPServerMigrations;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.phoss.smp.settings.SMPSettingsManagerXML;
import com.helger.photon.app.io.WebFileIO;
import com.helger.photon.core.mgr.PhotonBasicManager;
import com.helger.photon.core.sysmigration.SystemMigrationManager;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.UserManager;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xml.microdom.util.XMLMapHandler;

/**
 * {@link ISMPManagerProvider} implementation for this backend.
 *
 * @author Philip Helger
 */
@SuppressWarnings ("deprecation")
public final class SMPManagerProviderSQL implements ISMPManagerProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPManagerProviderSQL.class);
  private static final String SML_INFO_XML = "sml-info.xml";
  private static final String SMP_SETTINGS_XML = "smp-settings.xml";
  private static final String SMP_TRANSPORT_PROFILES_XML = "transportprofiles.xml";

  private boolean m_bUseDBUserMgr = true;

  public SMPManagerProviderSQL ()
  {}

  public void beforeInitManagers ()
  {
    final SystemMigrationManager aSysMigMgr = PhotonBasicManager.getSystemMigrationMgr ();

    // Migrate if necessary
    final Runnable aMigrationAction = () -> {
      try (final WebScoped aWS = new WebScoped ())
      {
        LOGGER.info ("Migrating all DB users to ph-oton users");
        final SMPUserManagerJDBC aSQLUserMgr = new SMPUserManagerJDBC ();
        final ICommonsList <ISMPUser> aSQLUsers = aSQLUserMgr.getAllUsers ();
        LOGGER.info ("Found " + aSQLUsers.size () + " DB user to migrate");

        final ICommonsOrderedMap <String, String> aCreatedMappings = new CommonsLinkedHashMap <> ();

        final UserManager aPhotonUserMgr = PhotonSecurityManager.getUserMgr ();
        for (final ISMPUser aSQLUser : aSQLUsers)
        {
          final DBUser aDBUser = (DBUser) aSQLUser;
          IUser aPhotonUser = null;
          int nIndex = 0;
          while (true)
          {
            final String sUserName = aDBUser.getUserName () + (nIndex > 0 ? Integer.toString (nIndex) : "");
            final String sEmailAddress = sUserName + "@example.org";
            aPhotonUser = aPhotonUserMgr.createNewUser (sUserName,
                                                        sEmailAddress,
                                                        aDBUser.getPassword (),
                                                        null,
                                                        sUserName,
                                                        null,
                                                        CSMPServer.DEFAULT_LOCALE,
                                                        null,
                                                        false);
            if (aPhotonUser != null)
              break;

            // User name already taken
            ++nIndex;
            if (nIndex > 1000)
            {
              // Avoid endless loop
              throw new IllegalStateException ("Too many iterations mapping the DB user '" +
                                               aDBUser.getUserName () +
                                               "' to a ph-oton user");
            }
          }
          aCreatedMappings.put (aDBUser.getUserName (), aPhotonUser.getID ());
          LOGGER.info ("Mapped DB user '" + aDBUser.getUserName () + "' to ph-oton user " + aPhotonUser.getID ());
        }
        if (XMLMapHandler.writeMap (aCreatedMappings,
                                    new FileSystemResource (WebFileIO.getDataIO ()
                                                                     .getFile ("migrations/db-photon-user-mapping.xml")))
                         .isFailure ())
          LOGGER.error ("Failed to store mapping of DB users to ph-oton users as XML");
        LOGGER.info ("Finished migrating all DB users to ph-oton users");
      }
    };
    aSysMigMgr.performMigrationIfNecessary (CSMPServerMigrations.MIGRATION_ID_SQL_DBUSER_TO_REGULAR_USERS,
                                            aMigrationAction);

    if (aSysMigMgr.wasMigrationExecutedSuccessfully (CSMPServerMigrations.MIGRATION_ID_SQL_DBUSER_TO_REGULAR_USERS))
    {
      // Migration was already performed
      m_bUseDBUserMgr = false;
    }
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
    if (m_bUseDBUserMgr)
      return new SMPUserManagerJDBC ();

    // Use ph-oton
    return new SMPUserManagerPhoton ();
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new SMPServiceGroupManagerSQL ();
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPRedirectManagerSQL (aServiceGroupMgr);
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                                    @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPServiceInformationManagerSQL (aServiceGroupMgr);
  }

  @Nullable
  public ISMPBusinessCardManager createBusinessCardMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                        @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPBusinessCardManagerSQL (aServiceGroupMgr);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
