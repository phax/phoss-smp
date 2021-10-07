/*
 * Copyright (C) 2014-2021 Philip Helger and contributors
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
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
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
                                  (nServiceGroupCount == 1 ? "service group" : "all " + nServiceGroupCount + " service groups") +
                                  (bHandleBusinessCards ? " and business card" + (nServiceGroupCount == 1 ? "" : "s") : "") +
                                  " to an XML file."));
      }

    final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addChild (new BootstrapButton ().addChild ("Export all Service Groups")
                                             .setIcon (EDefaultIcon.SAVE_ALL)
                                             .setOnClick (CAjax.FUNCTION_EXPORT_ALL_SERVICE_GROUPS.getInvocationURL (aRequestScope))
                                             .setDisabled (nServiceGroupCount <= 0));
  }
}
