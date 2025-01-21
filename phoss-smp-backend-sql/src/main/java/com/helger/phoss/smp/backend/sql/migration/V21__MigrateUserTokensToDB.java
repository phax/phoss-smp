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

import javax.annotation.Nonnull;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.photon.io.WebFileIO;
import com.helger.photon.jdbc.security.UserManagerJDBC;
import com.helger.photon.jdbc.security.UserTokenManagerJDBC;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.token.user.IUserToken;
import com.helger.photon.security.token.user.UserToken;
import com.helger.photon.security.token.user.UserTokenManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Migrate all security user tokens from the XML file to the DB
 *
 * @author Philip Helger
 * @since 6.0.7
 */
public final class V21__MigrateUserTokensToDB extends BaseJavaMigration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (V21__MigrateUserTokensToDB.class);

  public void migrate (@Nonnull final Context context) throws Exception
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      LOGGER.info ("Migrating all roles to the DB");

      final String sFilename = PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                               PhotonSecurityManager.FactoryXML.FILENAME_USERTOKENS_XML;
      final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
      if (aFile.exists ())
      {
        final UserTokenManager aMgrXML = new UserTokenManager (sFilename);
        final ICommonsList <IUserToken> aUserTokens = aMgrXML.getAll ();
        if (aUserTokens.isNotEmpty ())
        {
          final IUserManager aUserMgr = new UserManagerJDBC (SMPDBExecutor::new, SMPDBExecutor.TABLE_NAME_CUSTOMIZER);
          final UserTokenManagerJDBC aMgrNew = new UserTokenManagerJDBC (SMPDBExecutor::new,
                                                                         SMPDBExecutor.TABLE_NAME_CUSTOMIZER,
                                                                         aUserMgr);
          for (final IUserToken aUserToken : aUserTokens)
          {
            // Don't run the callback here
            if (aMgrNew.internalCreateUserToken ((UserToken) aUserToken, false) == null)
              LOGGER.error ("Failed to migrate user token " + aUserToken + " to DB");
          }
        }

        // Rename to avoid later inconsistencies
        WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");

        LOGGER.info ("Finished migrating all " + aUserTokens.size () + " user tokens to the DB");
      }
      else
      {
        LOGGER.warn ("No user token XML file found");
      }
    }
  }
}
