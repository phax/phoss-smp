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
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.EContinue;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileIOError;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.FilenameHelper;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppol.smlclient.SMLExceptionHelper;
import com.helger.peppolid.CIdentifier;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.smlhook.SmpSmlHelper;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.WebFileIO;
import com.helger.photon.mgrs.PhotonBasicManager;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.mgrs.longrun.ELongRunningJobResultType;
import com.helger.photon.mgrs.longrun.ILongRunningJobResultManager;
import com.helger.photon.mgrs.longrun.LongRunningJobData;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.text.ReadOnlyMultilingualText;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xsds.peppol.id1.ParticipantIdentifierType;

/**
 * The long running job that reconciles the Service Groups of this SMP with the participants the
 * SML holds for this SMP, using the <code>List()</code> operation of chapter 3.1.2.7 of the SML
 * specification.<br>
 * The caller must acquire {@link CSMLSync#LOCK} before starting this job - the job itself releases
 * the lock.
 *
 * @author Philip Helger
 * @since 8.5.1
 */
public class SMLSyncJob extends AbstractLongRunningJobRunnable
{
  /** The type of this long running job */
  public static final String JOB_TYPE = "sml-sync";

  private static final Logger LOGGER = LoggerFactory.getLogger (SMLSyncJob.class);

  public SMLSyncJob (@NonNull @Nonempty final String sUserID)
  {
    super (JOB_TYPE,
           new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Reconcile the Service Groups with the SML"),
           () -> sUserID);
    ValueEnforcer.notEmpty (sUserID, "UserID");
  }

  /**
   * @return The directory in which all reports are created. Never <code>null</code>. The directory
   *         may not yet exist.
   */
  @NonNull
  public static File getSyncDirectory ()
  {
    return WebFileIO.getDataIO ().getFile (CSMLSync.SYNC_DIRECTORY);
  }

  /**
   * @return The unique name of the report file to be created now. Never <code>null</code> nor
   *         empty.
   */
  @NonNull
  @Nonempty
  public static String createSyncFilename ()
  {
    final String sFilename = CSMLSync.SYNC_FILENAME_PREFIX +
                             PDTIOHelper.getCurrentLocalDateTimeForFilename () +
                             "-" +
                             UUID.randomUUID ().toString () +
                             CSMLSync.SYNC_FILENAME_EXTENSION;

    final String ret = FilenameHelper.getAsSecureValidASCIIFilename (sFilename);
    if (ret == null)
      throw new IllegalStateException ("Failed to create a valid report filename from '" + sFilename + "'");
    return ret;
  }

  /**
   * Check whether the provided file is a reconciliation report that may be handed out to a client,
   * and return it in canonical form.<br>
   * This is the single place that decides which files are downloadable. A file is only accepted if
   * it is an existing regular file located <em>directly</em> in the report directory and if its
   * name follows the report file naming. The comparison is done on the canonical paths, so that
   * neither <code>..</code> path elements nor symbolic links can be used to escape the report
   * directory.
   *
   * @param aFile
   *        The file to be checked. May be <code>null</code>.
   * @return <code>null</code> if the provided file is not a downloadable report, the canonical
   *         file otherwise.
   */
  @Nullable
  public static File getValidSyncFile (@Nullable final File aFile)
  {
    if (aFile == null)
      return null;

    try
    {
      final File aCanonicalFile = aFile.getCanonicalFile ();

      // Must be located directly in the report directory
      if (!getSyncDirectory ().getCanonicalFile ().equals (aCanonicalFile.getParentFile ()))
        return null;

      // Must follow the report file naming
      final String sFilename = aCanonicalFile.getName ();
      if (!sFilename.startsWith (CSMLSync.SYNC_FILENAME_PREFIX) ||
          !sFilename.endsWith (CSMLSync.SYNC_FILENAME_EXTENSION))
      {
        return null;
      }

      // Must be an existing regular file
      if (!aCanonicalFile.isFile ())
        return null;

      return aCanonicalFile;
    }
    catch (final IOException ex)
    {
      LOGGER.warn ("Failed to determine the canonical file of '" + aFile.getAbsolutePath () + "'", ex);
      return null;
    }
  }

