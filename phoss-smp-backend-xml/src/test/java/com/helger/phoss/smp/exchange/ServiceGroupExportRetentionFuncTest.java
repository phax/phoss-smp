/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.exchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.io.file.FileOperationManager;
import com.helger.io.file.SimpleFileIO;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.io.WebFileIO;

/**
 * Test class for the export file retention of {@link ServiceGroupExportJob}.
 *
 * @author Philip Helger
 */
public final class ServiceGroupExportRetentionFuncTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  private static File _createExportFile (final String sSuffix, final int nAgeDays)
  {
    WebFileIO.getDataIO ().createDirectory (ServiceGroupExportJob.EXPORT_DIRECTORY, true);
    final File aFile = new File (ServiceGroupExportJob.getExportDirectory (),
                                 ServiceGroupExportJob.EXPORT_FILENAME_PREFIX +
                                                                               sSuffix +
                                                                               ServiceGroupExportJob.EXPORT_FILENAME_EXTENSION);
    SimpleFileIO.writeFile (aFile, "<smp-data />".getBytes ());
    assertTrue (aFile.isFile ());
    assertTrue (aFile.setLastModified (System.currentTimeMillis () - TimeUnit.DAYS.toMillis (nAgeDays)));
    return aFile;
  }

  @Test
  public void testPurgeDeletesOnlyOutdatedExportFiles ()
  {
    // The default retention is 30 days
    assertEquals (30, SMPServerConfiguration.getExportRetentionDays ());

    final File aOld = _createExportFile ("old", 45);
    final File aRecent = _createExportFile ("recent", 5);

    // A file that does not follow the export naming must never be touched
    final File aForeign = new File (ServiceGroupExportJob.getExportDirectory (), "do-not-touch-me.txt");
    SimpleFileIO.writeFile (aForeign, "keep me".getBytes ());
    assertTrue (aForeign.setLastModified (System.currentTimeMillis () - TimeUnit.DAYS.toMillis (365)));

    try
    {
      assertEquals (1, ServiceGroupExportJob.purgeOldExportFiles ());

      assertFalse ("The outdated export must be deleted", aOld.isFile ());
      assertTrue ("The recent export must be kept", aRecent.isFile ());
      assertTrue ("A foreign file must never be deleted", aForeign.isFile ());

      // A second run has nothing left to do
      assertEquals (0, ServiceGroupExportJob.purgeOldExportFiles ());
    }
    finally
    {
      FileOperationManager.INSTANCE.deleteFileIfExisting (aOld);
      FileOperationManager.INSTANCE.deleteFileIfExisting (aRecent);
      FileOperationManager.INSTANCE.deleteFileIfExisting (aForeign);
    }
  }
}
