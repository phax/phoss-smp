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

import java.io.File;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ETriState;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.ICommonsList;
import com.helger.dao.DAOException;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.backend.mongodb.PhotonBasicManagerFactoryMongoDB;
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
import com.helger.photon.io.WebFileIO;
import com.helger.photon.mgrs.PhotonBasicManager;
import com.helger.photon.mgrs.longrun.ILongRunningJobResultManager;
import com.helger.photon.mgrs.longrun.LongRunningJobData;
import com.helger.photon.mgrs.longrun.LongRunningJobResultManager;
import com.helger.photon.mgrs.sysmigration.ISystemMigrationManager;
import com.helger.photon.mgrs.sysmigration.SystemMigrationManager;
import com.helger.photon.mgrs.sysmigration.SystemMigrationResult;
import com.helger.photon.mgrs.sysmsg.SystemMessageManager;
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

  /**
   * Bootstrap the MongoDB system migration history from the XML file. This must run before
   * {@link #_performMigrations()} so that {@code performMigrationIfNecessary} can correctly detect
   * already-completed migrations and skip them. Only imports if the MongoDB collection is empty
   * (i.e. first switch from XML to MongoDB backend).
   */
  private void _migrateSystemMigrationsFromXML ()
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      final ISystemMigrationManager aMgr = PhotonBasicManager.getSystemMigrationMgr ();

      // Only import if the collection is empty to avoid duplicates
      if (aMgr.getAllMigrationIDs ().isNotEmpty ())
        return;

      final String sFilename = PhotonBasicManager.FactoryXML.SYSTEM_MIGRATIONS_XML;
      final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
      if (!aFile.exists ())
        return;

      LOGGER.info ("Migrating all system migration results from XML to MongoDB");
      try
      {
        final SystemMigrationManager aMgrXML = new SystemMigrationManager (sFilename);
        final ICommonsList <SystemMigrationResult> aResults = aMgrXML.getAllMigrationResultsFlattened ();
        for (final SystemMigrationResult aResult : aResults)
          aMgr.addMigrationResult (aResult);

        WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");
        LOGGER.info ("Finished migrating all " + aResults.size () + " system migration results to MongoDB");
      }
      catch (final DAOException ex)
      {
        throw new IllegalStateException ("Failed to migrate system migrations from XML", ex);
      }
    }
  }

  private void _performMigrations ()
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      // See issue #358, #359, #360 and #361
      final ISystemMigrationManager aSysMigMgr = PhotonBasicManager.getSystemMigrationMgr ();

      aSysMigMgr.performMigrationIfNecessary ("mongodb-security-migrate-roles-from-xml", () -> {
        LOGGER.info ("Migrating all roles from XML to MongoDB");
        try
        {
          final String sFilename = PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                                   PhotonSecurityManager.FactoryXML.FILENAME_ROLES_XML;
          final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
          if (aFile.exists ())
          {
            final RoleManager aRoleMgrXML = new RoleManager (sFilename);
            final ICommonsList <Role> aRoles = aRoleMgrXML.getAll ().getAllMapped (Role.class::cast);
            if (aRoles.isNotEmpty ())
            {
              final RoleManagerMongoDB aRoleMgrMongo = (RoleManagerMongoDB) PhotonSecurityManager.getRoleMgr ();

              // Make sure we have an empty DB
              aRoleMgrMongo.getCollection ().drop ();

              // Migrate all roles
              for (final Role aRole : aRoles)
                aRoleMgrMongo.internalCreateMigrationRole (aRole);

              // Rename to avoid later inconsistencies
              WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");
            }

            LOGGER.info ("Finished migrating all " + aRoles.size () + " roles to the DB");
          }
          else
          {
            LOGGER.info ("No roles file found");
          }
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
          final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
          if (aFile.exists ())
          {
            final UserManager aUserMgrXML = new UserManager (sFilename);
            final ICommonsList <User> aUsers = aUserMgrXML.getAll ().getAllMapped (User.class::cast);
            if (aUsers.isNotEmpty ())
            {
              final UserManagerMongoDB aUserMgrMongo = (UserManagerMongoDB) PhotonSecurityManager.getUserMgr ();

              // Make sure we have an empty DB
              aUserMgrMongo.getCollection ().drop ();

              // Migrate all users
              for (final User aUser : aUsers)
                aUserMgrMongo.internalCreateMigrationUser (aUser);

              // Rename to avoid later inconsistencies
              WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");
            }
            LOGGER.info ("Finished migrating all " + aUsers.size () + " users to the DB");
          }
          else
          {
            LOGGER.info ("No users file found");
          }
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
          final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
          if (aFile.exists ())
          {
            final UserGroupManager aUserGroupMgrXML = new UserGroupManager (sFilename,
                                                                            PhotonSecurityManager.getUserMgr (),
                                                                            PhotonSecurityManager.getRoleMgr ());
            final ICommonsList <UserGroup> aUserGroups = aUserGroupMgrXML.getAll ()
                                                                         .getAllMapped (UserGroup.class::cast);
            if (aUserGroups.isNotEmpty ())
            {
              final UserGroupManagerMongoDB aUserGroupMgrMongo = (UserGroupManagerMongoDB) PhotonSecurityManager.getUserGroupMgr ();

              // Make sure we have an empty DB
              aUserGroupMgrMongo.getCollection ().drop ();

              // Migrate all user groups
              for (final UserGroup aUserGroup : aUserGroups)
                aUserGroupMgrMongo.internalCreateMigrationUserGroup (aUserGroup);

              // Rename to avoid later inconsistencies
              WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");
            }
            LOGGER.info ("Finished migrating all " + aUserGroups.size () + " user groups to the DB");
          }
          else
          {
            LOGGER.info ("No user groups file found");
          }
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
          final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
          if (aFile.exists ())
          {
            final UserTokenManager aUserTokenMgrXML = new UserTokenManager (sFilename);
            final ICommonsList <UserToken> aUserTokens = aUserTokenMgrXML.getAll ()
                                                                         .getAllMapped (UserToken.class::cast);
            if (aUserTokens.isNotEmpty ())
            {
              final UserTokenManagerMongoDB aUserTokenMgrMongo = (UserTokenManagerMongoDB) PhotonSecurityManager.getUserTokenMgr ();

              // Make sure we have an empty DB
              aUserTokenMgrMongo.getCollection ().drop ();

              // Migrate all users
              for (final UserToken aUserToken : aUserTokens)
                aUserTokenMgrMongo.internalCreateMigrationUserToken (aUserToken);

              // Rename to avoid later inconsistencies
              WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");
            }
            LOGGER.info ("Finished migrating all " + aUserTokens.size () + " user tokens to the DB");
          }
          else
          {
            LOGGER.info ("No user token file found");
          }
        }
        catch (final DAOException ex)
        {
          throw new IllegalStateException ("Failed to init XML mgrs", ex);
        }
      });

      aSysMigMgr.performMigrationIfNecessary ("mongodb-basic-migrate-sysmessage-from-xml", () -> {
        LOGGER.info ("Migrating system message to MongoDB");
        try
        {
          final String sFilename = PhotonBasicManager.FactoryXML.SYSTEM_MESSAGE_XML;
          final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
          if (aFile.exists ())
          {
            final SystemMessageManager aMgrXML = new SystemMessageManager (sFilename);
            if (aMgrXML.hasSystemMessage ())
            {
              PhotonBasicManager.getSystemMessageMgr ()
                                .setSystemMessage (aMgrXML.getMessageType (), aMgrXML.getSystemMessage ());
            }
            WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");
            LOGGER.info ("Finished migrating system message to MongoDB");
          }
          else
          {
            LOGGER.info ("No system message file found");
          }
        }
        catch (final DAOException ex)
        {
          throw new IllegalStateException ("Failed to init XML mgrs", ex);
        }
      });

      aSysMigMgr.performMigrationIfNecessary ("mongodb-basic-migrate-longruningjobs-from-xml", () -> {
        LOGGER.info ("Migrating all long running job results to MongoDB");
        try
        {
          final String sFilename = PhotonBasicManager.FactoryXML.LONG_RUNNING_JOB_RESULTS_XML;
          final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
          if (aFile.exists ())
          {
            final LongRunningJobResultManager aMgrXML = new LongRunningJobResultManager (sFilename);
            final ICommonsList <LongRunningJobData> aResults = aMgrXML.getAllJobResults ();
            if (aResults.isNotEmpty ())
            {
              final ILongRunningJobResultManager aMgrMongo = PhotonBasicManager.getLongRunningJobResultMgr ();
              for (final LongRunningJobData aResult : aResults)
                aMgrMongo.addResult (aResult);
            }
            WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");
            LOGGER.info ("Finished migrating all " + aResults.size () + " long running job results to MongoDB");
          }
          else
          {
            LOGGER.info ("No long running job results file found");
          }
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
    // Set the basic manager factory to use MongoDB for all three managers
    // Must be before PhotonSecurityManager is instantiated
    PhotonBasicManagerFactoryMongoDB.install ();

    // Set the special PhotonSecurityManager factory
    PhotonSecurityManagerFactoryMongoDB.install ();

    // Bootstrap migration history from XML before any migration checks run
    _migrateSystemMigrationsFromXML ();

    // Migrate the rest
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
