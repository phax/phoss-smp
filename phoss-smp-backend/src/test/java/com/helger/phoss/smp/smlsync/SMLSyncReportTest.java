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
package com.helger.phoss.smp.smlsync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.helger.base.io.stream.StreamHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.datetime.helper.PDTFactory;

/**
 * Test class for class {@link SMLSyncReport}.
 *
 * @author Philip Helger
 */
public final class SMLSyncReportTest
{
  @Rule
  public final TemporaryFolder m_aTempFolder = new TemporaryFolder ();

  private static final String PI1 = "iso6523-actorid-upis::0088:1";
  private static final String PI2 = "iso6523-actorid-upis::0088:2";
  private static final String PI3 = "iso6523-actorid-upis::0088:3";

  private static String _readEntry (final File aZipFile, final String sEntryName) throws IOException
  {
    try (final ZipFile aZip = new ZipFile (aZipFile))
    {
      final ZipEntry aEntry = aZip.getEntry (sEntryName);
      if (aEntry == null)
        return null;
      return StreamHelper.getAllBytesAsString (aZip.getInputStream (aEntry), StandardCharsets.UTF_8);
    }
  }

  private static SMLSyncResult _createResult (final int nLocal, final int nMissing, final int nOrphans)
  {
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();
    return new SMLSyncResult (aNow, aNow.plusSeconds (5), "peppoltest", "HELGER-SMP", 1, 2, nLocal, nMissing, nOrphans);
  }

  @Test
  public void testAllThreeEntriesArePresent () throws IOException
  {
    final File aZip = new File (m_aTempFolder.getRoot (), "report.zip");
    final File aOrphans = m_aTempFolder.newFile ("orphans.tmp");
    Files.writeString (aOrphans.toPath (), PI3 + "\n");

    final ICommonsSet <String> aMissing = new CommonsTreeSet <> (PI1, PI2);
    SMLSyncReport.writeReport (aZip, _createResult (2, 2, 1), aOrphans, aMissing, aOrphans);

    assertTrue (aZip.isFile ());
    assertNotNull (_readEntry (aZip, CSMLSync.ENTRY_SUMMARY));
    assertEquals (PI1 + "\n" + PI2 + "\n", _readEntry (aZip, CSMLSync.ENTRY_MISSING_IN_SML));
    assertEquals (PI3 + "\n", _readEntry (aZip, CSMLSync.ENTRY_ORPHANS_IN_SML));
  }

  @Test
  public void testSummaryContainsTheCounts () throws IOException
  {
    final File aZip = new File (m_aTempFolder.getRoot (), "report.zip");
    final File aOrphans = new File (m_aTempFolder.getRoot (), "does-not-exist.tmp");

    SMLSyncReport.writeReport (aZip, _createResult (7, 3, 0), aOrphans, new CommonsArrayList <> (PI1), aOrphans);

    final String sSummary = _readEntry (aZip, CSMLSync.ENTRY_SUMMARY);
    assertTrue (sSummary, sSummary.contains ("local-participants=\"7\""));
    assertTrue (sSummary, sSummary.contains ("missing-in-sml=\"3\""));
    assertTrue (sSummary, sSummary.contains ("orphans-in-sml=\"0\""));
    assertTrue (sSummary, sSummary.contains ("HELGER-SMP"));
  }

  @Test
  public void testAbsentOrphanFileYieldsAnEmptyEntry () throws IOException
  {
    // The job does not create the temporary file if there is not a single orphan
    final File aZip = new File (m_aTempFolder.getRoot (), "report.zip");
    final File aOrphans = new File (m_aTempFolder.getRoot (), "does-not-exist.tmp");
    assertFalse (aOrphans.exists ());

    SMLSyncReport.writeReport (aZip, _createResult (1, 0, 0), aOrphans, new CommonsArrayList <> (), aOrphans);

    assertEquals ("", _readEntry (aZip, CSMLSync.ENTRY_ORPHANS_IN_SML));
    assertEquals ("", _readEntry (aZip, CSMLSync.ENTRY_MISSING_IN_SML));
  }

