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
package com.helger.peppol.smpserver.rest2;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.mime.CMimeType;
import com.helger.pd.businesscard.v3.PD3BusinessCardMarshaller;
import com.helger.pd.businesscard.v3.PD3BusinessCardType;
import com.helger.peppol.smpserver.app.SMPWebAppConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.restapi.BusinessCardServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.core.api.IAPIDescriptor;
import com.helger.photon.core.api.IAPIExecutor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.XMLWriterSettings;

public final class APIExecutorBusinessCardGet implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorBusinessCardGet.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    if (!SMPMetaManager.getSettings ().isPEPPOLDirectoryIntegrationEnabled ())
    {
      // PD integration is disabled
      LOGGER.warn ("The " +
                   SMPWebAppConfiguration.getDirectoryName () +
                   " integration is disabled. getBusinessCard will not be executed.");
      aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_FOUND);
    }
    else
    {
      final String sServiceGroupID = aPathVariables.get (Rest2Filter.PARAM_SERVICE_GROUP_ID);
      final ISMPServerAPIDataProvider aDataProvider = new Rest2DataProvider (aRequestScope);
      /*
       * getBusinessCard throws an exception if non is found
       */
      final PD3BusinessCardType ret = new BusinessCardServerAPI (aDataProvider).getBusinessCard (sServiceGroupID);
      final byte [] aBytes = new PD3BusinessCardMarshaller ().getAsBytes (ret);

      aUnifiedResponse.setContent (aBytes)
                      .setMimeType (CMimeType.TEXT_XML)
                      .setCharset (XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ);
    }
  }
}
