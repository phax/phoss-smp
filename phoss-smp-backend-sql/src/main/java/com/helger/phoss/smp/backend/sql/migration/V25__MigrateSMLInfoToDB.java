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

import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.backend.sql.SMPDBExecutor;
import com.helger.phoss.smp.backend.sql.mgr.SMLInfoManagerJDBC;
import com.helger.phoss.smp.domain.sml.SMLInfoManagerXML;
import com.helger.photon.io.WebFileIO;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Migrate all SML info entries from the XML file to the DB
 *
 * @author Philip Helger
 */
public final class V25__MigrateSMLInfoToDB extends BaseJavaMigration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (V25__MigrateSMLInfoToDB.class);

  public void migrate (@NonNull final Context context) throws Exception
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      LOGGER.info ("Migrating all SML infos to the DB");

      final String sFilename = "sml-info.xml";
      final File aFile = WebFileIO.getDataIO ().getFile (sFilename);
      if (aFile.exists ())
      {
        final SMLInfoManagerXML aMgrXML = new SMLInfoManagerXML (sFilename);
        final ICommonsList <ISMLInfo> aSMLInfos = aMgrXML.getAllSMLInfos ();

        if (aSMLInfos.isNotEmpty ())
        {
          final SMLInfoManagerJDBC aMgrNew = new SMLInfoManagerJDBC (SMPDBExecutor::new,
                                                                     SMPDBExecutor.TABLE_NAME_PREFIX);
          for (final ISMLInfo aSMLInfo : aSMLInfos)
            aMgrNew.createSMLInfo (aSMLInfo.getDisplayName (),
                                   aSMLInfo.getDNSZone (),
                                   aSMLInfo.getManagementServiceURL (),
                                   aSMLInfo.getURLSuffixManageSMP (),
                                   aSMLInfo.getURLSuffixManageParticipant (),
                                   aSMLInfo.isClientCertificateRequired ());
        }

        // Rename to avoid later inconsistencies
        WebFileIO.getDataIO ().renameFile (sFilename, sFilename + ".migrated");

        LOGGER.info ("Finished migrating all " + aSMLInfos.size () + " SML infos to the DB");
      }
      else
      {
        LOGGER.info ("No SML info file found");
      }
    }
  }
}
