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

import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exchange.ServiceGroupExportJob;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.pages.BootstrapPagesMenuConfigurator;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageText;
import com.helger.photon.uicore.page.WebPageExecutionContext;

/**
 * Class to export service groups with all contents
 *
 * @author Philip Helger
 */
public final class PageSecureServiceGroupExport extends AbstractSMPWebPage
{
  private static final String ACTION_START_EXPORT = "start-export";

  public PageSecureServiceGroupExport (@NonNull @Nonempty final String sID)
  {
    super (sID, "Export");
  }

  private void _startExport (@NonNull final WebPageExecutionContext aWPEC, final boolean bExportBusinessCards)
  {
    // Only a single export may run at a time, because exporting everything is expensive
    if (!ServiceGroupExportJob.LOCK.tryAcquire (aWPEC.getLoggedInUserID ()))
    {
      aWPEC.postRedirectGetInternal (warn ("Another Service Group export is already running in the background. Please wait until it is finished."));
    }
    else
    {
      try
      {
        PhotonWorkerPool.getInstance ()
                        .run (ServiceGroupExportJob.JOB_ID,
                              new ServiceGroupExportJob (bExportBusinessCards, aWPEC.getLoggedInUserID ()));
      }
      catch (final RuntimeException ex)
      {
        // The job was never started, so it can never release the lock
        ServiceGroupExportJob.LOCK.release ();
        throw ex;
      }

      aWPEC.postRedirectGetInternal (success ("The export of all Service Groups is now running in the background. " +
                                              "The created file is shown on the \"" +
                                              EWebPageText.PAGE_NAME_APPINFO_LONG_RUNNING_JOBS.getDisplayText (aWPEC.getDisplayLocale ()) +
                                              "\" page as soon as it is finished."));
    }
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final long nServiceGroupCount = aServiceGroupMgr.getSMPServiceGroupCount ();

    final boolean bExportBusinessCards = aSettings.isDirectoryIntegrationEnabled ();

    if (aWPEC.hasAction (ACTION_START_EXPORT) && nServiceGroupCount > 0)
      _startExport (aWPEC, bExportBusinessCards);

    if (nServiceGroupCount < 0)
      aNodeList.addChild (error ("The number of service groups is unknown, hence nothing can be exported!"));
    else
      if (nServiceGroupCount == 0)
        aNodeList.addChild (warn ("Since no service group is present, nothing can be exported!"));
      else
      {
        aNodeList.addChild (info ("Export " +
                                  (nServiceGroupCount == 1 ? "service group" : "all " +
                                                                               nServiceGroupCount +
                                                                               " service groups") +
                                  (bExportBusinessCards ? " and business card" + (nServiceGroupCount == 1 ? "" : "s")
                                                        : "") +
                                  " to an XML file. The export runs in the background and the created file is stored on the server."));
      }

    // Only a single export may run at a time
    final boolean bExportRunning = ServiceGroupExportJob.LOCK.isRunning ();
    if (bExportRunning)
    {
      final LocalDateTime aStartDT = ServiceGroupExportJob.LOCK.getStartDateTime ();
      aNodeList.addChild (warn ("An export is currently running in the background" +
                                (aStartDT == null ? "" : " (started at " +
                                                         PDTToString.getAsString (aStartDT,
                                                                                  aWPEC.getDisplayLocale ()) +
                                                         ")") +
                                ". Please wait until it is finished before starting a new one."));
    }

    // The main export logic happens in the background job
    final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addChild (new BootstrapButton ().addChild ("Export all Service Groups")
                                             .setIcon (EDefaultIcon.SAVE_ALL)
                                             .setOnClick (aWPEC.getSelfHref ()
                                                               .add (CPageParam.PARAM_ACTION, ACTION_START_EXPORT))
                                             .setDisabled (nServiceGroupCount <= 0 || bExportRunning));
    aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
    if (aWPEC.getMenuTree ()
             .containsItemWithID (BootstrapPagesMenuConfigurator.MENU_ADMIN_APPINFO_LONG_RUNNING_JOBS))
    {
      aToolbar.addButton (EWebPageText.PAGE_NAME_APPINFO_LONG_RUNNING_JOBS.getDisplayText (aWPEC.getDisplayLocale ()),
                          aWPEC.getLinkToMenuItem (BootstrapPagesMenuConfigurator.MENU_ADMIN_APPINFO_LONG_RUNNING_JOBS),
                          EDefaultIcon.NEXT);
    }
  }
}