  /**
   * Delete all reports that are older than the configured retention period, as well as the long
   * running job results that refer to them. If the configured retention period is &le; 0, the
   * reports are kept forever and nothing is deleted.<br>
   * This is called at the start of every run. There is deliberately no scheduled job doing this,
   * because a reconciliation is triggered manually.
   *
   * @return The number of deleted reports. Always &ge; 0.
   * @see SMPServerConfiguration#getSMLSyncRetentionDays()
   */
  @Nonnegative
  public static int purgeOldSyncFiles ()
  {
    final int nRetentionDays = SMPServerConfiguration.getSMLSyncRetentionDays ();
    if (nRetentionDays <= 0)
    {
      // Keep forever
      return 0;
    }

    final File aSyncDir = getSyncDirectory ();
    if (!aSyncDir.isDirectory ())
    {
      // Nothing was ever reconciled
      return 0;
    }

    final LocalDateTime aMaxDT = PDTFactory.getCurrentLocalDateTime ().minusDays (nRetentionDays);
    final ICommonsSet <String> aDeletedFilenames = new CommonsHashSet <> ();

    for (final File aFile : FileHelper.getDirectoryContent (aSyncDir))
    {
      if (!aFile.isFile ())
        continue;
      if (!aFile.getName ().startsWith (CSMLSync.SYNC_FILENAME_PREFIX) ||
          !aFile.getName ().endsWith (CSMLSync.SYNC_FILENAME_EXTENSION))
      {
        // Don't touch foreign files
        continue;
      }
      if (PDTFactory.createLocalDateTime (aFile.lastModified ()).isAfter (aMaxDT))
        continue;

      if (FileOperationManager.INSTANCE.deleteFile (aFile).isSuccess ())
      {
        LOGGER.info ("Deleted the outdated SML reconciliation report '" + aFile.getAbsolutePath () + "'");
        aDeletedFilenames.add (aFile.getAbsolutePath ());
      }
      else
        LOGGER.warn ("Failed to delete the outdated SML reconciliation report '" + aFile.getAbsolutePath () + "'");
    }

    if (aDeletedFilenames.isNotEmpty ())
    {
      // Remove the long running job results that now point to deleted files
      final ILongRunningJobResultManager aResultMgr = PhotonBasicManager.getLongRunningJobResultMgr ();
      for (final LongRunningJobData aJobData : aResultMgr.getAllJobResults ())
      {
        final LongRunningJobResult aResult = aJobData.getResult ();
        if (aResult != null &&
            aResult.getType () == ELongRunningJobResultType.FILE &&
            aDeletedFilenames.contains (aResult.getResultFile ().getAbsolutePath ()))
        {
          aResultMgr.deleteResult (aJobData.getID ());
        }
      }
    }

    return aDeletedFilenames.size ();
  }

  /**
   * Stream all participants the SML holds for this SMP through the provided differ.
   *
   * @return The number of pages that were read from the SML.
   */
  @Nonnegative
  private static int _readSMLParticipants (@NonNull final ISMLInfo aSMLInfo,
                                           @NonNull @Nonempty final String sSMPID,
                                           @NonNull final SMLSyncDiffer aDiffer,
                                           @NonNull final Writer aAllWriter) throws Exception
  {
    final Duration aPageDelay = SMPServerConfiguration.getSMLSyncPageDelay ();
    final ManageParticipantIdentifierServiceCaller aCaller = SmpSmlHelper.createSMLCallerPI (aSMLInfo);

    return aCaller.listAllPages (sSMPID, aPage -> {
      // Record the complete list first - it is the input for restoring the participants after an
      // SMP was unregistered from the SML
      for (final ParticipantIdentifierType aPI : aPage.getParticipantIdentifier ())
        try
        {
          aAllWriter.write (CIdentifier.getURIEncoded (aPI));
          aAllWriter.write ('\n');
        }
        catch (final IOException ex)
        {
          throw new UncheckedIOException (ex);
        }

      aDiffer.addSMLPage (aPage);

      if (aPageDelay != null && !aPageDelay.isZero () && !aPageDelay.isNegative ())
      {
        // Be gentle with the SML
        try
        {
          Thread.sleep (aPageDelay.toMillis ());
        }
        catch (final InterruptedException ex)
        {
          Thread.currentThread ().interrupt ();
          return EContinue.BREAK;
        }
      }
      return EContinue.CONTINUE;
    });
  }

