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
package com.helger.phoss.smp.rest;

import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exchange.ServiceGroupExport;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.write.IXMLWriterSettings;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * REST API to export all Service Groups into XML v1
 *
 * @author Philip Helger
 * @since 5.6.0
 */
public final class APIExecutorExportAllXMLVer1 extends AbstractSMPAPIExecutor
{
  public static final String PARAM_INCLUDE_BUSINESS_CARDS = "include-business-cards";

  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorExportAllXMLVer1.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sLogPrefix = "[REST API Export-All-XML-V1] ";
    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLogPrefix + "Starting Export");

    // Only authenticated user may do so
    final BasicAuthClientCredentials aBasicAuth = getMandatoryAuth (aRequestScope.headers ());
    SMPUserManagerPhoton.validateUserCredentials (aBasicAuth);

    // Start action after authentication
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();

    // Now get all relevant service groups
    final ICommonsList <ISMPServiceGroup> aAllServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();

    final boolean bIncludeBusinessCards = aRequestScope.params ()
                                                       .getAsBoolean (PARAM_INCLUDE_BUSINESS_CARDS,
                                                                      aSettings.isDirectoryIntegrationEnabled ());
    final IMicroDocument aDoc = ServiceGroupExport.createExportDataXMLVer10 (aAllServiceGroups, bIncludeBusinessCards);

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLogPrefix + "Finished creating Export data");

    // Build the XML response
    final IXMLWriterSettings aXWS = new XMLWriterSettings ();
    aUnifiedResponse.setContentAndCharset (MicroWriter.getNodeAsString (aDoc, aXWS), aXWS.getCharset ())
                    .setMimeType (new MimeType (CMimeType.APPLICATION_XML).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                         aXWS.getCharset ().name ()))
                    .disableCaching ();
  }
}
