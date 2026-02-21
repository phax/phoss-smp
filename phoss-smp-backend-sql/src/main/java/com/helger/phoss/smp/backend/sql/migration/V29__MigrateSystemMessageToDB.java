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
package com.helger.phoss.smp.backend.sql.migration;

import java.io.File;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.photon.io.WebFileIO;
import com.helger.photon.jdbc.basic.SystemMessageManagerJDBC;
import com.helger.photon.mgrs.PhotonBasicManager;
import com.helger.photon.mgrs.sysmsg.SystemMessageManager;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Migrate the system message from the XML file to the DB
 *
 * @author Philip Helger
 * @since 8.0.16
 */
public final class V29__MigrateSystemMessageToDB extends BaseJavaMigration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (V29__MigrateSystemMessageToDB.class);

  public void migrate (@NonNull final Context context) throws Exception
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      LOGGER.info ("Migrating system message to the DB");

      final String sFilename = PhotonBasicManager.FactoryXML.SYSTEM_MESSAGE_XML;
      final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
      if (aFile.exists ())
      {
        final SystemMessageManager aMgrXML = new SystemMessageManager (sFilename);

        if (aMgrXML.hasSystemMessage ())
        {
          final SystemMessageManagerJDBC aMgrNew = new SystemMessageManagerJDBC (SMPDBExecutor::new,
                                                                                 SMPDBExecutor.TABLE_NAME_CUSTOMIZER);
          aMgrNew.setSystemMessage (aMgrXML.getMessageType (), aMgrXML.getSystemMessage ());
        }

        // Rename to avoid later inconsistencies
        WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");

        LOGGER.info ("Finished migrating system message to the DB");
      }
      else
      {
        LOGGER.info ("No system message file found");
      }
    }
  }
}
