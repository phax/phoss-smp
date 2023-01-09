/*
 * Copyright (C) 2019-2023 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.migration;

import java.io.File;

import javax.annotation.Nonnull;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.photon.app.io.WebFileIO;
import com.helger.photon.jdbc.security.RoleManagerJDBC;
import com.helger.photon.jdbc.security.UserGroupManagerJDBC;
import com.helger.photon.jdbc.security.UserManagerJDBC;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.usergroup.IUserGroup;
import com.helger.photon.security.usergroup.UserGroup;
import com.helger.photon.security.usergroup.UserGroupManager;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Migrate all security users from the XML file to the DB
 *
 * @author Philip Helger
 * @since 5.5.0
 */
public final class V12__MigrateUserGroupsToDB extends BaseJavaMigration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (V12__MigrateUserGroupsToDB.class);

  public void migrate (@Nonnull final Context context) throws Exception
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      LOGGER.info ("Migrating all user groups to the DB");

      final String sFilename = PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                               PhotonSecurityManager.FactoryXML.FILENAME_USERGROUPS_XML;
      final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
      if (aFile.exists ())
      {
        final IUserManager aUserMgr = new UserManagerJDBC (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_CUSTOMIZER);
        final IRoleManager aRoleMgr = new RoleManagerJDBC (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_CUSTOMIZER);

        final UserGroupManager aMgrXML = new UserGroupManager (sFilename, aUserMgr, aRoleMgr);
        final ICommonsList <IUserGroup> aUserGroups = aMgrXML.getAll ();

        if (aUserGroups.isNotEmpty ())
        {
          final UserGroupManagerJDBC aMgrNew = new UserGroupManagerJDBC (SMPDBExecutor::new,
                                                                         SMPDBExecutor.TABLE_NAME_CUSTOMIZER,
                                                                         aUserMgr,
                                                                         aRoleMgr);
          for (final IUserGroup aUserGroup : aUserGroups)
          {
            // Don't run the callback here
            if (aMgrNew.internalCreateNewUserGroup ((UserGroup) aUserGroup, false, false) == null)
              LOGGER.error ("Failed to migrate user group " + aUserGroup + " to DB");
          }
        }

        // Rename to avoid later inconsistencies
        WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");

        LOGGER.info ("Finished migrating all " + aUserGroups.size () + " user groups to the DB");
      }
      else
      {
        LOGGER.warn ("No user group file found");
      }
    }
  }
}