  @Test
  public void testAllMissingAddsTheUnregisterNote () throws IOException
  {
    // Every local participant missing is the signature of an unregister plus re-register
    final File aZip = new File (m_aTempFolder.getRoot (), "report.zip");
    final File aOrphans = new File (m_aTempFolder.getRoot (), "does-not-exist.tmp");

    SMLSyncReport.writeReport (aZip,
                               _createResult (2, 2, 0),
                               aOrphans,
                               new CommonsArrayList <> (PI1, PI2),
                               aOrphans);

    final String sSummary = _readEntry (aZip, CSMLSync.ENTRY_SUMMARY);
    assertTrue (sSummary, sSummary.contains ("unregistered and re-registered"));
  }

  @Test
  public void testInSyncAddsNoNote () throws IOException
  {
    final File aZip = new File (m_aTempFolder.getRoot (), "report.zip");
    final File aOrphans = new File (m_aTempFolder.getRoot (), "does-not-exist.tmp");

    SMLSyncReport.writeReport (aZip, _createResult (5, 0, 0), aOrphans, new CommonsArrayList <> (), aOrphans);

    final String sSummary = _readEntry (aZip, CSMLSync.ENTRY_SUMMARY);
    assertFalse (sSummary, sSummary.contains ("<note>"));
  }

  @Test
  public void testSummaryRoundTrip () throws IOException
  {
    // The writer and the reader must agree on every attribute
    final File aZip = new File (m_aTempFolder.getRoot (), "report.zip");
    final File aOrphans = new File (m_aTempFolder.getRoot (), "does-not-exist.tmp");
    final SMLSyncResult aWritten = _createResult (7, 3, 2);

    SMLSyncReport.writeReport (aZip, aWritten, aOrphans, new CommonsArrayList <> (PI1), aOrphans);
    final SMLSyncResult aRead = SMLSyncReport.readSummary (aZip);

    assertEquals (aWritten.getStartDateTime (), aRead.getStartDateTime ());
    assertEquals (aWritten.getEndDateTime (), aRead.getEndDateTime ());
    assertEquals (aWritten.getSMLID (), aRead.getSMLID ());
    assertEquals (aWritten.getSMPID (), aRead.getSMPID ());
    assertEquals (aWritten.getSMLPageCount (), aRead.getSMLPageCount ());
    assertEquals (aWritten.getSMLParticipantCount (), aRead.getSMLParticipantCount ());
    assertEquals (aWritten.getLocalParticipantCount (), aRead.getLocalParticipantCount ());
    assertEquals (aWritten.getMissingInSMLCount (), aRead.getMissingInSMLCount ());
    assertEquals (aWritten.getOrphansInSMLCount (), aRead.getOrphansInSMLCount ());
  }

  @Test
  public void testAffectedCountPerAction () throws IOException
  {
    // This is what drives the number in the button and whether it is disabled
    final File aZip = new File (m_aTempFolder.getRoot (), "report.zip");
    final File aOrphans = new File (m_aTempFolder.getRoot (), "does-not-exist.tmp");
    SMLSyncReport.writeReport (aZip, _createResult (7, 3, 2), aOrphans, new CommonsArrayList <> (PI1), aOrphans);

    final SMLSyncResult aRead = SMLSyncReport.readSummary (aZip);
    assertEquals (3, ESMLRepairAction.REGISTER_MISSING.getAffectedCount (aRead));
    // Both orphan actions act on the same bucket
    assertEquals (2, ESMLRepairAction.REMOVE_ORPHANS.getAffectedCount (aRead));
    assertEquals (2, ESMLRepairAction.CREATE_LOCALLY.getAffectedCount (aRead));
  }

  @Test
  public void testGetValidSyncFileRejectsNull ()
  {
    // The download gate must reject anything that is not a report in the report directory.
    // Everything beyond null needs an initialized WebFileIO, so it is covered by the
    // SMPServerTestRule based tests rather than here.
    assertNull (SMLSyncJob.getValidSyncFile (null));
  }
}
