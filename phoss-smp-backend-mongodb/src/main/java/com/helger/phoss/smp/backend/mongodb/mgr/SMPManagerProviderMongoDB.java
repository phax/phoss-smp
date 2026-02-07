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
package com.helger.phoss.smp.backend.mongodb.mgr;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ETriState;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.ICommonsList;
import com.helger.dao.DAOException;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.backend.mongodb.PhotonSecurityManagerFactoryMongoDB;
import com.helger.phoss.smp.backend.mongodb.security.RoleManagerMongoDB;
import com.helger.phoss.smp.backend.mongodb.security.UserGroupManagerMongoDB;
import com.helger.phoss.smp.backend.mongodb.security.UserManagerMongoDB;
import com.helger.phoss.smp.backend.mongodb.security.UserTokenManagerMongoDB;
import com.helger.phoss.smp.domain.ISMPManagerProvider;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.photon.core.mgr.PhotonBasicManager;
import com.helger.photon.core.sysmigration.SystemMigrationManager;
import com.helger.photon.io.WebFileIO;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.role.Role;
import com.helger.photon.security.role.RoleManager;
import com.helger.photon.security.token.user.UserToken;
import com.helger.photon.security.token.user.UserTokenManager;
import com.helger.photon.security.user.User;
import com.helger.photon.security.user.UserManager;
import com.helger.photon.security.usergroup.UserGroup;
import com.helger.photon.security.usergroup.UserGroupManager;
import com.helger.web.scope.mgr.WebScoped;

/**
 * {@link ISMPManagerProvider} implementation for this backend.
 *
 * @author Philip Helger
 */