  /**
   * Remove the false positives from the orphan candidates, by keeping only those that are still
   * not held locally after the SML list was read.
   *
   * @return The number of real orphans.
   */
  @Nonnegative
  private static int _verifyOrphans (@NonNull final File aCandidateFile,
                                     @NonNull final ICommonsSet <String> aLocalIDsNow,
                                     @NonNull final File aVerifiedFile) throws IOException
  {
    int ret = 0;
    try (final BufferedReader aReader = new BufferedReader (StreamHelper.createReader (FileHelper.getInputStream (aCandidateFile),
                                                                                       StandardCharsets.UTF_8));
         final Writer aWriter = StreamHelper.createWriter (FileHelper.getBufferedOutputStream (aVerifiedFile),
                                                           StandardCharsets.UTF_8))
    {
      String sLine;
      while ((sLine = aReader.readLine ()) != null)
        if (StringHelper.isNotEmpty (sLine) && !aLocalIDsNow.contains (sLine))
        {
          aWriter.write (sLine);
          aWriter.write ('\n');
          ret++;
        }
    }
    return ret;
  }

  @NonNull
  public LongRunningJobResult createLongRunningJobResult ()
  {
    // First get rid of the outdated reports, so that the disk usage stays bounded
    purgeOldSyncFiles ();

    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    if (!aSettings.isSMLEnabled ())
      throw new IllegalStateException ("The SML connection is not enabled");

    final ISMLInfo aSMLInfo = aSettings.getSMLInfo ();
    if (aSMLInfo == null)
      throw new IllegalStateException ("No SML is selected in the SMP settings");

    final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
    if (StringHelper.isEmpty (sSMPID))
      throw new IllegalStateException ("No SMP ID is configured");

    final FileIOError aError = WebFileIO.getDataIO ().createDirectory (CSMLSync.SYNC_DIRECTORY, true);
    if (aError.isFailure ())
      throw new IllegalStateException ("Failed to create the report directory: " + aError.toString ());

    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final File aSyncDir = getSyncDirectory ();
    final File aZipFile = new File (aSyncDir, createSyncFilename ());
    final File aAllInSMLFile = new File (aSyncDir, aZipFile.getName () + ".all.tmp");
    final File aOrphanCandidateFile = new File (aSyncDir, aZipFile.getName () + ".candidates.tmp");
    final File aOrphanVerifiedFile = new File (aSyncDir, aZipFile.getName () + ".orphans.tmp");

    final LocalDateTime aStartDT = PDTFactory.getCurrentLocalDateTime ();
    try
    {
      final ICommonsSet <String> aLocalIDs = aServiceGroupMgr.getAllSMPServiceGroupIDs ();
      LOGGER.info ("Reconciling " +
                   aLocalIDs.size () +
                   " local Service Group(s) with the SML '" +
                   aSMLInfo.getManagementServiceURL () +
                   "' for SMP '" +
                   sSMPID +
                   "'");

      final SMLSyncDiffer aDiffer;
      final int nPageCount;
      try (final Writer aOrphanWriter = StreamHelper.createWriter (FileHelper.getBufferedOutputStream (aOrphanCandidateFile),
                                                                   StandardCharsets.UTF_8);
           final Writer aAllWriter = StreamHelper.createWriter (FileHelper.getBufferedOutputStream (aAllInSMLFile),
                                                               StandardCharsets.UTF_8))
      {
        aDiffer = new SMLSyncDiffer (aLocalIDs, sID -> {
          try
          {
            aOrphanWriter.write (sID);
            aOrphanWriter.write ('\n');
          }
          catch (final IOException ex)
          {
            throw new UncheckedIOException (ex);
          }
        });
        nPageCount = _readSMLParticipants (aSMLInfo, sSMPID, aDiffer, aAllWriter);
      }

      // The SML list was read over a period of time, during which Service Groups may have been
      // created and deleted. Re-reading the local identifiers removes exactly those false
      // positives - the SML offers no way to re-check a single participant.
      final ICommonsSet <String> aLocalIDsNow = aServiceGroupMgr.getAllSMPServiceGroupIDs ();
      final ICommonsSet <String> aMissing = SMLSyncDiffer.getVerifiedMissingInSML (aDiffer.getMissingInSMLCandidates (),
                                                                                   aLocalIDsNow);
      final int nOrphans = _verifyOrphans (aOrphanCandidateFile, aLocalIDsNow, aOrphanVerifiedFile);

      final SMLSyncResult aResult = new SMLSyncResult (aStartDT,
                                                       PDTFactory.getCurrentLocalDateTime (),
                                                       aSMLInfo.getID (),
                                                       sSMPID,
                                                       nPageCount,
                                                       aDiffer.getSMLParticipantCount (),
                                                       aDiffer.getLocalParticipantCount (),
                                                       aMissing.size (),
                                                       nOrphans);

      SMLSyncReport.writeReport (aZipFile, aResult, aAllInSMLFile, aMissing, aOrphanVerifiedFile);

      LOGGER.info ("Successfully created the SML reconciliation report in '" +
                   aZipFile.getAbsolutePath () +
                   "': " +
                   aResult.toString ());
      AuditHelper.onAuditExecuteSuccess ("smp-sml-sync",
                                         sSMPID,
                                         aSMLInfo.getID (),
                                         Integer.valueOf (aResult.getSMLParticipantCount ()),
                                         Integer.valueOf (aResult.getLocalParticipantCount ()),
                                         Integer.valueOf (aResult.getMissingInSMLCount ()),
                                         Integer.valueOf (aResult.getOrphansInSMLCount ()));
    }
    catch (final Exception ex)
    {
      // Don't leave a partially written report behind
      FileOperationManager.INSTANCE.deleteFileIfExisting (aZipFile);

      // Prefer the SML fault message. It is far more specific than the exception class name, and it
      // is this message that ends up in the stored job result and therefore in the UI - the cause
      // is only visible in the stack trace of the server log.
      String sErrorMsg = SMLExceptionHelper.getFaultMessage (ex);
      if (StringHelper.isEmpty (sErrorMsg))
        sErrorMsg = ex.getClass ().getName () + " - " + ex.getMessage ();

      AuditHelper.onAuditExecuteFailure ("smp-sml-sync", sSMPID, aSMLInfo.getID (), sErrorMsg);
      throw new IllegalStateException ("Failed to reconcile the Service Groups of SMP '" +
                                       sSMPID +
                                       "' with the SML '" +
                                       aSMLInfo.getManagementServiceURL () +
                                       "': " +
                                       sErrorMsg,
                                       ex);
    }
    finally
    {
      FileOperationManager.INSTANCE.deleteFileIfExisting (aAllInSMLFile);
      FileOperationManager.INSTANCE.deleteFileIfExisting (aOrphanCandidateFile);
      FileOperationManager.INSTANCE.deleteFileIfExisting (aOrphanVerifiedFile);
    }

    return LongRunningJobResult.createFile (aZipFile);
  }

  @Override
  public void run ()
  {
    // A Web Scope is needed for the DB access as well as for storing the job result
    try (final WebScoped w = new WebScoped ())
    {
      super.run ();
    }
    finally
    {
      // Always release, even if the job failed
      CSMLSync.LOCK.release ();
    }
  }
}
