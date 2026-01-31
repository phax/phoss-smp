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
package com.helger.phoss.smp.rest;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exchange.ServiceGroupExport;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;

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

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sLogPrefix = "[REST API Export-All-XML-V1] ";
    LOGGER.info (sLogPrefix + "Starting Export");

    // Only authenticated user may do so
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());
    SMPUserManagerPhoton.validateUserCredentials (aCredentials);

    // Start action after authentication
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();

    // Now get all relevant service groups
    final ICommonsList <ISMPServiceGroup> aAllServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();

    final boolean bIncludeBusinessCards = aRequestScope.params ()
                                                       .getAsBoolean (PARAM_INCLUDE_BUSINESS_CARDS,
                                                                      aSettings.isDirectoryIntegrationEnabled ());
    final IMicroDocument aDoc = ServiceGroupExport.createExportDataXMLVer10 (aAllServiceGroups, bIncludeBusinessCards);

    LOGGER.info (sLogPrefix + "Finished creating Export data");

    // Build the XML response
    aUnifiedResponse.xml (aDoc).disableCaching ();
  }
}
