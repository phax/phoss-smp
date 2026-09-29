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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingBufferedReader;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.io.file.FileHelper;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;
import com.helger.xml.microdom.serialize.MicroWriter;

/**
 * Writes the result of a reconciliation of the local Service Groups with the SML as a ZIP file
 * containing
 * <ul>
 * <li>{@link CSMLSync#ENTRY_SUMMARY} - the run metadata and the counts</li>
 * <li>{@link CSMLSync#ENTRY_MISSING_IN_SML} - the participants that are missing at the SML</li>
 * <li>{@link CSMLSync#ENTRY_ORPHANS_IN_SML} - the participants the SML holds but this SMP does
 * not</li>
 * </ul>
 * A ZIP is used, so that the two lists never have to be truncated: they are written line by line,
 * so an SMP with hundreds of thousands of participants produces a complete report without the full
 * lists ever being held in memory, and the highly repetitive participant identifiers compress very
 * well.
 *
 * @author Philip Helger
 * @since 8.6.0
 */
@Immutable
public final class SMLSyncReport
{
  // The element and attribute names of the summary, shared by the writer and the reader
  private static final String EL_ROOT = "sml-sync-report";
  private static final String EL_RUN = "run";
  private static final String EL_SML = "sml";
  private static final String EL_SMP = "smp";
  private static final String EL_COUNTS = "counts";
  private static final String EL_NOTE = "note";
  private static final String ATTR_VERSION = "version";
  private static final String ATTR_START = "start";
  private static final String ATTR_END = "end";
  private static final String ATTR_DURATION = "duration";
  private static final String ATTR_ID = "id";
  private static final String ATTR_SML_PAGES = "sml-pages";
  private static final String ATTR_SML_PARTICIPANTS = "sml-participants";
  private static final String ATTR_LOCAL_PARTICIPANTS = "local-participants";
  private static final String ATTR_MISSING_IN_SML = "missing-in-sml";
  private static final String ATTR_ORPHANS_IN_SML = "orphans-in-sml";

  private SMLSyncReport ()
  {}

  @NonNull
  private static IMicroDocument _createSummaryDocument (@NonNull final SMLSyncResult aResult)
  {
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.addElement (EL_ROOT);
    eRoot.setAttribute (ATTR_VERSION, "1.0");

    final IMicroElement eRun = eRoot.addElement (EL_RUN);
    eRun.setAttribute (ATTR_START, aResult.getStartDateTime ().toString ());
    eRun.setAttribute (ATTR_END, aResult.getEndDateTime ().toString ());
    eRun.setAttribute (ATTR_DURATION, aResult.getDuration ().toString ());

    final IMicroElement eSML = eRoot.addElement (EL_SML);
    eSML.setAttribute (ATTR_ID, aResult.getSMLID ());

    final IMicroElement eSMP = eRoot.addElement (EL_SMP);
    eSMP.setAttribute (ATTR_ID, aResult.getSMPID ());

    final IMicroElement eCounts = eRoot.addElement (EL_COUNTS);
    eCounts.setAttribute (ATTR_SML_PAGES, aResult.getSMLPageCount ());
    eCounts.setAttribute (ATTR_SML_PARTICIPANTS, aResult.getSMLParticipantCount ());
    eCounts.setAttribute (ATTR_LOCAL_PARTICIPANTS, aResult.getLocalParticipantCount ());
    eCounts.setAttribute (ATTR_MISSING_IN_SML, aResult.getMissingInSMLCount ());
    eCounts.setAttribute (ATTR_ORPHANS_IN_SML, aResult.getOrphansInSMLCount ());

    if (aResult.isAllLocalParticipantsMissing ())
    {
      // Make the most important finding impossible to overlook
      eRoot.addElement (EL_NOTE)
           .addText ("All local participants are missing at the SML. Unregistering an SMP from the SML" +
                     " deletes all of its participants, so this is the signature of an SMP that was" +
                     " unregistered and re-registered - and not of a broken SML connection.");
    }

    return aDoc;
  }

