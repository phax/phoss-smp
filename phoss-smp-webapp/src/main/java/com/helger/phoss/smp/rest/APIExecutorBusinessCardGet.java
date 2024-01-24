/*
 * Copyright (C) 2014-2024 Philip Helger and contributors
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

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.mime.CMimeType;
import com.helger.peppol.businesscard.v3.PD3BusinessCardMarshaller;
import com.helger.peppol.businesscard.v3.PD3BusinessCardType;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.BusinessCardServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.XMLWriterSettings;

public final class APIExecutorBusinessCardGet extends AbstractSMPAPIExecutor
{
  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sServiceGroupID = aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID);
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, sServiceGroupID);

    if (!SMPMetaManager.getSettings ().isDirectoryIntegrationEnabled ())
    {
      // PD integration is disabled
      throw new SMPPreconditionFailedException ("The " +
                                                SMPWebAppConfiguration.getDirectoryName () +
                                                " integration is disabled. getBusinessCard will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // getBusinessCard throws an exception if non is found
    final PD3BusinessCardType ret = new BusinessCardServerAPI (aDataProvider).getBusinessCard (sServiceGroupID);
    final byte [] aBytes = new PD3BusinessCardMarshaller ().getAsBytes (ret);

    aUnifiedResponse.setContent (aBytes).setMimeType (CMimeType.TEXT_XML).setCharset (XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ);
  }
}
