/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
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
