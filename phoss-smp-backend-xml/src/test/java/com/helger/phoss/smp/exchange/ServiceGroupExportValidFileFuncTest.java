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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.io.file.FileOperationManager;
import com.helger.io.file.SimpleFileIO;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.io.WebFileIO;

/**
 * Test class for {@link ServiceGroupExportJob#getValidExportFile(File)}. This method is the single
 * gate deciding which files may be downloaded, so it is tested explicitly.
 *
 * @author Philip Helger
 */
public final class ServiceGroupExportValidFileFuncTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testOnlyRealExportFilesAreAccepted ()
  {
    WebFileIO.getDataIO ().createDirectory (ServiceGroupExportJob.EXPORT_DIRECTORY, true);
    final File aExportDir = ServiceGroupExportJob.getExportDirectory ();

    final File aExport = new File (aExportDir,
                                   ServiceGroupExportJob.EXPORT_FILENAME_PREFIX +
                                             "20260828120000" +
                                             ServiceGroupExportJob.EXPORT_FILENAME_EXTENSION);
    final File aForeign = new File (aExportDir, "something-else.xml");
    final File aOutside = WebFileIO.getDataIO ().getFile ("outside-the-export-dir.xml");
    SimpleFileIO.writeFile (aExport, "<smp-data />".getBytes ());
    SimpleFileIO.writeFile (aForeign, "<smp-data />".getBytes ());
    SimpleFileIO.writeFile (aOutside, "<smp-data />".getBytes ());

    try
    {
      // The real thing
      final File aResolved = ServiceGroupExportJob.getValidExportFile (aExport);
      assertNotNull ("A real export file must be accepted", aResolved);
      assertEquals (aExport.getName (), aResolved.getName ());

      // null in - null out
      assertNull (ServiceGroupExportJob.getValidExportFile (null));

      // Right directory, but not an export file name
      assertNull ("A foreign file in the export directory must be rejected",
                  ServiceGroupExportJob.getValidExportFile (aForeign));

      // Correct name, but outside the export directory
      assertNull ("A file outside the export directory must be rejected",
                  ServiceGroupExportJob.getValidExportFile (aOutside));

      // Correct name and directory, but does not exist
      assertNull ("A non-existing file must be rejected",
                  ServiceGroupExportJob.getValidExportFile (new File (aExportDir,
                                                                      ServiceGroupExportJob.EXPORT_FILENAME_PREFIX +
                                                                                 "does-not-exist" +
                                                                                 ServiceGroupExportJob.EXPORT_FILENAME_EXTENSION)));

      // Path traversal out of the export directory must not work
      assertNull ("Path traversal must be rejected",
                  ServiceGroupExportJob.getValidExportFile (new File (aExportDir,
                                                                      ".." + File.separator +
                                                                                 aOutside.getName ())));

      // Path traversal that ends up in the export directory again is fine, because the canonical
      // file is the real export file
      final File aTraversedBack = new File (aExportDir, ".." + File.separator +
                                                        aExportDir.getName () +
                                                        File.separator +
                                                        aExport.getName ());
      final File aTraversedBackResolved = ServiceGroupExportJob.getValidExportFile (aTraversedBack);
      assertNotNull (aTraversedBackResolved);
      assertEquals (aExport.getName (), aTraversedBackResolved.getName ());

      // A directory is not a file
      assertTrue (aExportDir.isDirectory ());
      assertNull ("A directory must be rejected", ServiceGroupExportJob.getValidExportFile (aExportDir));
    }
    finally
    {
      FileOperationManager.INSTANCE.deleteFileIfExisting (aExport);
      FileOperationManager.INSTANCE.deleteFileIfExisting (aForeign);
      FileOperationManager.INSTANCE.deleteFileIfExisting (aOutside);
    }
  }
}