  private static void _writeEntryFromFile (@NonNull final ZipOutputStream aZOS,
                                           @NonNull @Nonempty final String sEntryName,
                                           @NonNull final File aSrcFile) throws IOException
  {
    aZOS.putNextEntry (new ZipEntry (sEntryName));
    if (aSrcFile.isFile ())
    {
      // Copy line by line, so that the content is never held in memory at once
      try (final NonBlockingBufferedReader aReader = FileHelper.getBufferedReader (aSrcFile, StandardCharsets.UTF_8))
      {
        final Writer aWriter = StreamHelper.createWriter (aZOS, StandardCharsets.UTF_8);
        String sLine;
        while ((sLine = aReader.readLine ()) != null)
        {
          aWriter.write (sLine);
          aWriter.write ('\n');
        }
        aWriter.flush ();
      }
    }
    aZOS.closeEntry ();
  }

  private static void _writeEntryFromIterable (@NonNull final ZipOutputStream aZOS,
                                               @NonNull @Nonempty final String sEntryName,
                                               @NonNull final Iterable <String> aLines) throws IOException
  {
    aZOS.putNextEntry (new ZipEntry (sEntryName));
    final Writer aWriter = StreamHelper.createWriter (aZOS, StandardCharsets.UTF_8);
    for (final String sLine : aLines)
    {
      aWriter.write (sLine);
      aWriter.write ('\n');
    }
    aWriter.flush ();
    aZOS.closeEntry ();
  }

  private static void _writeEntryFromString (@NonNull final ZipOutputStream aZOS,
                                             @NonNull @Nonempty final String sEntryName,
                                             @NonNull final String sContent) throws IOException
  {
    aZOS.putNextEntry (new ZipEntry (sEntryName));
    aZOS.write (sContent.getBytes (StandardCharsets.UTF_8));
    aZOS.closeEntry ();
  }

  /**
   * Read the summary of a previously created report. This is a cheap operation - the summary is a
   * few hundred bytes, in contrast to the participant lists next to it.
   *
   * @param aZipFile
   *        The report to be read. May not be <code>null</code>.
   * @return The summary of the run. Never <code>null</code>.
   * @throws IOException
   *         If the report cannot be read, or does not contain a well formed summary
   */
  @NonNull
  public static SMLSyncResult readSummary (@NonNull final File aZipFile) throws IOException
  {
    ValueEnforcer.notNull (aZipFile, "ZipFile");

    try (final ZipFile aZip = new ZipFile (aZipFile))
    {
      final ZipEntry aEntry = aZip.getEntry (CSMLSync.ENTRY_SUMMARY);
      if (aEntry == null)
        throw new IOException ("The report '" +
                               aZipFile.getName () +
                               "' contains no entry '" +
                               CSMLSync.ENTRY_SUMMARY +
                               "'");

      final IMicroDocument aDoc = MicroReader.readMicroXML (aZip.getInputStream (aEntry));
      final IMicroElement eRoot = aDoc == null ? null : aDoc.getDocumentElement ();
      if (eRoot == null)
        throw new IOException ("The summary of the report '" + aZipFile.getName () + "' is not well formed XML");

      final IMicroElement eRun = eRoot.getFirstChildElement (EL_RUN);
      final IMicroElement eSML = eRoot.getFirstChildElement (EL_SML);
      final IMicroElement eSMP = eRoot.getFirstChildElement (EL_SMP);
      final IMicroElement eCounts = eRoot.getFirstChildElement (EL_COUNTS);
      if (eRun == null || eSML == null || eSMP == null || eCounts == null)
        throw new IOException ("The summary of the report '" + aZipFile.getName () + "' is incomplete");

      try
      {
        return new SMLSyncResult (LocalDateTime.parse (eRun.getAttributeValue (ATTR_START)),
                                  LocalDateTime.parse (eRun.getAttributeValue (ATTR_END)),
                                  eSML.getAttributeValue (ATTR_ID),
                                  eSMP.getAttributeValue (ATTR_ID),
                                  eCounts.getAttributeValueAsInt (ATTR_SML_PAGES, 0),
                                  eCounts.getAttributeValueAsInt (ATTR_SML_PARTICIPANTS, 0),
                                  eCounts.getAttributeValueAsInt (ATTR_LOCAL_PARTICIPANTS, 0),
                                  eCounts.getAttributeValueAsInt (ATTR_MISSING_IN_SML, 0),
                                  eCounts.getAttributeValueAsInt (ATTR_ORPHANS_IN_SML, 0));
      }
      catch (final RuntimeException ex)
      {
        throw new IOException ("The summary of the report '" + aZipFile.getName () + "' cannot be interpreted", ex);
      }
    }
  }

