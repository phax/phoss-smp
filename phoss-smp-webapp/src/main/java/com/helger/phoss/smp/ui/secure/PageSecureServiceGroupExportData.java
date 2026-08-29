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
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.exchange.ServiceGroupExport;
import com.helger.phoss.smp.exchange.ServiceGroupExportJob;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.phoss.smp.ui.ajax.AbstractSMPAjaxExecutor;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.mgrs.PhotonBasicManager;
import com.helger.photon.mgrs.longrun.ELongRunningJobResultType;
import com.helger.photon.mgrs.longrun.LongRunningJobData;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Class to show and download the previously created Service Group exports.
 *
 * @author Philip Helger
 */
public final class PageSecureServiceGroupExportData extends AbstractSMPWebPage
{
  /**
   * The ID of the long running job that created the export. Deliberately not a filename - see
   * {@link #_getDownloadableExportFile(String)}.
   */
  private static final String PARAM_JOB_ID = "jobid";

  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureServiceGroupExportData.class);

  private static final IAjaxFunctionDeclaration AJAX_DOWNLOAD_EXPORT;

  /**
   * Resolve the export file to be downloaded.<br>
   * The client only provides the ID of the long running job that created the export - never a
   * filename or a path. The ID is resolved via the long running job result manager, and the file
   * that is stored there must additionally pass
   * {@link ServiceGroupExportJob#getValidExportFile(File)}, which only accepts existing files
   * located directly in the export directory whose name follows the export file naming. Therefore
   * no other file of the file system can be downloaded through this page.
   *
   * @param sJobID
   *        The long running job ID provided by the client. May be <code>null</code>.
   * @return <code>null</code> if no matching downloadable export file exists.
   */
  @Nullable
  private static File _getDownloadableExportFile (@Nullable final String sJobID)
  {
    if (StringHelper.isEmpty (sJobID))
      return null;

    final LongRunningJobData aJobData = PhotonBasicManager.getLongRunningJobResultMgr ().getJobResultOfID (sJobID);
    if (aJobData == null)
      return null;

    // Only a Service Group export may be downloaded here
    if (!ServiceGroupExportJob.JOB_TYPE.equals (aJobData.getJobType ()))
      return null;

    final LongRunningJobResult aResult = aJobData.getResult ();
    if (aResult == null || aResult.getType () != ELongRunningJobResultType.FILE)
      return null;

    return ServiceGroupExportJob.getValidExportFile (aResult.getResultFile ());
  }

  static
  {
    // Ensure it can only be accessed by logged in users
    AJAX_DOWNLOAD_EXPORT = CAjax.addAjaxWithLogin (new AbstractSMPAjaxExecutor ()
    {
      @Override
      protected void mainHandleRequest (@NonNull final LayoutExecutionContext aLEC,
                                        @NonNull final PhotonUnifiedResponse aAjaxResponse) throws Exception
      {
        final String sJobID = aLEC.params ().getAsStringTrimmed (PARAM_JOB_ID);
        final File aFile = _getDownloadableExportFile (sJobID);
        if (aFile == null)
        {
          LOGGER.warn ("Failed to resolve a downloadable Service Group export for job ID '" + sJobID + "'");
          aAjaxResponse.setStatus (CHttp.HTTP_NOT_FOUND);
          return;
        }

        LOGGER.info ("Downloading the Service Group export '" + aFile.getAbsolutePath () + "'");
        aAjaxResponse.setContent (new FileSystemResource (aFile));
        aAjaxResponse.setCharset (ServiceGroupExport.XML_WRITER_SETTINGS.getCharset ());
        aAjaxResponse.setMimeType (ServiceGroupExport.getExportMimeType ());
        aAjaxResponse.attachment (aFile.getName ());
      }
    });
  }

  public PageSecureServiceGroupExportData (@NonNull @Nonempty final String sID)
  {
    super (sID, "Export data");
  }

  /**
   * @return All long running job results that refer to an existing, downloadable export file, the
   *         newest one first. Never <code>null</code>.
   */
  @NonNull
  private static ICommonsList <LongRunningJobData> _getAllDownloadableExports ()
  {
    final ICommonsList <LongRunningJobData> ret = new CommonsArrayList <> ();
    // Only the results of the Service Group export job - other long running jobs are none of this
    // page's business
    PhotonBasicManager.getLongRunningJobResultMgr ().forEachJobResult (ServiceGroupExportJob.JOB_TYPE, aJobData -> {
      final LongRunningJobResult aResult = aJobData.getResult ();
      if (aResult != null &&
          aResult.getType () == ELongRunningJobResultType.FILE &&
          ServiceGroupExportJob.getValidExportFile (aResult.getResultFile ()) != null)
      {
        ret.add (aJobData);
      }
    });
    return ret;
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final SizeHelper aSH = SizeHelper.getSizeHelperOfLocale (aDisplayLocale);

    final ICommonsList <LongRunningJobData> aAllExports = _getAllDownloadableExports ();

    final int nRetentionDays = SMPServerConfiguration.getExportRetentionDays ();
    if (nRetentionDays > 0)
    {
      aNodeList.addChild (info ("Created exports are stored on the server and are automatically deleted after " +
                                nRetentionDays +
                                " days."));
    }
    else
      aNodeList.addChild (info ("Created exports are stored on the server and are never deleted automatically."));

    if (aAllExports.isEmpty ())
      aNodeList.addChild (warn ("No Service Group export is currently available for download."));

    final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
    if (aWPEC.getMenuTree ().containsItemWithID (CMenuSecure.MENU_SERVICE_GROUPS_EXPORT))
    {
      aToolbar.addButton ("Create a new export",
                          aWPEC.getLinkToMenuItem (CMenuSecure.MENU_SERVICE_GROUPS_EXPORT),
                          EDefaultIcon.NEXT);
    }

    final HCTable aTable = new HCTable (new DTCol ("Created").setDisplayType (EDTColType.DATETIME, aDisplayLocale)
                                                             .setInitialSorting (ESortOrder.DESCENDING),
                                        new DTCol ("Started by"),
                                        new DTCol ("File name"),
                                        new DTCol ("Size").setDisplayType (EDTColType.INT, aDisplayLocale),
                                        new DTCol ("Download")).setID (getID ());
    for (final LongRunningJobData aJobData : aAllExports)
    {
      // Never null - it was checked in _getAllDownloadableExports
      final File aFile = ServiceGroupExportJob.getValidExportFile (aJobData.getResult ().getResultFile ());

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (PDTToString.getAsString (aJobData.getEndDateTime () != null ? aJobData.getEndDateTime ()
                                                                               : aJobData.getStartDateTime (),
                                             aDisplayLocale));
      aRow.addCell (SecurityHelper.getUserDisplayName (aJobData.getStartingUserID (), aDisplayLocale));
      aRow.addCell (aFile.getName ());
      aRow.addCell (aSH.getAsMatching (aFile.length (), 2));
      aRow.addCell (new BootstrapButton ().addChild ("Download")
                                          .setIcon (EDefaultIcon.SAVE)
                                          .setOnClick (AJAX_DOWNLOAD_EXPORT.getInvocationURL (aRequestScope)
                                                                           .add (PARAM_JOB_ID, aJobData.getID ())));
    }
    aNodeList.addChild (aTable);

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aDataTables);
  }
}
