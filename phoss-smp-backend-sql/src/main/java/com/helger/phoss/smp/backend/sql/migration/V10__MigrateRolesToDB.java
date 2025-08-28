/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
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

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.photon.io.WebFileIO;
import com.helger.photon.jdbc.security.RoleManagerJDBC;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.role.IRole;
import com.helger.photon.security.role.Role;
import com.helger.photon.security.role.RoleManager;
import com.helger.web.scope.mgr.WebScoped;

import jakarta.annotation.Nonnull;

/**
 * Migrate all security roles from the XML file to the DB
 *
 * @author Philip Helger
 * @since 5.5.0
 */
public final class V10__MigrateRolesToDB extends BaseJavaMigration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (V10__MigrateRolesToDB.class);

  public void migrate (@Nonnull final Context context) throws Exception
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      LOGGER.info ("Migrating all roles to the DB");

      final String sFilename = PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                               PhotonSecurityManager.FactoryXML.FILENAME_ROLES_XML;
      final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
      if (aFile.exists ())
      {
        final RoleManager aMgrXML = new RoleManager (sFilename);
        final ICommonsList <IRole> aRoles = aMgrXML.getAll ();
        if (aRoles.isNotEmpty ())
        {
          final RoleManagerJDBC aMgrNew = new RoleManagerJDBC (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_CUSTOMIZER);
          for (final IRole aRole : aRoles)
          {
            // Don't run the callback here
            if (aMgrNew.internalCreateNewRole ((Role) aRole, false, false) == null)
              LOGGER.error ("Failed to migrate role " + aRole + " to DB");
          }
        }

        // Rename to avoid later inconsistencies
        WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");

        LOGGER.info ("Finished migrating all " + aRoles.size () + " roles to the DB");
      }
      else
      {
        LOGGER.warn ("No role XML file found");
      }
    }
  }
}
