/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.ui.secure;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.ESortOrder;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.http.CHttp;
import com.helger.io.misc.SizeHelper;
import com.helger.io.resource.FileSystemResource;
import com.helger.mime.CMimeType;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.smlsync.CSMLSync;
import com.helger.phoss.smp.smlsync.SMLSyncJob;
import com.helger.phoss.smp.ui.ajax.AbstractSMPAjaxExecutor;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.badge.BootstrapBadge;
import com.helger.photon.bootstrap5.badge.EBootstrapBadgeType;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.photon.mgrs.PhotonBasicManager;
import com.helger.photon.mgrs.longrun.ELongRunningJobResultType;
import com.helger.photon.mgrs.longrun.LongRunningJobData;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Reconcile the local Service Groups with the participants the SML holds for this SMP, and offer
 * the created reports for download.
 *
 * @author Philip Helger
 * @since 8.6.0
 */
public final class PageSecureSMLRegSync extends AbstractPageSecureSMLReg
{
  private static final String ACTION_START_SYNC = "startsync";

  /**
   * The ID of the long running job that created the report. Deliberately not a filename - see
   * {@link #_getDownloadableReport(String)}.
   */
  private static final String PARAM_JOB_ID = "jobid";

  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureSMLRegSync.class);

  private static final IAjaxFunctionDeclaration AJAX_DOWNLOAD_REPORT;

  /**
   * Resolve the report to be downloaded.<br>
   * The client only provides the ID of the long running job that created the report - never a
   * filename or a path. The ID is resolved via the long running job result manager, and the file
   * stored there must additionally pass {@link SMLSyncJob#getValidSyncFile(File)}, which only
   * accepts existing files located directly in the report directory whose name follows the report
   * file naming. Therefore no other file of the file system can be downloaded through this page.
   *
   * @param sJobID
   *        The long running job ID provided by the client. May be <code>null</code>.
   * @return <code>null</code> if no matching downloadable report exists.
   */
  @Nullable
  private static File _getDownloadableReport (@Nullable final String sJobID)
  {
    if (StringHelper.isEmpty (sJobID))
      return null;

    final LongRunningJobData aJobData = PhotonBasicManager.getLongRunningJobResultMgr ().getJobResultOfID (sJobID);
    if (aJobData == null)
      return null;

    // Only an SML reconciliation report may be downloaded here
    if (!SMLSyncJob.JOB_TYPE.equals (aJobData.getJobType ()))
      return null;

    final LongRunningJobResult aResult = aJobData.getResult ();
    if (aResult == null || aResult.getType () != ELongRunningJobResultType.FILE)
      return null;

    return SMLSyncJob.getValidSyncFile (aResult.getResultFile ());
  }

  static
  {
    // Ensure it can only be accessed by logged in users
    AJAX_DOWNLOAD_REPORT = CAjax.addAjaxWithLogin (new AbstractSMPAjaxExecutor ()
    {
      @Override
      protected void mainHandleRequest (@NonNull final LayoutExecutionContext aLEC,
                                        @NonNull final PhotonUnifiedResponse aAjaxResponse) throws Exception
      {
        final String sJobID = aLEC.params ().getAsStringTrimmed (PARAM_JOB_ID);
        final File aFile = _getDownloadableReport (sJobID);
        if (aFile == null)
        {
          LOGGER.warn ("Failed to resolve a downloadable SML reconciliation report for job ID '" + sJobID + "'");
          aAjaxResponse.setStatus (CHttp.HTTP_NOT_FOUND);
          return;
        }

        LOGGER.info ("Downloading the SML reconciliation report '" + aFile.getAbsolutePath () + "'");
        aAjaxResponse.setContent (new FileSystemResource (aFile));
        aAjaxResponse.setMimeType (CMimeType.APPLICATION_ZIP);
        aAjaxResponse.attachment (aFile.getName ());
      }
    });
  }

  public PageSecureSMLRegSync (@NonNull @Nonempty final String sID)
  {
    super (sID, "Reconcile participants");
  }

  /**
   * @return All reconciliation runs, successful as well as failed ones. A failed run must be listed
   *         too - otherwise an operator who starts a reconciliation that fails sees the "is running
   *         in the background" message and then nothing at all, forever. Never <code>null</code>.
   */
  @NonNull
  private static ICommonsList <LongRunningJobData> _getAllRuns ()
  {
    final ICommonsList <LongRunningJobData> ret = new CommonsArrayList <> ();
    PhotonBasicManager.getLongRunningJobResultMgr ().forEachJobResult (SMLSyncJob.JOB_TYPE, ret::add);
    return ret;
  }

  /**
   * @param aJobData
   *        The run to be checked. May not be <code>null</code>.
   * @return The report of a successfully finished run, or <code>null</code> if the run failed or
   *         its report was meanwhile deleted.
   */
  @Nullable
  private static File _getReportOfRun (@NonNull final LongRunningJobData aJobData)
  {
    final LongRunningJobResult aResult = aJobData.getResult ();
    if (aResult == null || aResult.getType () != ELongRunningJobResultType.FILE)
      return null;
    return SMLSyncJob.getValidSyncFile (aResult.getResultFile ());
  }

  private void _startSync (@NonNull final WebPageExecutionContext aWPEC)
  {
    // Only a single participant wide SML operation may run at a time
    if (!CSMLSync.LOCK.tryAcquire (aWPEC.getLoggedInUserID ()))
    {
      aWPEC.postRedirectGetInternal (warn ("Another SML operation is already running in the background. Please wait until it is finished."));
    }
    else
    {
      try
      {
        PhotonWorkerPool.getInstance ().run (SMLSyncJob.JOB_TYPE, new SMLSyncJob (aWPEC.getLoggedInUserID ()));
      }
      catch (final RuntimeException ex)
      {
        // The job was never started, so it can never release the lock
        CSMLSync.LOCK.release ();
        throw ex;
      }

      AuditHelper.onAuditExecuteSuccess ("smp-sml-sync-start", SMPServerConfiguration.getSMLSMPID ());
      aWPEC.postRedirectGetInternal (success ("The reconciliation with the SML is now running in the background. " +
                                              "The created report can be downloaded from this page as soon as it is finished."));
    }
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    if (!canShowPage (aWPEC))
      return;

    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final SizeHelper aSH = SizeHelper.getSizeHelperOfLocale (aDisplayLocale);

    if (aWPEC.hasAction (ACTION_START_SYNC))
    {
      _startSync (aWPEC);
      // Never reached - the action performs a redirect
    }

    aNodeList.addChild (info ("Compare the Service Groups of this SMP with the participants the SML has registered for it. " +
                              "This reads the complete participant list from the SML, so it runs in the background and the " +
                              "created report is stored on the server."));
    aNodeList.addChild (div ("The report lists the participants that exist here but are ").addChild (em ("not"))
                                                                                          .addChild (" registered at the SML - those cannot be resolved via DNS - and the participants the SML has registered for this SMP that do ")
                                                                                          .addChild (em ("not"))
                                                                                          .addChild (" exist here - those resolve to this SMP, which then answers HTTP 404."));

    final long nServiceGroupCount = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupCount ();
    if (nServiceGroupCount == 0)
      aNodeList.addChild (warn ("This SMP holds no Service Group. A reconciliation can still be started, and it will show everything the SML has registered for this SMP."));

    final boolean bSyncRunning = CSMLSync.LOCK.isRunning ();
    if (bSyncRunning)
    {
      final LocalDateTime aStartDT = CSMLSync.LOCK.getStartDateTime ();
      aNodeList.addChild (warn ("An SML operation is currently running in the background" +
                                (aStartDT == null ? ""
                                                  : " (started at " + PDTToString.getAsString (aStartDT, aDisplayLocale) + ")") +
                                ". Please wait until it is finished before starting a new one."));
    }

    final int nRetentionDays = SMPServerConfiguration.getSMLSyncRetentionDays ();
    if (nRetentionDays > 0)
    {
      aNodeList.addChild (info ("Created reports are stored on the server and are deleted after " +
                                nRetentionDays +
                                " days, the next time a reconciliation is started."));
    }
    else
      aNodeList.addChild (info ("Created reports are stored on the server and are never deleted automatically."));

    final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addChild (new BootstrapButton ().addChild ("Reconcile with the SML")
                                             .setIcon (EDefaultIcon.REFRESH)
                                             .setOnClick (aWPEC.getSelfHref ()
                                                               .add (CPageParam.PARAM_ACTION, ACTION_START_SYNC))
                                             .setDisabled (bSyncRunning));
    aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);

    final ICommonsList <LongRunningJobData> aAllRuns = _getAllRuns ();
    if (aAllRuns.isEmpty ())
      aNodeList.addChild (warn ("No reconciliation was performed yet."));
    else
    {
      final HCTable aTable = new HCTable (new DTCol ("Date").setDisplayType (EDTColType.DATETIME, aDisplayLocale)
                                                            .setInitialSorting (ESortOrder.DESCENDING),
                                          new DTCol ("Started by"),
                                          new DTCol ("Result"),
                                          new DTCol ("Size").setDisplayType (EDTColType.INT, aDisplayLocale),
                                          new DTCol ("Download")).setID (getID ());
      for (final LongRunningJobData aJobData : aAllRuns)
      {
        final HCRow aRow = aTable.addBodyRow ();
        aRow.addCell (PDTToString.getAsString (aJobData.getEndDateTime () != null ? aJobData.getEndDateTime ()
                                                                                  : aJobData.getStartDateTime (),
                                               aDisplayLocale));
        aRow.addCell (SecurityHelper.getUserDisplayName (aJobData.getStartingUserID (), aDisplayLocale));

        final File aFile = _getReportOfRun (aJobData);
        if (aFile != null)
        {
          aRow.addCell (aFile.getName ());
          aRow.addCell (aSH.getAsMatching (aFile.length (), 2));
          aRow.addCell (new BootstrapButton ().addChild ("Download")
                                              .setIcon (EDefaultIcon.SAVE)
                                              .setOnClick (AJAX_DOWNLOAD_REPORT.getInvocationURL (aRequestScope)
                                                                               .add (PARAM_JOB_ID,
                                                                                     aJobData.getID ())));
        }
        else
          if (aJobData.getExecutionSuccess ().isFalse ())
          {
            // Show why it failed - the reason is otherwise only in the server log
            final LongRunningJobResult aResult = aJobData.getResult ();
            // The stored text is the exception message followed by the stack trace. Only the first
            // line is shown - it carries the SML fault message, whereas the stack trace belongs in
            // the server log.
            final String sError = aResult == null ? null : aResult.getResultText ();
            String sHeadline = "No further details are available.";
            if (StringHelper.isNotEmpty (sError))
            {
              final int nIndex = sError.indexOf ('\n');
              sHeadline = nIndex < 0 ? sError : sError.substring (0, nIndex);
            }
            // Every row must have one cell per column - DataTables counts cells and does not
            // support colspan in a body row
            aRow.addCell (new BootstrapBadge (EBootstrapBadgeType.DANGER).addChild ("Failed"), div (sHeadline));
            aRow.addCell ();
            aRow.addCell ();
          }
          else
          {
            // Succeeded, but the report was deleted by the retention handling. A running job is
            // never in this list - results are only stored when a job ends.
            aRow.addCell (new BootstrapBadge (EBootstrapBadgeType.WARNING).addChild ("The report is no longer available"));
            aRow.addCell ();
            aRow.addCell ();
          }
      }
      aNodeList.addChild (aTable);

      final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
      aNodeList.addChild (aDataTables);
    }
  }
}
