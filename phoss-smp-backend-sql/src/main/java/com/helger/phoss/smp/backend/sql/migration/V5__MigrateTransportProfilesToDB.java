/**
 * Copyright (C) 2019-2021 Philip Helger and contributors
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
import com.helger.dao.DAOException;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.phoss.smp.backend.sql.mgr.SMPTransportProfileManagerJDBC;
import com.helger.phoss.smp.domain.transportprofile.SMPTransportProfileManagerXML;
import com.helger.photon.app.io.WebFileIO;
import com.helger.web.scope.mgr.WebScoped;

public final class V5__MigrateTransportProfilesToDB extends BaseJavaMigration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (V5__MigrateTransportProfilesToDB.class);

  public void migrate (@Nonnull final Context context)
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      LOGGER.info ("Migrating all transport profiles to the DB");

      final String sFilename = "transportprofiles.xml";
      final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
      if (aFile.exists ())
      {
        final SMPTransportProfileManagerXML aMgrXML = new SMPTransportProfileManagerXML (sFilename);
        final ICommonsList <ISMPTransportProfile> aTransportProfiles = aMgrXML.getAll ();

        if (aTransportProfiles.isNotEmpty ())
        {
          final SMPTransportProfileManagerJDBC aMgrNew = new SMPTransportProfileManagerJDBC (SMPDBExecutor::new);
          for (final ISMPTransportProfile aTransportProfile : aTransportProfiles)
            if (aMgrNew.createSMPTransportProfile (aTransportProfile.getID (),
                                                   aTransportProfile.getName (),
                                                   aTransportProfile.isDeprecated ()) == null)
              LOGGER.error ("Failed to migrate " + aTransportProfile + " to DB");
        }

        // Rename to avoid later inconsistencies
        WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");

        LOGGER.info ("Finished migrating all " + aTransportProfiles.size () + " transport profiles to the DB");
      }
      else
      {
        LOGGER.info ("No transport profile file found");
      }
    }
    catch (final DAOException ex)
    {
      LOGGER.warn ("Error reading the XML transport profile", ex);
    }
  }
}