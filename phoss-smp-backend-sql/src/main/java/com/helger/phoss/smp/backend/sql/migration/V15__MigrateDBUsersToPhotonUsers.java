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

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.io.resource.FileSystemResource;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.phoss.smp.backend.sql.SMPDataSourceSingleton;
import com.helger.phoss.smp.backend.sql.domain.DBUser;
import com.helger.photon.io.WebFileIO;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xml.microdom.util.XMLMapHandler;

/**
 * This was introduced in V5.5.2 to work around an issue with plain DBs because in V2, the
 * "smp_secuser" table does not exist yet. Unfortunately this table would be required when creating
 * a new user with "PhotonSecurityManager.getUserMgr ()"
 *
 * @author Philip Helger
 */
public final class V15__MigrateDBUsersToPhotonUsers extends BaseJavaMigration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (V15__MigrateDBUsersToPhotonUsers.class);

  public void migrate (@NonNull final Context context) throws Exception
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      LOGGER.info ("Migrating all old DB users to ph-oton users");
      final EDatabaseSystemType eDBType = SMPDataSourceSingleton.getDatabaseType ();

      // Old JDBC user manager
      final SMPUserManagerJDBC aSQLUserMgr = new SMPUserManagerJDBC (SMPDBExecutor::new,
                                                                     SMPDBExecutor.TABLE_NAME_PREFIX);
      final ICommonsList <DBUser> aSQLUsers = aSQLUserMgr.getAllUsers ();
      LOGGER.info ("Found " + aSQLUsers.size () + " DB user to migrate");

      final ICommonsOrderedMap <String, String> aCreatedMappings = new CommonsLinkedHashMap <> ();

      // New JDBC user manager
      final IUserManager aPhotonUserMgr = PhotonSecurityManager.getUserMgr ();
      for (final DBUser aSQLUser : aSQLUsers)
      {
        final DBUser aDBUser = aSQLUser;
        IUser aPhotonUser = null;
        int nIndex = 0;
        while (true)
        {
          final String sUserName = aDBUser.getUserName () + (nIndex > 0 ? Integer.toString (nIndex) : "");
          // The suffix "@example.org" is added to make it an email-address
          final String sEmailAddress = sUserName + "@example.org";
          aPhotonUser = aPhotonUserMgr.createNewUser (sEmailAddress,
                                                      sEmailAddress,
                                                      aDBUser.getPassword (),
                                                      null,
                                                      sUserName,
                                                      null,
                                                      CSMPServer.DEFAULT_LOCALE,
                                                      null,
                                                      false);
          if (aPhotonUser != null)
          {
            // New user was successfully created
            break;
          }

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

      // Update the ownership in "smp_ownership"
      // Remove the table "smp_user"
      aSQLUserMgr.updateOwnershipsAndKillUsers (aCreatedMappings);

      if (XMLMapHandler.writeMap (aCreatedMappings,
                                  new FileSystemResource (WebFileIO.getDataIO ()
                                                                   .getFile ("migrations/db-photon-user-mapping-" +
                                                                             eDBType.getID () +
                                                                             ".xml"))).isFailure ())
        LOGGER.error ("Failed to store mapping of DB users to ph-oton users as XML");
      LOGGER.info ("Finished migrating all DB users to ph-oton users");
    }
  }
}
