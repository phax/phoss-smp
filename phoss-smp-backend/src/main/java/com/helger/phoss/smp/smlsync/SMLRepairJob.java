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
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppol.smlclient.SMLExceptionHelper;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.smlhook.SmpSmlHelper;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.text.ReadOnlyMultilingualText;
import com.helger.web.scope.mgr.WebScoped;

/**
 * The long running job that repairs the differences between this SMP and the SML, based on a
 * reconciliation report created by {@link SMLSyncJob}.<br>
 * The participants are sent to the SML in chunks. If a whole chunk fails, every participant of that
 * chunk is retried individually, so that a single unusable participant does not prevent the rest of
 * the chunk from being repaired - and so that the resulting log says exactly which participants
 * could not be handled.<br>
 * Chapter 3.1.2.7 of the SML specification states that the SML data may change between reads, so a
 * report is always somewhat out of date. "The identifier is already in use" when registering and
 * "not found" when removing are therefore expected outcomes, not errors, and they are counted
 * separately.<br>
 * The caller must acquire {@link CSMLSync#LOCK} before starting this job - the job itself releases
 * the lock.
 *
 * @author Philip Helger
 * @since 8.5.1
 */
public class SMLRepairJob extends AbstractLongRunningJobRunnable
{
  /** The type of this long running job */
  public static final String JOB_TYPE = "sml-repair";

  private static final Logger LOGGER = LoggerFactory.getLogger (SMLRepairJob.class);

  private final File m_aReportFile;
  private final ESMLRepairAction m_eAction;

  public SMLRepairJob (@NonNull final File aReportFile,
                       @NonNull final ESMLRepairAction eAction,
                       @NonNull @Nonempty final String sUserID)
  {
    super (JOB_TYPE,
           new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Repair the SML registration differences"),
           () -> sUserID);
    ValueEnforcer.notNull (aReportFile, "ReportFile");
    ValueEnforcer.notNull (eAction, "Action");
    ValueEnforcer.notEmpty (sUserID, "UserID");
    m_aReportFile = aReportFile;
    m_eAction = eAction;
  }

  /**
   * Send a single chunk to the SML.
   *
   * @throws Exception
   *         if the SML rejected the whole chunk
   */
  private static void _sendChunk (@NonNull final ManageParticipantIdentifierServiceCaller aCaller,
                                  @NonNull @Nonempty final String sSMPID,
                                  @NonNull final ESMLRepairAction eAction,
                                  @NonNull final List <IParticipantIdentifier> aChunk) throws Exception
  {
    if (eAction == ESMLRepairAction.REGISTER_MISSING)
      aCaller.createList (aChunk, sSMPID);
    else
      aCaller.deleteList (aChunk, sSMPID);
  }

  /**
   * Send a single participant to the SML.
   *
   * @throws Exception
   *         if the SML rejected it
   */
  private static void _sendSingle (@NonNull final ManageParticipantIdentifierServiceCaller aCaller,
                                   @NonNull @Nonempty final String sSMPID,
                                   @NonNull final ESMLRepairAction eAction,
                                   @NonNull final IParticipantIdentifier aPI) throws Exception
  {
    if (eAction == ESMLRepairAction.REGISTER_MISSING)
      aCaller.create (sSMPID, aPI);
    else
      aCaller.delete (sSMPID, aPI);
  }

  @NonNull
  public LongRunningJobResult createLongRunningJobResult ()
  {
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    if (!aSettings.isSMLEnabled ())
      throw new IllegalStateException ("The SML connection is not enabled");

    final ISMLInfo aSMLInfo = aSettings.getSMLInfo ();
    if (aSMLInfo == null)
      throw new IllegalStateException ("No SML is selected in the SMP settings");

    final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
    if (StringHelper.isEmpty (sSMPID))
      throw new IllegalStateException ("No SMP ID is configured");

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final int nChunkSize = SMPServerConfiguration.getSMLRepairChunkSize ();

    // Read the participants to be repaired from the report
    final ICommonsList <IParticipantIdentifier> aAll = new CommonsArrayList <> ();
    final ICommonsList <String> aUnparsable = new CommonsArrayList <> ();
    try
    {
      SMLSyncReport.readEntry (m_aReportFile, m_eAction.getReportEntry (), sURI -> {
        final IParticipantIdentifier aPI = aIdentifierFactory.parseParticipantIdentifier (sURI);
        if (aPI == null)
          aUnparsable.add (sURI);
        else
          aAll.add (aPI);
      });
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to read '" +
                                       m_eAction.getReportEntry () +
                                       "' from the report '" +
                                       m_aReportFile.getName () +
                                       "'",
                                       ex);
    }

