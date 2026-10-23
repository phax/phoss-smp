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
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.compare.ESortOrder;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringParser;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.HCCheckBox;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.smlsync.CSMLSync;
import com.helger.phoss.smp.smlsync.ESMLRepairAction;
import com.helger.phoss.smp.smlsync.SMLRepairHistory;
import com.helger.phoss.smp.smlsync.SMLRepairJob;
import com.helger.phoss.smp.smlsync.SMLSyncJob;
import com.helger.phoss.smp.smlsync.SMLSyncReport;
import com.helger.phoss.smp.smlsync.SMLSyncResult;
import com.helger.photon.bootstrap5.badge.BootstrapBadge;
import com.helger.photon.bootstrap5.badge.EBootstrapBadgeType;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap5.button.EBootstrapButtonType;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.form.BootstrapForm;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.photon.mgrs.PhotonBasicManager;
import com.helger.photon.mgrs.longrun.ELongRunningJobResultType;
import com.helger.photon.mgrs.longrun.LongRunningJobData;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;

/**
 * Repair the differences between this SMP and the SML, based on a reconciliation report created on
 * the <em>Reconcile participants</em> page.
 *
 * @author Philip Helger
 * @since 8.6.0
 */
public final class PageSecureSMLRegRepair extends AbstractPageSecureSMLReg
{
  private static final String PARAM_JOB_ID = "jobid";
  private static final String PARAM_REPAIR_ACTION = "repairaction";
  private static final String PARAM_LIMIT = "limit";
  private static final String PARAM_PRESELECT = "preselect";
  /** The name of the checkbox of a single participant */
  private static final String FIELD_PARTICIPANT = "pi";
  private static final String ACTION_CONFIRM = "confirm";
  private static final String VALUE_ALL = "all";
  private static final String VALUE_NONE = "none";
  private static final int DEFAULT_LIMIT = 100;
  private static final int [] LIMITS = { DEFAULT_LIMIT, 1000, 10000 };
  private static final int PREVIEW_COUNT = 20;

  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureSMLRegRepair.class);

  public PageSecureSMLRegRepair (@NonNull @Nonempty final String sID)
  {
    super (sID, "Repair SML differences");
  }

  /**
   * Resolve the report to be acted on. The client only provides the ID of the long running job that
   * created it - never a filename or a path - and the file stored there must pass
   * {@link SMLSyncJob#getValidSyncFile(File)}.
   *
   * @param sJobID
   *        The long running job ID provided by the client. May be <code>null</code>.
   * @return <code>null</code> if no matching report exists.
   */
  @Nullable
  private static File _getReport (@Nullable final String sJobID)
  {
    if (StringHelper.isEmpty (sJobID))
      return null;

    final LongRunningJobData aJobData = PhotonBasicManager.getLongRunningJobResultMgr ().getJobResultOfID (sJobID);
    if (aJobData == null || !SMLSyncJob.JOB_TYPE.equals (aJobData.getJobType ()))
      return null;

    final LongRunningJobResult aResult = aJobData.getResult ();
    if (aResult == null || aResult.getType () != ELongRunningJobResultType.FILE)
      return null;

    return SMLSyncJob.getValidSyncFile (aResult.getResultFile ());
  }

  @NonNull
  private static ICommonsList <LongRunningJobData> _getAllOfType (@NonNull @Nonempty final String sJobType)
  {
    final ICommonsList <LongRunningJobData> ret = new CommonsArrayList <> ();
    PhotonBasicManager.getLongRunningJobResultMgr ().forEachJobResult (sJobType, ret::add);
    return ret;
  }

  private void _startRepair (@NonNull final WebPageExecutionContext aWPEC,
                             @NonNull final File aReportFile,
                             @NonNull final ESMLRepairAction eAction,
                             @NonNull final Set <String> aSelectedURIs)
  {
    // The same lock as the reconciliation - a repair must not run while the SML list is being read
    if (!CSMLSync.LOCK.tryAcquire (aWPEC.getLoggedInUserID ()))
    {
      aWPEC.postRedirectGetInternal (warn ("Another SML operation is already running in the background. Please wait until it is finished."));
    }
    else
    {
      try
      {
        PhotonWorkerPool.getInstance ()
                        .run (SMLRepairJob.JOB_TYPE,
                              new SMLRepairJob (aReportFile, eAction, aWPEC.getLoggedInUserID (), aSelectedURIs));
      }
      catch (final RuntimeException ex)
      {
        // The job was never started, so it can never release the lock
        CSMLSync.LOCK.release ();
        throw ex;
      }

      LOGGER.info ("Started the SML repair '" +
                   eAction.getID () +
                   "' from report '" +
                   aReportFile.getName () +
                   "' for " +
                   aSelectedURIs.size () +
                   " selected participant(s)");
      aWPEC.postRedirectGetInternal (success (div ("The repair is now running in the background. The result appears on this page as soon as it is finished.")).addChild (div ("The numbers of the report will not change - run a new reconciliation afterwards to see the current state.")));
    }
  }

  @Nonnegative
  private static int _getLimit (@Nullable final String sLimit)
  {
    if (VALUE_ALL.equals (sLimit))
      return Integer.MAX_VALUE;

    final int ret = StringParser.parseInt (sLimit, DEFAULT_LIMIT);
    for (final int n : LIMITS)
      if (n == ret)
        return ret;
    return DEFAULT_LIMIT;
  }

  @NonNull
  private static ICommonsSet <String> _getSelected (@NonNull final WebPageExecutionContext aWPEC)
  {
    final ICommonsSet <String> ret = new CommonsLinkedHashSet <> ();
    // The parameter container only offers single values
    final String [] aValues = aWPEC.getRequestScope ().getRequest ().getParameterValues (FIELD_PARTICIPANT);
    if (aValues != null)
      for (final String sURI : aValues)
        if (StringHelper.isNotEmpty (sURI))
          ret.add (sURI.trim ());
    return ret;
  }

  private void _addWarnings (@NonNull final HCNodeList aNodeList,
                             @NonNull final ESMLRepairAction eAction,
                             @NonNull final String sCount)
  {
    if (eAction.isLocalOperation ())
    {
      aNodeList.addChild (info (div ("This would create " + sCount + " Service Group(s) locally, owned by you."))
                                                                                                                 .addChild (div ("Nothing is sent to the SML - these participants are already registered there, which is exactly why they show up as orphans."))
                                                                                                                 .addChild (div ("The Service Groups are created without any endpoint, so the participants stay unreachable until their endpoints are added.")));
    }
    else
    {
      aNodeList.addChild (info (div ("This would send " +
                                     sCount +
                                     " participant(s) to the SML, in chunks of " +
                                     SMPServerConfiguration.getSMLRepairChunkSize () +
                                     ".")).addChild (div ("The report was created at a point in time and the SML may have changed since. Participants that are already in the wanted state are counted separately and are not treated as an error.")));

      if (eAction.isDestructive ())
      {
        aNodeList.addChild (error (div ("This removes participants from the SML, which makes them unreachable.")).addChild (div ("Every removed participant becomes free for another SMP to claim, so this cannot reliably be undone.")));
        aNodeList.addChild (info (div ("If these participants are missing locally because local data was lost, ")).addChild (em ("Create orphans locally"))
                                                                                                                  .addChild (" is what you want instead - it adopts them rather than deleting them from the network."));
      }
    }
  }

  /**
   * Show the participants of the report that the selected action operates on, so that the operator
   * can select the ones to act on.
   */
  private void _showSelection (@NonNull final WebPageExecutionContext aWPEC,
                               @NonNull final File aReportFile,
                               @NonNull final ESMLRepairAction eAction)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    final String sJobID = aWPEC.params ().getAsStringTrimmed (PARAM_JOB_ID);
    final String sLimitParam = aWPEC.params ().getAsStringTrimmed (PARAM_LIMIT);
    final int nLimit = _getLimit (sLimitParam);
    final boolean bPreselect = VALUE_ALL.equals (aWPEC.params ().getAsStringTrimmed (PARAM_PRESELECT));

    // Only the shown participants are kept in memory
    final ICommonsList <String> aShown = new CommonsArrayList <> ();
    final int nTotal;
    try
    {
      nTotal = SMLSyncReport.readEntry (aReportFile, eAction.getReportEntry (), sURI -> {
        if (aShown.size () < nLimit)
          aShown.add (sURI);
      });
    }
    catch (final Exception ex)
    {
      aNodeList.addChild (error ("Failed to read the report '" + aReportFile.getName () + "': " + ex.getMessage ()));
      return;
    }

    aNodeList.addChild (getUIHandler ().createActionHeader (eAction.getDisplayName ()));

    if (nTotal == 0)
    {
      aNodeList.addChild (success ("The report contains no participant for this action - there is nothing to do."));
      return;
    }

    aNodeList.addChild (info ("Select the participants this action should be applied to. Nothing is selected by default and nothing is sent anywhere before you confirmed the selection on the next page."));

    if (nTotal > aShown.size ())
    {
      aNodeList.addChild (warn ("The report contains " +
                                nTotal +
                                " participants for this action but only the first " +
                                aShown.size () +
                                " are shown. Increase the number of shown participants to be able to select more."));
    }

    // Number of shown participants + select all/none
    final BootstrapButtonToolbar aLimitToolbar = aNodeList.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    for (final int n : LIMITS)
      aLimitToolbar.addChild (new BootstrapButton (nLimit == n ? EBootstrapButtonType.PRIMARY
                                                               : EBootstrapButtonType.OUTLINE_SECONDARY).addChild ("Show " +
                                                                                                                   n)
                                                                                                        .setOnClick (aWPEC.getSelfHref ()
                                                                                                                          .add (PARAM_JOB_ID,
                                                                                                                                sJobID)
                                                                                                                          .add (PARAM_REPAIR_ACTION,
                                                                                                                                eAction.getID ())
                                                                                                                          .add (PARAM_LIMIT,
                                                                                                                                n)));
    aLimitToolbar.addChild (new BootstrapButton (nLimit == Integer.MAX_VALUE ? EBootstrapButtonType.PRIMARY
                                                                             : EBootstrapButtonType.OUTLINE_SECONDARY).addChild ("Show all")
                                                                                                                      .setOnClick (aWPEC.getSelfHref ()
                                                                                                                                        .add (PARAM_JOB_ID,
                                                                                                                                              sJobID)
                                                                                                                                        .add (PARAM_REPAIR_ACTION,
                                                                                                                                              eAction.getID ())
                                                                                                                                        .add (PARAM_LIMIT,
                                                                                                                                              VALUE_ALL)));
    for (final String sPreselect : new String [] { VALUE_ALL, VALUE_NONE })
      aLimitToolbar.addButton (VALUE_ALL.equals (sPreselect) ? "Select all shown" : "Select none",
                               aWPEC.getSelfHref ()
                                    .add (PARAM_JOB_ID, sJobID)
                                    .add (PARAM_REPAIR_ACTION, eAction.getID ())
                                    .addIf (PARAM_LIMIT, sLimitParam, StringHelper::isNotEmpty)
                                    .add (PARAM_PRESELECT, sPreselect),
                               EDefaultIcon.REFRESH);

    final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormSelf (aWPEC));
    aForm.addChild (new HCHiddenField (PARAM_JOB_ID, sJobID));
    aForm.addChild (new HCHiddenField (PARAM_REPAIR_ACTION, eAction.getID ()));

    // No DataTables here: it removes filtered and paged rows from the DOM, so checked
    // participants on other pages would silently not be submitted
    final HCTable aTable = new HCTable (new DTCol ("Select"), new DTCol ("Participant ID"));
    for (final String sURI : aShown)
      aTable.addBodyRow ()
            .addCell (new HCCheckBox ().setName (FIELD_PARTICIPANT).setValue (sURI).setChecked (bPreselect))
            .addCell (new HCCode ().addChild (sURI));
    aForm.addChild (aTable);

    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addHiddenField (CPageParam.PARAM_ACTION, ACTION_CONFIRM);
    aToolbar.addChild (new BootstrapSubmitButton ().addChild ("Continue with the selected participants")
                                                   .setIcon (EDefaultIcon.NEXT)
                                                   .setDisabled (CSMLSync.LOCK.isRunning ()));
    aToolbar.addButton ("Cancel", aWPEC.getSelfHref (), EDefaultIcon.CANCEL);
  }

  /**
   * Show what the selected action would do for the selected participants, and require an explicit
   * confirmation before anything is sent to the SML.
   */
  private void _showConfirm (@NonNull final WebPageExecutionContext aWPEC,
                             @NonNull final ESMLRepairAction eAction,
                             @NonNull final Set <String> aSelectedURIs)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    aNodeList.addChild (getUIHandler ().createActionHeader (eAction.getDisplayName ()));
    _addWarnings (aNodeList, eAction, Integer.toString (aSelectedURIs.size ()));

    final HCUL aUL = new HCUL ();
    int nShown = 0;
    for (final String sURI : aSelectedURIs)
    {
      if (nShown++ >= PREVIEW_COUNT)
        break;
      aUL.addItem (new HCCode ().addChild (sURI));
    }
    if (aSelectedURIs.size () > PREVIEW_COUNT)
      aUL.addItem (em ("... and " + (aSelectedURIs.size () - PREVIEW_COUNT) + " more"));
    aNodeList.addChild (aUL);

    final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormSelf (aWPEC));
    aForm.addChild (new HCHiddenField (PARAM_JOB_ID, aWPEC.params ().getAsStringTrimmed (PARAM_JOB_ID)));
    aForm.addChild (new HCHiddenField (PARAM_REPAIR_ACTION, eAction.getID ()));
    for (final String sURI : aSelectedURIs)
      aForm.addChild (new HCHiddenField (FIELD_PARTICIPANT, sURI));

    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
    aToolbar.addChild (new BootstrapSubmitButton ().addChild ("Yes, perform this on the " +
                                                              aSelectedURIs.size () +
                                                              " selected participant(s)")
                                                   .setIcon (EDefaultIcon.YES)
                                                   .setDisabled (CSMLSync.LOCK.isRunning ()));
    aToolbar.addButton ("Back to the selection",
                        aWPEC.getSelfHref ()
                             .add (PARAM_JOB_ID, aWPEC.params ().getAsStringTrimmed (PARAM_JOB_ID))
                             .add (PARAM_REPAIR_ACTION, eAction.getID ()),
                        EDefaultIcon.BACK);
    aToolbar.addButton ("Cancel", aWPEC.getSelfHref (), EDefaultIcon.CANCEL);
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    if (!canShowPage (aWPEC))
      return;

    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    final File aReportFile = _getReport (aWPEC.params ().getAsStringTrimmed (PARAM_JOB_ID));
    final ESMLRepairAction eAction = ESMLRepairAction.getFromIDOrNull (aWPEC.params ()
                                                                            .getAsStringTrimmed (PARAM_REPAIR_ACTION));

    if (aReportFile != null && eAction != null)
    {
      if (aWPEC.hasAction (CPageParam.ACTION_PERFORM) || aWPEC.hasAction (ACTION_CONFIRM))
      {
        final ICommonsSet <String> aSelectedURIs = _getSelected (aWPEC);
        if (aSelectedURIs.isEmpty ())
          aNodeList.addChild (warn ("No participant was selected."));
        else
          if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
          {
            _startRepair (aWPEC, aReportFile, eAction, aSelectedURIs);
            // Never reached - the action performs a redirect
          }
          else
          {
            _showConfirm (aWPEC, eAction, aSelectedURIs);
            return;
          }
      }
      _showSelection (aWPEC, aReportFile, eAction);
      return;
    }

    aNodeList.addChild (info (div ("Register the participants that are missing at the SML, and resolve the ones the SML has registered for this SMP that do not exist here - either by removing them from the SML or by creating them locally.")).addChild (div ("Both are based on a report created on the ").addChild (em ("Reconcile participants"))
                                                                                                                                                                                                                                                                                                              .addChild (" page, so create a report first and then repair from it.")));

    final boolean bRunning = CSMLSync.LOCK.isRunning ();
    if (bRunning)
    {
      final LocalDateTime aStartDT = CSMLSync.LOCK.getStartDateTime ();
      aNodeList.addChild (warn ("An SML operation is currently running in the background" +
                                (aStartDT == null ? ""
                                                  : " (started at " +
                                                    PDTToString.getAsString (aStartDT, aDisplayLocale) +
                                                    ")") +
                                "."));
    }

    final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
    if (aWPEC.getMenuTree ().containsItemWithID (CMenuSecure.MENU_SML_REG_SYNC))
    {
      aToolbar.addButton ("Reconcile participants",
                          aWPEC.getLinkToMenuItem (CMenuSecure.MENU_SML_REG_SYNC),
                          EDefaultIcon.NEXT);
    }

    // The available reports to repair from
    {
      final ICommonsList <LongRunningJobData> aAllReports = _getAllOfType (SMLSyncJob.JOB_TYPE);

      // One column per action, derived from the enum - a hard coded list would silently drift from
      // the cells below, and DataTables fails if a row has fewer cells than the table has columns
      final ICommonsList <DTCol> aCols = new CommonsArrayList <> ();
      aCols.add (new DTCol ("Report of").setDisplayType (EDTColType.DATETIME, aDisplayLocale)
                                        .setInitialSorting (ESortOrder.DESCENDING));
      aCols.add (new DTCol ("Created by"));
      for (final ESMLRepairAction e : ESMLRepairAction.values ())
        aCols.add (new DTCol (e.getShortName ()));
      final HCTable aTable = new HCTable (aCols).setID (getID () + "reports");
      for (final LongRunningJobData aJobData : aAllReports)
      {
        final File aFile = _getReport (aJobData.getID ());
        if (aFile == null)
          continue;

        // The counts come from the summary of the report, which is a few hundred bytes - counting
        // the lines of the participant lists instead would mean reading the whole report
        SMLSyncResult aSummary = null;
        try
        {
          aSummary = SMLSyncReport.readSummary (aFile);
        }
        catch (final Exception ex)
        {
          LOGGER.warn ("Failed to read the summary of the report '" + aFile.getName () + "': " + ex.getMessage ());
        }

        final HCRow aRow = aTable.addBodyRow ();
        aRow.addCell (PDTToString.getAsString (aJobData.getEndDateTime () != null ? aJobData.getEndDateTime ()
                                                                                  : aJobData.getStartDateTime (),
                                               aDisplayLocale));
        aRow.addCell (SecurityHelper.getUserDisplayName (aJobData.getStartingUserID (), aDisplayLocale));
        for (final ESMLRepairAction e : ESMLRepairAction.values ())
        {
          // Without a readable summary the count is unknown, so the button stays enabled rather
          // than pretending there is nothing to do
          final int nAffected = aSummary == null ? -1 : e.getAffectedCount (aSummary);
          final SMLRepairHistory.Entry aLastRepair = SMLRepairHistory.getLastRepair (aFile.getName (), e);

          final IHCNode aButton = new BootstrapButton (e.isDestructive () ? EBootstrapButtonType.DANGER
                                                                          : EBootstrapButtonType.SECONDARY).addChild (nAffected < 0 ? e.getShortName ()
                                                                                                                                    : e.getShortName () +
                                                                                                                                      " (" +
                                                                                                                                      nAffected +
                                                                                                                                      ")")
                                                                                                           .setIcon (e.isDestructive () ? EDefaultIcon.DELETE
                                                                                                                                        : EDefaultIcon.PLUS)
                                                                                                           .setOnClick (aWPEC.getSelfHref ()
                                                                                                                             .add (PARAM_JOB_ID,
                                                                                                                                   aJobData.getID ())
                                                                                                                             .add (PARAM_REPAIR_ACTION,
                                                                                                                                   e.getID ()))
                                                                                                           .setDisabled (bRunning ||
                                                                                                             nAffected == 0);
          if (aLastRepair == null)
            aRow.addCell (aButton);
          else
          {
            // Explains why the number next to it did not change
            aRow.addCell (aButton,
                          div (new BootstrapBadge (aLastRepair.isComplete () ? EBootstrapBadgeType.SUCCESS
                                                                             : EBootstrapBadgeType.WARNING).addChild ("Repaired at " +
                                                                                                                      PDTToString.getAsString (aLastRepair.getDateTime (),
                                                                                                                                               aDisplayLocale) +
                                                                                                                      ": " +
                                                                                                                      aLastRepair.getSucceededCount () +
                                                                                                                      " done, " +
                                                                                                                      aLastRepair.getAlreadyDoneCount () +
                                                                                                                      " already done, " +
                                                                                                                      aLastRepair.getFailedCount () +
                                                                                                                      " failed")));
          }
        }
      }
      if (aTable.hasBodyRows ())
      {
        aNodeList.addChild (h3 ("Available reconciliation reports"));
        aNodeList.addChild (info (div ("The numbers are the ones of the respective report and therefore do not change when you repair - a report is a snapshot of the moment it was created.")).addChild (div ("Run a new reconciliation to see the current state.")));
        aNodeList.addChild (aTable);
        aNodeList.addChild (BootstrapDataTables.createDefaultDataTables (aWPEC, aTable));
      }
      else
        aNodeList.addChild (warn ("No reconciliation report is available. Please create one first."));
    }

    // The previous repair runs
    {
      final ICommonsList <LongRunningJobData> aAllRepairs = _getAllOfType (SMLRepairJob.JOB_TYPE);
      if (aAllRepairs.isNotEmpty ())
      {
        final HCTable aTable = new HCTable (new DTCol ("Date").setDisplayType (EDTColType.DATETIME, aDisplayLocale)
                                                              .setInitialSorting (ESortOrder.DESCENDING),
                                            new DTCol ("Started by"),
                                            new DTCol ("Result")).setID (getID () + "repairs");
        for (final LongRunningJobData aJobData : aAllRepairs)
        {
          final HCRow aRow = aTable.addBodyRow ();
          aRow.addCell (PDTToString.getAsString (aJobData.getEndDateTime () != null ? aJobData.getEndDateTime ()
                                                                                    : aJobData.getStartDateTime (),
                                                 aDisplayLocale));
          aRow.addCell (SecurityHelper.getUserDisplayName (aJobData.getStartingUserID (), aDisplayLocale));

          final LongRunningJobResult aResult = aJobData.getResult ();
          final String sText = aResult == null ? null : aResult.getResultText ();
          aRow.addCell (StringHelper.isEmpty (sText) ? em ("No details available") : new HCCode ().addChild (sText));
        }
        aNodeList.addChild (h3 ("Previous repairs"));
        aNodeList.addChild (aTable);
        aNodeList.addChild (BootstrapDataTables.createDefaultDataTables (aWPEC, aTable));
      }
    }
  }
}
