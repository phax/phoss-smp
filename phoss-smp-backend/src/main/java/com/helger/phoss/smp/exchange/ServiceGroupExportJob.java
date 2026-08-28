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
import java.io.OutputStream;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileIOError;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.photon.io.WebFileIO;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
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

  @NonNull
  public LongRunningJobResult createLongRunningJobResult ()
  {
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