    LOGGER.info ("Starting the SML repair '" +
                 m_eAction.getID () +
                 "' for " +
                 aAll.size () +
                 " participant(s) of SMP '" +
                 sSMPID +
                 "' in chunks of " +
                 nChunkSize);

    final StringBuilder aLog = new StringBuilder ();
    aLog.append (m_eAction.getDisplayName ())
        .append ("\nReport: ")
        .append (m_aReportFile.getName ())
        .append ("\nSMP ID: ")
        .append (sSMPID)
        .append ("\nParticipants in the report: ")
        .append (aAll.size ())
        .append ("\nChunk size: ")
        .append (nChunkSize)
        .append ("\n\n");

    for (final String sURI : aUnparsable)
      aLog.append ("SKIPPED - not a valid participant identifier: ").append (sURI).append ('\n');

    int nSucceeded = 0;
    int nAlreadyDone = 0;
    int nFailed = 0;

    final ManageParticipantIdentifierServiceCaller aCaller = SmpSmlHelper.createSMLCallerPI (aSMLInfo);
    for (int nStart = 0; nStart < aAll.size (); nStart += nChunkSize)
    {
      final List <IParticipantIdentifier> aChunk = aAll.subList (nStart,
                                                                 Math.min (nStart + nChunkSize, aAll.size ()));
      try
      {
        _sendChunk (aCaller, sSMPID, m_eAction, aChunk);
        nSucceeded += aChunk.size ();
        aLog.append ("OK - chunk of ").append (aChunk.size ()).append (" participant(s)\n");
      }
      catch (final Exception exChunk)
      {
        // The SML rejects a CreateList or DeleteList as a whole, with no per item result, so the
        // only way to learn which participant is the problem is to retry them one by one
        final String sChunkError = StringHelper.getNotNull (SMLExceptionHelper.getFaultMessage (exChunk),
                                                            exChunk.getMessage ());
        LOGGER.warn ("The chunk starting at index " + nStart + " failed, retrying individually: " + sChunkError);
        aLog.append ("The chunk starting at ")
            .append (nStart)
            .append (" failed (")
            .append (sChunkError)
            .append ("), retrying individually\n");

        for (final IParticipantIdentifier aPI : aChunk)
        {
          final String sURI = aPI.getURIEncoded ();
          try
          {
            _sendSingle (aCaller, sSMPID, m_eAction, aPI);
            nSucceeded++;
            aLog.append ("  OK - ").append (sURI).append ('\n');
          }
          catch (final Exception exSingle)
          {
            final String sError = StringHelper.getNotNull (SMLExceptionHelper.getFaultMessage (exSingle),
                                                           exSingle.getMessage ());
            // An identifier that is already registered, respectively no longer registered, is the
            // expected outcome of acting on a report that is not perfectly up to date
            final boolean bAlreadyDone = m_eAction == ESMLRepairAction.REGISTER_MISSING ? _isAlreadyInUse (sError)
                                                                                        : _isNotFound (exSingle);
            if (bAlreadyDone)
            {
              nAlreadyDone++;
              aLog.append ("  ALREADY DONE - ").append (sURI).append (" - ").append (sError).append ('\n');
            }
            else
            {
              nFailed++;
              aLog.append ("  FAILED - ").append (sURI).append (" - ").append (sError).append ('\n');
            }
          }
        }
      }
    }

    aLog.append ("\nSucceeded: ")
        .append (nSucceeded)
        .append ("\nAlready done: ")
        .append (nAlreadyDone)
        .append ("\nFailed: ")
        .append (nFailed)
        .append ('\n');

    LOGGER.info ("Finished the SML repair '" +
                 m_eAction.getID () +
                 "': " +
                 nSucceeded +
                 " succeeded, " +
                 nAlreadyDone +
                 " already done, " +
                 nFailed +
                 " failed");
    AuditHelper.onAuditExecuteSuccess ("smp-sml-repair",
                                       m_eAction.getID (),
                                       sSMPID,
                                       Integer.valueOf (nSucceeded),
                                       Integer.valueOf (nAlreadyDone),
                                       Integer.valueOf (nFailed));

    return LongRunningJobResult.createText (aLog.toString ());
  }

  private static boolean _isAlreadyInUse (@NonNull final String sErrorMsg)
  {
    // The SML reports this as a badRequestFault with a message - there is no dedicated fault
    final String sLower = sErrorMsg.toLowerCase (CSMPServer.DEFAULT_LOCALE);
    return sLower.contains ("already in use") || sLower.contains ("already exist");
  }

  private static boolean _isNotFound (@NonNull final Exception ex)
  {
    return ex instanceof com.helger.peppol.smlclient.participant.NotFoundFault;
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
