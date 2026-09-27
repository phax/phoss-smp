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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.StreamHelper;
import com.helger.io.file.FileHelper;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
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
 * @since 8.5.1
 */
@Immutable
public final class SMLSyncReport
{
  private SMLSyncReport ()
  {}

  @NonNull
  private static IMicroDocument _createSummaryDocument (@NonNull final SMLSyncResult aResult)
  {
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.addElement ("sml-sync-report");
    eRoot.setAttribute ("version", "1.0");

    final IMicroElement eRun = eRoot.addElement ("run");
    eRun.setAttribute ("start", aResult.getStartDateTime ().toString ());
    eRun.setAttribute ("end", aResult.getEndDateTime ().toString ());
    eRun.setAttribute ("duration", aResult.getDuration ().toString ());

    final IMicroElement eSML = eRoot.addElement ("sml");
    eSML.setAttribute ("id", aResult.getSMLID ());

    final IMicroElement eSMP = eRoot.addElement ("smp");
    eSMP.setAttribute ("id", aResult.getSMPID ());

    final IMicroElement eCounts = eRoot.addElement ("counts");
    eCounts.setAttribute ("sml-pages", aResult.getSMLPageCount ());
    eCounts.setAttribute ("sml-participants", aResult.getSMLParticipantCount ());
    eCounts.setAttribute ("local-participants", aResult.getLocalParticipantCount ());
    eCounts.setAttribute ("missing-in-sml", aResult.getMissingInSMLCount ());
    eCounts.setAttribute ("orphans-in-sml", aResult.getOrphansInSMLCount ());

    if (aResult.isAllLocalParticipantsMissing ())
    {
      // Make the most important finding impossible to overlook
      eRoot.addElement ("note")
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
      try (final BufferedReader aReader = new BufferedReader (StreamHelper.createReader (FileHelper.getInputStream (aSrcFile),
                                                                                         StandardCharsets.UTF_8)))
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
   * Write the reconciliation report.
   *
   * @param aZipFile
   *        The ZIP file to be created. May not be <code>null</code>.
   * @param aResult
   *        The summary of the run. May not be <code>null</code>.
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
                                  @NonNull final Iterable <String> aMissingInSML,
                                  @NonNull final File aOrphansInSMLFile) throws IOException
  {
    ValueEnforcer.notNull (aZipFile, "ZipFile");
    ValueEnforcer.notNull (aResult, "Result");
    ValueEnforcer.notNull (aMissingInSML, "MissingInSML");
    ValueEnforcer.notNull (aOrphansInSMLFile, "OrphansInSMLFile");

    final OutputStream aOS = FileHelper.getBufferedOutputStream (aZipFile);
    if (aOS == null)
      throw new IOException ("Failed to open the report file '" + aZipFile.getAbsolutePath () + "' for writing");

    try (final ZipOutputStream aZOS = new ZipOutputStream (aOS, StandardCharsets.UTF_8))
    {
      _writeEntryFromString (aZOS, CSMLSync.ENTRY_SUMMARY, MicroWriter.getNodeAsString (_createSummaryDocument (aResult)));
      _writeEntryFromIterable (aZOS, CSMLSync.ENTRY_MISSING_IN_SML, aMissingInSML);
      _writeEntryFromFile (aZOS, CSMLSync.ENTRY_ORPHANS_IN_SML, aOrphansInSMLFile);
    }
  }
}
