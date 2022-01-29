/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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
package com.helger.phoss.smp.ui.ajax;

import javax.annotation.Nonnull;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exchange.ServiceGroupExport;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.xml.microdom.IMicroDocument;

/**
 * Export all service groups incl. service information and business cards (if
 * enabled) to XML.
 *
 * @author Philip Helger
 */
public final class AjaxExecutorSecureExportAllServiceGroups extends AbstractSMPAjaxExecutor
{
  @Override
  protected void mainHandleRequest (@Nonnull final LayoutExecutionContext aLEC,
                                    @Nonnull final PhotonUnifiedResponse aAjaxResponse) throws Exception
  {
    final ISMPSettingsManager aSettingsMgr = SMPMetaManager.getSettingsMgr ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ICommonsList <ISMPServiceGroup> aAllServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();

    final IMicroDocument aDoc = ServiceGroupExport.createExportData (aAllServiceGroups,
                                                                     aSettingsMgr.getSettings ().isDirectoryIntegrationEnabled ());

    // Build the XML response
    aAjaxResponse.xml (aDoc);
    aAjaxResponse.attachment ("smp-data-" + PDTIOHelper.getCurrentLocalDateTimeForFilename () + ".xml");
  }
}
