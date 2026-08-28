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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileIOError;
import com.helger.io.file.FileOperationManager;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.photon.io.WebFileIO;
import com.helger.photon.mgrs.PhotonBasicManager;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.mgrs.longrun.ELongRunningJobResultType;
import com.helger.photon.mgrs.longrun.ILongRunningJobResultManager;
import com.helger.photon.mgrs.longrun.LongRunningJobData;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.lock.SingleRunLock;
import com.helger.text.ReadOnlyMultilingualText;
import com.helger.web.scope.mgr.WebScoped;

/**
 * A long running job that exports all Service Groups into a file below the data path. Contrary to a
 * synchronous export this does not block an HTTP thread and it does not keep the created data in
 * memory.<br>
 * The caller must acquire {@link #LOCK} before starting this job - the job itself releases the lock
 * when it is done.
 *
 * @author Philip Helger
 * @since 8.2.1
 */
public class ServiceGroupExportJob extends AbstractLongRunningJobRunnable
{
  /** The ID of this long running job type */
  public static final String JOB_ID = "service-group-export";

  /**
   * The process wide lock ensuring, that only a single Service Group export runs at a time. It is
   * used by this job as well as by the synchronous REST export APIs.
   */
  public static final SingleRunLock LOCK = new SingleRunLock ("Service Group export");

  /** The name of the directory below the data path, in which the export files are created */
  public static final String EXPORT_DIRECTORY = "servicegroup-export";

  /** The prefix of all created export files */
  public static final String EXPORT_FILENAME_PREFIX = "phoss-smp-export-";

  /** The extension of all created export files */
  public static final String EXPORT_FILENAME_EXTENSION = ".xml";

  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceGroupExportJob.class);

  private final boolean m_bIncludeBusinessCards;

  public ServiceGroupExportJob (final boolean bIncludeBusinessCards, @NonNull @Nonempty final String sUserID)
  {
    super (JOB_ID,
           new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Export all Service Groups"),
           () -> sUserID);
    ValueEnforcer.notEmpty (sUserID, "UserID");
    m_bIncludeBusinessCards = bIncludeBusinessCards;
  }

  /**
   * @return The directory in which all export files are created. Never <code>null</code>. The
   *         directory may not yet exist.
   */
  @NonNull
  public static File getExportDirectory ()
  {
    return WebFileIO.getDataIO ().getFile (EXPORT_DIRECTORY);
  }

  /**
   * @return The name of the export file to be created now. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public static String createExportFilename ()
  {
    return EXPORT_FILENAME_PREFIX + PDTIOHelper.getCurrentLocalDateTimeForFilename () + EXPORT_FILENAME_EXTENSION;
  }

  /**
   * Check whether the provided file is a Service Group export file that may be handed out to a
   * client, and return it in canonical form.<br>
   * This is the single place that decides which files are downloadable. A file is only accepted if
   * it is an existing regular file located <em>directly</em> in the export directory and if its
   * name follows the export file naming. The comparison is done on the canonical paths, so that
   * neither <code>..</code> path elements nor symbolic links can be used to escape the export
   * directory.
   *
   * @param aFile
   *        The file to be checked. May be <code>null</code>.
   * @return <code>null</code> if the provided file is not a downloadable export file, the canonical
   *         file otherwise.
   * @since 8.2.1
   */
  @Nullable
  public static File getValidExportFile (@Nullable final File aFile)
  {
    if (aFile == null)
      return null;

    try
    {
      final File aCanonicalFile = aFile.getCanonicalFile ();

      // Must be located directly in the export directory
      if (!getExportDirectory ().getCanonicalFile ().equals (aCanonicalFile.getParentFile ()))
        return null;

      // Must follow the export file naming
      final String sFilename = aCanonicalFile.getName ();
      if (!sFilename.startsWith (EXPORT_FILENAME_PREFIX) || !sFilename.endsWith (EXPORT_FILENAME_EXTENSION))
        return null;

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
   * Delete all export files that are older than the configured retention period, as well as the
   * long running job results that refer to them. If the configured retention period is &le; 0, the
   * export files are kept forever and nothing is deleted.
   *
   * @return The number of deleted export files. Always &ge; 0.
   * @see SMPServerConfiguration#getExportRetentionDays()
   * @since 8.2.1
   */
  @Nonnegative
  public static int purgeOldExportFiles ()
  {
    final int nRetentionDays = SMPServerConfiguration.getExportRetentionDays ();
    if (nRetentionDays <= 0)
    {
      // Keep forever
      return 0;
    }

    final File aExportDir = getExportDirectory ();
    if (!aExportDir.isDirectory ())
    {
      // Nothing was ever exported
      return 0;
    }

    final LocalDateTime aMaxDT = PDTFactory.getCurrentLocalDateTime ().minusDays (nRetentionDays);
    final ICommonsSet <String> aDeletedFilenames = new CommonsHashSet <> ();

    for (final File aFile : FileHelper.getDirectoryContent (aExportDir))
    {
      if (!aFile.isFile ())
        continue;
      if (!aFile.getName ().startsWith (EXPORT_FILENAME_PREFIX) ||
          !aFile.getName ().endsWith (EXPORT_FILENAME_EXTENSION))
      {
        // Don't touch foreign files
        continue;
      }
      if (PDTFactory.createLocalDateTime (aFile.lastModified ()).isAfter (aMaxDT))
        continue;

      if (FileOperationManager.INSTANCE.deleteFile (aFile).isSuccess ())
      {
        LOGGER.info ("Deleted the outdated Service Group export file '" + aFile.getAbsolutePath () + "'");
        aDeletedFilenames.add (aFile.getAbsolutePath ());
      }
      else
        LOGGER.warn ("Failed to delete the outdated Service Group export file '" +
                     aFile.getAbsolutePath () +
                     "'");
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

  @NonNull
  public LongRunningJobResult createLongRunningJobResult ()
  {
    // First get rid of the outdated exports, so that the disk usage stays bounded
    purgeOldExportFiles ();

    final FileIOError aError = WebFileIO.getDataIO ().createDirectory (EXPORT_DIRECTORY, true);
    if (aError.isFailure ())
      throw new IllegalStateException ("Failed to create the export directory: " + aError.toString ());

    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ICommonsList <ISMPServiceGroup> aAllServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();

    final File aFile = new File (getExportDirectory (), createExportFilename ());
    LOGGER.info ("Exporting " + aAllServiceGroups.size () + " Service Groups to '" + aFile.getAbsolutePath () + "'");

    try (final OutputStream aOS = FileHelper.getBufferedOutputStream (aFile))
    {
      if (aOS == null)
        throw new IllegalStateException ("Failed to open the export file '" +
                                         aFile.getAbsolutePath () +
                                         "' for writing");

      ServiceGroupExport.createExportDataXMLVer10 (aAllServiceGroups, m_bIncludeBusinessCards, aOS);
    }
    catch (final Exception ex)
    {
      // Don't leave a partially written file behind
      WebFileIO.getDataIO ().deleteFileIfExisting (EXPORT_DIRECTORY + "/" + aFile.getName ());
      throw new IllegalStateException ("Failed to create the Service Group export in '" +
                                       aFile.getAbsolutePath () +
                                       "'", ex);
    }

    LOGGER.info ("Successfully created the Service Group export in '" +
                 aFile.getAbsolutePath () +
                 "' with " +
                 aFile.length () +
                 " bytes");

    return LongRunningJobResult.createFile (aFile);
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
      LOCK.release ();
    }
  }
}
