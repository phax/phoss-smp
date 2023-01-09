/*
 * Copyright (C) 2014-2023 Philip Helger and contributors
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

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exchange.ServiceGroupExport;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.phoss.smp.ui.ajax.AbstractSMPAjaxExecutor;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;

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
      protected void mainHandleRequest (@Nonnull final LayoutExecutionContext aLEC,
                                        @Nonnull final PhotonUnifiedResponse aAjaxResponse) throws Exception
      {
        final ISMPSettings aSettings = SMPMetaManager.getSettings ();
        final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
        final ICommonsList <ISMPServiceGroup> aAllServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();

        final IMicroDocument aDoc = ServiceGroupExport.createExportDataXMLVer10 (aAllServiceGroups,
                                                                                 aSettings.isDirectoryIntegrationEnabled ());

        // Build the XML response
        aAjaxResponse.xml (aDoc);
        aAjaxResponse.attachment ("smp-data-" + PDTIOHelper.getCurrentLocalDateTimeForFilename () + ".xml");
      }
    });
  }

  public PageSecureServiceGroupExport (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Export");
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final long nServiceGroupCount = aServiceGroupMgr.getSMPServiceGroupCount ();

    final boolean bHandleBusinessCards = aSettings.isDirectoryIntegrationEnabled ();

    if (nServiceGroupCount < 0)
      aNodeList.addChild (error ("The number of service groups is unknown, hence nothing can be exported!"));
    else
      if (nServiceGroupCount == 0)
        aNodeList.addChild (warn ("Since no service group is present, nothing can be exported!"));
      else
      {
        aNodeList.addChild (info ("Export " +
                                  (nServiceGroupCount == 1 ? "service group"
                                                           : "all " + nServiceGroupCount + " service groups") +
                                  (bHandleBusinessCards ? " and business card" + (nServiceGroupCount == 1 ? "" : "s")
                                                        : "") +
                                  " to an XML file."));
      }

    // The main export logic happens in the AJAX handler
    final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addChild (new BootstrapButton ().addChild ("Export all Service Groups")
                                             .setIcon (EDefaultIcon.SAVE_ALL)
                                             .setOnClick (AJAX_EXPORT_SG.getInvocationURL (aRequestScope))
                                             .setDisabled (nServiceGroupCount <= 0));
  }
}