public final class SMPManagerProviderMongoDB implements ISMPManagerProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPManagerProviderMongoDB.class);

  private void _performMigrations ()
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      // See issue #358, #359, #360 and #361
      final SystemMigrationManager aSysMigMgr = PhotonBasicManager.getSystemMigrationMgr ();

      aSysMigMgr.performMigrationIfNecessary ("mongodb-security-migrate-roles-from-xml", () -> {
        LOGGER.info ("Migrating all roles from XML to MongoDB");
        try
        {
          final String sFilename = PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                                   PhotonSecurityManager.FactoryXML.FILENAME_ROLES_XML;
          final RoleManager aRoleMgrXML = new RoleManager (sFilename);
          final ICommonsList <Role> aRoles = aRoleMgrXML.getAll ().getAllMapped (Role.class::cast);
          if (aRoles.isNotEmpty ())
          {
            final RoleManagerMongoDB aRoleMgr = (RoleManagerMongoDB) PhotonSecurityManager.getRoleMgr ();

            // Make sure we have an empty DB
            aRoleMgr.getCollection ().drop ();

            // Migrate all roles
            for (final Role aRole : aRoles)
              aRoleMgr.internalCreateMigrationRole (aRole);

            // Rename to avoid later inconsistencies
            WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");
          }

          LOGGER.info ("Finished migrating all " + aRoles.size () + " roles to the DB");
        }
        catch (final DAOException ex)
        {
          throw new IllegalStateException ("Failed to init XML mgrs", ex);
        }
      });

      aSysMigMgr.performMigrationIfNecessary ("mongodb-security-migrate-users-from-xml", () -> {
        LOGGER.info ("Migrating all users from XML to MongoDB");
        try
        {
          final String sFilename = PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                                   PhotonSecurityManager.FactoryXML.FILENAME_USERS_XML;
          final UserManager aUserMgrXML = new UserManager (sFilename);
          final ICommonsList <User> aUsers = aUserMgrXML.getAll ().getAllMapped (User.class::cast);
          if (aUsers.isNotEmpty ())
          {
            final UserManagerMongoDB aUserMgr = (UserManagerMongoDB) PhotonSecurityManager.getUserMgr ();

            // Make sure we have an empty DB
            aUserMgr.getCollection ().drop ();

            // Migrate all users
            for (final User aUser : aUsers)
              aUserMgr.internalCreateMigrationUser (aUser);

            // Rename to avoid later inconsistencies
            WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");
          }
          LOGGER.info ("Finished migrating all " + aUsers.size () + " users to the DB");
        }
        catch (final DAOException ex)
        {
          throw new IllegalStateException ("Failed to init XML mgrs", ex);
        }
      });

      aSysMigMgr.performMigrationIfNecessary ("mongodb-security-migrate-usergroups-from-xml", () -> {
        if (!aSysMigMgr.wasMigrationExecutedSuccessfully ("mongodb-security-migrate-roles-from-xml"))
          throw new IllegalStateException ("User groups can only be migrated, after roles were migrated");
        if (!aSysMigMgr.wasMigrationExecutedSuccessfully ("mongodb-security-migrate-users-from-xml"))
          throw new IllegalStateException ("User groups can only be migrated, after users were migrated");

        LOGGER.info ("Migrating all user groups from XML to MongoDB");
        try
        {
          final String sFilename = PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                                   PhotonSecurityManager.FactoryXML.FILENAME_USERGROUPS_XML;
          final UserGroupManager aUserGroupMgrXML = new UserGroupManager (sFilename,
                                                                          PhotonSecurityManager.getUserMgr (),
                                                                          PhotonSecurityManager.getRoleMgr ());
          final ICommonsList <UserGroup> aUserGroups = aUserGroupMgrXML.getAll ().getAllMapped (UserGroup.class::cast);
          if (aUserGroups.isNotEmpty ())
          {
            final UserGroupManagerMongoDB aUserGroupMgr = (UserGroupManagerMongoDB) PhotonSecurityManager.getUserGroupMgr ();

            // Make sure we have an empty DB
            aUserGroupMgr.getCollection ().drop ();

            // Migrate all user groups
            for (final UserGroup aUserGroup : aUserGroups)
              aUserGroupMgr.internalCreateMigrationUserGroup (aUserGroup);

            // Rename to avoid later inconsistencies
            WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");
          }
          LOGGER.info ("Finished migrating all " + aUserGroups.size () + " user groups to the DB");
        }
        catch (final DAOException ex)
        {
          throw new IllegalStateException ("Failed to init XML mgrs", ex);
        }
      });

      aSysMigMgr.performMigrationIfNecessary ("mongodb-security-migrate-usertokens-from-xml", () -> {
        LOGGER.info ("Migrating all user tokens from XML to MongoDB");
        try
        {
          final String sFilename = PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                                   PhotonSecurityManager.FactoryXML.FILENAME_USERTOKENS_XML;
          final UserTokenManager aUserTokenMgrXML = new UserTokenManager (sFilename);
          final ICommonsList <UserToken> aUserTokens = aUserTokenMgrXML.getAll ().getAllMapped (UserToken.class::cast);
          if (aUserTokens.isNotEmpty ())
          {
            final UserTokenManagerMongoDB aUserTokenMgr = (UserTokenManagerMongoDB) PhotonSecurityManager.getUserTokenMgr ();

            // Make sure we have an empty DB
            aUserTokenMgr.getCollection ().drop ();

            // Migrate all users
            for (final UserToken aUserToken : aUserTokens)
              aUserTokenMgr.internalCreateMigrationUserToken (aUserToken);

            // Rename to avoid later inconsistencies
            WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");
          }
          LOGGER.info ("Finished migrating all " + aUserTokens.size () + " user tokens to the DB");
        }
        catch (final DAOException ex)
        {
          throw new IllegalStateException ("Failed to init XML mgrs", ex);
        }
      });
    }
  }

  @Override
  public void beforeInitManagers ()
  {
    // Set the special PhotonSecurityManager factory
    PhotonSecurityManager.setFactory (new PhotonSecurityManagerFactoryMongoDB ());
    PhotonSecurityManager.getInstance ();

    _performMigrations ();
  }

  @NonNull
  public ETriState getBackendConnectionEstablishedDefaultState ()
  {
    return ETriState.UNDEFINED;
  }

  @NonNull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    return new SMLInfoManagerMongoDB ();
  }

  @NonNull
  public ISMPSettingsManager createSettingsMgr ()
  {
    return new SMPSettingsManagerMongoDB ();
  }

  @NonNull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    return new SMPTransportProfileManagerMongoDB ();
  }

  @NonNull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new SMPServiceGroupManagerMongoDB ();
  }

  @NonNull
  public ISMPRedirectManager createRedirectMgr (@NonNull final IIdentifierFactory aIdentifierFactory)
  {
    return new SMPRedirectManagerMongoDB (aIdentifierFactory);
  }

  @NonNull
  public ISMPServiceInformationManager createServiceInformationMgr (@NonNull final IIdentifierFactory aIdentifierFactory)
  {
    return new SMPServiceInformationManagerMongoDB (aIdentifierFactory);
  }

  @NonNull
  public ISMPParticipantMigrationManager createParticipantMigrationMgr ()
  {
    return new SMPParticipantMigrationManagerMongoDB ();
  }

  @Nullable
  public ISMPBusinessCardManager createBusinessCardMgr (@NonNull final IIdentifierFactory aIdentifierFactory,
                                                        @NonNull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPBusinessCardManagerMongoDB (aIdentifierFactory);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
