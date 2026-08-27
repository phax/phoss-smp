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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.format.PDTToString;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.http.CHttp;
import com.helger.mime.CMimeType;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exchange.ServiceGroupExport;
import com.helger.phoss.smp.exchange.ServiceGroupExportLock;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.phoss.smp.ui.ajax.AbstractSMPAjaxExecutor;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Class to export service groups with all contents
 *
 * @author Philip Helger
 */
public final class PageSecureServiceGroupExport extends AbstractSMPWebPage
{
  private static final IAjaxFunctionDeclaration AJAX_EXPORT_SG;

  static
  {
    // Ensure it can only be accessed by logged in users
    AJAX_EXPORT_SG = CAjax.addAjaxWithLogin (new AbstractSMPAjaxExecutor ()
    {
      @Override
      protected void mainHandleRequest (@NonNull final LayoutExecutionContext aLEC,
                                        @NonNull final PhotonUnifiedResponse aAjaxResponse) throws Exception
      {
        // Only a single export may run at a time, because exporting everything is expensive
        if (!ServiceGroupExportLock.tryAcquire (aLEC.getLoggedInUserID ()))
        {
          aAjaxResponse.setStatus (CHttp.HTTP_SERVICE_UNAVAILABLE);
          aAjaxResponse.setAllowContentOnStatusCode (true)
                       .setContentAndCharset ("Another Service Group export is already running. Please try again later.",
                                              StandardCharsets.UTF_8)
                       .setMimeType (CMimeType.TEXT_PLAIN);
          return;
        }

        try
        {
          final ISMPSettings aSettings = SMPMetaManager.getSettings ();
          final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
          final ICommonsList <ISMPServiceGroup> aAllServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();
          final boolean bExportBusinessCards = aSettings.isDirectoryIntegrationEnabled ();

          // Stream the export, so that never more than a single Service Group is kept in memory
          try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
          {
            ServiceGroupExport.createExportDataXMLVer10 (aAllServiceGroups, bExportBusinessCards, aBAOS);

            // Build the XML response
            aAjaxResponse.setContent (aBAOS);
            aAjaxResponse.setCharset (ServiceGroupExport.XML_WRITER_SETTINGS.getCharset ());
            aAjaxResponse.setMimeType (ServiceGroupExport.getExportMimeType ());
            aAjaxResponse.attachment ("phoss-smp-export-" +
                                      PDTIOHelper.getCurrentLocalDateTimeForFilename () +
                                      ".xml");
          }
        }
        finally
        {
          ServiceGroupExportLock.release ();
        }
      }
    });
  }

  public PageSecureServiceGroupExport (@NonNull @Nonempty final String sID)
  {
    super (sID, "Export");
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final long nServiceGroupCount = aServiceGroupMgr.getSMPServiceGroupCount ();

    final boolean bExportBusinessCards = aSettings.isDirectoryIntegrationEnabled ();

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
                                  " to an XML file."));
      }

    // Only a single export may run at a time
    final boolean bExportRunning = ServiceGroupExportLock.isExportRunning ();
    if (bExportRunning)
    {
      final LocalDateTime aStartDT = ServiceGroupExportLock.getExportStartDateTime ();
      aNodeList.addChild (warn ("An export is currently running in the background" +
                                (aStartDT == null ? "" : " (started at " +
                                                         PDTToString.getAsString (aStartDT,
                                                                                  aWPEC.getDisplayLocale ()) +
                                                         ")") +
                                ". Please wait until it is finished before starting a new one."));
    }

    // The main export logic happens in the AJAX handler
    final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addChild (new BootstrapButton ().addChild ("Export all Service Groups")
                                             .setIcon (EDefaultIcon.SAVE_ALL)
                                             .setOnClick (AJAX_EXPORT_SG.getInvocationURL (aRequestScope))
                                             .setDisabled (nServiceGroupCount <= 0 || bExportRunning));
    aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
  }
}
