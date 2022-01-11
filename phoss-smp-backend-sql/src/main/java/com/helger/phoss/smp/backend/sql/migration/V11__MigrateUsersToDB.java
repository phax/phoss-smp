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
import com.helger.photon.jdbc.security.UserManagerJDBC;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.User;
import com.helger.photon.security.user.UserManager;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Migrate all security users from the XML file to the DB
 *
 * @author Philip Helger
 * @since 5.5.0
 */
public final class V11__MigrateUsersToDB extends BaseJavaMigration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (V11__MigrateUsersToDB.class);

  public void migrate (@Nonnull final Context context) throws Exception
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      LOGGER.info ("Migrating all users to the DB");

      final String sFilename = PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY + PhotonSecurityManager.FactoryXML.FILENAME_USERS_XML;
      final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
      if (aFile.exists ())
      {
        // Read the XML file
        final UserManager aMgrXML = new UserManager (sFilename);
        final ICommonsList <IUser> aUsers = aMgrXML.getAll ();

        if (aUsers.isNotEmpty ())
        {
          // Create a new JDBC manager
          final UserManagerJDBC aMgrNew = new UserManagerJDBC (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_CUSTOMIZER);
          for (final IUser aUser : aUsers)
          {
            // Don't run the callback here
            if (aMgrNew.internalCreateNewUser ((User) aUser, false, false) == null)
              LOGGER.error ("Failed to migrate user " + aUser + " to DB");
          }
        }

        // Rename to avoid later inconsistencies
        WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");

        LOGGER.info ("Finished migrating all " + aUsers.size () + " users to the DB");
      }
      else
      {
        LOGGER.warn ("No user file found");
      }
    }
  }
}
