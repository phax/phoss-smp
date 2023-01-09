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

import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.phoss.smp.backend.sql.mgr.SMPSettingsManagerJDBC;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.settings.SMPSettingsManagerXML;
import com.helger.photon.app.io.WebFileIO;
import com.helger.web.scope.mgr.WebScoped;

public class V14__MigrateSettingsToDB extends BaseJavaMigration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (V5__MigrateTransportProfilesToDB.class);

  public void migrate (@Nonnull final Context context) throws Exception
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      LOGGER.info ("Migrating all settings to the DB");

      final String sFilename = "smp-settings.xml";
      final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
      if (aFile.exists ())
      {
        final SMPSettingsManagerXML aMgrXML = new SMPSettingsManagerXML (sFilename);
        final ISMPSettings aSettings = aMgrXML.getSettings ();

        final SMPSettingsManagerJDBC aMgrNew = new SMPSettingsManagerJDBC (SMPDBExecutor::new);
        if (aMgrNew.updateSettings (aSettings.isRESTWritableAPIDisabled (),
                                    aSettings.isDirectoryIntegrationEnabled (),
                                    aSettings.isDirectoryIntegrationRequired (),
                                    aSettings.isDirectoryIntegrationAutoUpdate (),
                                    aSettings.getDirectoryHostName (),
                                    aSettings.isSMLEnabled (),
                                    aSettings.isSMLRequired (),
                                    aSettings.getSMLInfoID ())
                   .isUnchanged ())
          throw new IllegalStateException ("Failed to migrate SMP settings to DB");

        // Rename to avoid later inconsistencies
        WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");

        LOGGER.info ("Finished migrating all SMP settings to the DB");
      }
      else
      {
        LOGGER.info ("No SMP settings file found");
      }
    }
  }
}
