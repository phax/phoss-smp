/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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
import com.helger.phoss.smp.app.CSMPExchange;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardMicroTypeConverter;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.convert.MicroTypeConverter;

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
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    final ICommonsList <ISMPServiceGroup> aAllServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();

    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.appendElement (CSMPExchange.ELEMENT_SMP_DATA);
    eRoot.setAttribute (CSMPExchange.ATTR_VERSION, CSMPExchange.VERSION_10);

    // Add all service groups
    for (final ISMPServiceGroup aServiceGroup : aAllServiceGroups.getSortedInline (ISMPServiceGroup.comparator ()))
    {
      final IMicroElement eServiceGroup = eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aServiceGroup,
                                                                                                       CSMPExchange.ELEMENT_SERVICEGROUP));

      // Add all service information
      final ICommonsList <ISMPServiceInformation> aAllServiceInfos = aServiceInfoMgr.getAllSMPServiceInformationOfServiceGroup (aServiceGroup);
      for (final ISMPServiceInformation aServiceInfo : aAllServiceInfos.getSortedInline (ISMPServiceInformation.comparator ()))
      {
        eServiceGroup.appendChild (MicroTypeConverter.convertToMicroElement (aServiceInfo,
                                                                             CSMPExchange.ELEMENT_SERVICEINFO));
      }

      // Add all redirects
      final ICommonsList <ISMPRedirect> aAllRedirects = aRedirectMgr.getAllSMPRedirectsOfServiceGroup (aServiceGroup);
      for (final ISMPRedirect aServiceInfo : aAllRedirects.getSortedInline (ISMPRedirect.comparator ()))
      {
        eServiceGroup.appendChild (MicroTypeConverter.convertToMicroElement (aServiceInfo,
                                                                             CSMPExchange.ELEMENT_REDIRECT));
      }
    }

    // Add Business cards only if PD integration is enabled
    if (aSettingsMgr.getSettings ().isPEPPOLDirectoryIntegrationEnabled ())
    {
      // Add all business cards
      final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
      final ICommonsList <ISMPBusinessCard> aAllBusinessCards = aBusinessCardMgr.getAllSMPBusinessCards ();
      for (final ISMPBusinessCard aBusinessCard : aAllBusinessCards.getSortedInline (ISMPBusinessCard.comparator ()))
        eRoot.appendChild (SMPBusinessCardMicroTypeConverter.convertToMicroElement (aBusinessCard,
                                                                                    null,
                                                                                    CSMPExchange.ELEMENT_BUSINESSCARD,
                                                                                    true));
    }

    // Build the XML response
    aAjaxResponse.xml (aDoc);
    aAjaxResponse.attachment ("smp-data-" + PDTIOHelper.getCurrentLocalDateTimeForFilename () + ".xml");
  }
}
