/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.ui.ajax;

import javax.annotation.Nonnull;

import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.mime.CMimeType;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.peppol.smpserver.app.CSMPExchange;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.settings.ISMPSettingsManager;
import com.helger.photon.core.ajax.response.AjaxBinaryResponse;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.serialize.MicroWriter;

/**
 * Export all service groups incl. service information and business cards (if
 * enabled) to XML.
 *
 * @author Philip Helger
 */
public final class AjaxExecutorSecureExportAllServiceGroups extends AbstractSMPAjaxExecutor
{

  @Override
  @Nonnull
  protected AjaxBinaryResponse mainHandleRequest (@Nonnull final LayoutExecutionContext aLEC) throws Exception
  {
    final ISMPSettingsManager aSettingsMgr = SMPMetaManager.getSettingsMgr ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final ICommonsList <ISMPServiceGroup> aAllServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();

    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.appendElement (CSMPExchange.ELEMENT_SMP_DATA);
    eRoot.setAttribute (CSMPExchange.ATTR_VERSION, CSMPExchange.VERSION_10);

    // Add all service groups
    for (final ISMPServiceGroup aServiceGroup : aAllServiceGroups)
    {
      final IMicroElement eServiceGroup = eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aServiceGroup,
                                                                                                       CSMPExchange.ELEMENT_SERVICEGROUP));
      final ICommonsList <ISMPServiceInformation> aAllServiceInfos = aServiceInfoMgr.getAllSMPServiceInformationOfServiceGroup (aServiceGroup);
      for (final ISMPServiceInformation aServiceInfo : aAllServiceInfos)
      {
        eServiceGroup.appendChild (MicroTypeConverter.convertToMicroElement (aServiceInfo,
                                                                             CSMPExchange.ELEMENT_SERVICEINFO));
      }
    }

    // Add Business cards only if PD integration is enabled
    if (aSettingsMgr.getSettings ().isPEPPOLDirectoryIntegrationEnabled ())
    {
      // Add all business cards
      final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
      final ICommonsList <ISMPBusinessCard> aAllBusinessCards = aBusinessCardMgr.getAllSMPBusinessCards ();
      for (final ISMPBusinessCard aBusinessCard : aAllBusinessCards)
        eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aBusinessCard, CSMPExchange.ELEMENT_BUSINESSCARD));
    }

    // Build the XML response
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    MicroWriter.writeToStream (aDoc, aBAOS);

    final String sFilename = "smp-data-" + PDTIOHelper.getCurrentLocalDateTimeForFilename () + ".xml";
    return new AjaxBinaryResponse (aBAOS.toByteArray (), CMimeType.APPLICATION_XML, sFilename);
  }
}