  /**
   * Read one of the participant list entries of a previously created report, line by line, so that
   * a large list never has to be held in memory.
   *
   * @param aZipFile
   *        The report to be read. May not be <code>null</code>.
   * @param sEntryName
   *        The ZIP entry to be read, one of {@link CSMLSync#ENTRY_ALL_IN_SML},
   *        {@link CSMLSync#ENTRY_MISSING_IN_SML} or {@link CSMLSync#ENTRY_ORPHANS_IN_SML}. May
   *        neither be <code>null</code> nor empty.
   * @param aLineConsumer
   *        Invoked for every non-empty line, in file order. May not be <code>null</code>.
   * @return The number of lines that were handed over. Always &ge; 0.
   * @throws IOException
   *         In case of an IO error, or if the entry does not exist
   */
  @Nonnegative
  public static int readEntry (@NonNull final File aZipFile,
                               @NonNull @Nonempty final String sEntryName,
                               @NonNull final Consumer <String> aLineConsumer) throws IOException
  {
    ValueEnforcer.notNull (aZipFile, "ZipFile");
    ValueEnforcer.notEmpty (sEntryName, "EntryName");
    ValueEnforcer.notNull (aLineConsumer, "LineConsumer");

    int ret = 0;
    try (final ZipFile aZip = new ZipFile (aZipFile))
    {
      final ZipEntry aEntry = aZip.getEntry (sEntryName);
      if (aEntry == null)
        throw new IOException ("The report '" + aZipFile.getName () + "' contains no entry '" + sEntryName + "'");

      try (final NonBlockingBufferedReader aReader = new NonBlockingBufferedReader (StreamHelper.createReader (aZip.getInputStream (aEntry),
                                                                                                               StandardCharsets.UTF_8)))
      {
        String sLine;
        while ((sLine = aReader.readLine ()) != null)
          if (StringHelper.isNotEmpty (sLine))
          {
            aLineConsumer.accept (sLine);
            ret++;
          }
      }
    }
    return ret;
  }

  /**
   * Write the reconciliation report.
   *
   * @param aZipFile
   *        The ZIP file to be created. May not be <code>null</code>.
   * @param aResult
   *        The summary of the run. May not be <code>null</code>.
   * @param aAllInSMLFile
   *        A file containing all URI encoded participant identifiers the SML has registered for
   *        this SMP, one per line. May not be <code>null</code>, but does not need to exist.
   * @param aMissingInSML
   *        The URI encoded participant identifiers that are missing at the SML. May not be
   *        <code>null</code>.
   * @param aOrphansInSMLFile
   *        A file containing the URI encoded participant identifiers the SML holds but this SMP
   *        does not, one per line. May not be <code>null</code>, but does not need to exist - an
   *        absent file is treated like an empty list.
   * @throws IOException
   *         In case of an IO error
   */
  public static void writeReport (@NonNull final File aZipFile,
                                  @NonNull final SMLSyncResult aResult,
                                  @NonNull final File aAllInSMLFile,
                                  @NonNull final Iterable <String> aMissingInSML,
                                  @NonNull final File aOrphansInSMLFile) throws IOException
  {
    ValueEnforcer.notNull (aZipFile, "ZipFile");
    ValueEnforcer.notNull (aResult, "Result");
    ValueEnforcer.notNull (aAllInSMLFile, "AllInSMLFile");
    ValueEnforcer.notNull (aMissingInSML, "MissingInSML");
    ValueEnforcer.notNull (aOrphansInSMLFile, "OrphansInSMLFile");

    final OutputStream aOS = FileHelper.getBufferedOutputStream (aZipFile);
    if (aOS == null)
      throw new IOException ("Failed to open the report file '" + aZipFile.getAbsolutePath () + "' for writing");

    try (final ZipOutputStream aZOS = new ZipOutputStream (aOS, StandardCharsets.UTF_8))
    {
      _writeEntryFromString (aZOS,
                             CSMLSync.ENTRY_SUMMARY,
                             MicroWriter.getNodeAsString (_createSummaryDocument (aResult)));
      _writeEntryFromFile (aZOS, CSMLSync.ENTRY_ALL_IN_SML, aAllInSMLFile);
      _writeEntryFromIterable (aZOS, CSMLSync.ENTRY_MISSING_IN_SML, aMissingInSML);
      _writeEntryFromFile (aZOS, CSMLSync.ENTRY_ORPHANS_IN_SML, aOrphansInSMLFile);
    }
  }
}
