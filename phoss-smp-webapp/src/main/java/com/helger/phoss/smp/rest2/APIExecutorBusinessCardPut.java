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
package com.helger.phoss.smp.rest2;

import java.nio.charset.Charset;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.helper.PDBusinessCardHelper;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.restapi.BusinessCardServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.core.api.IAPIDescriptor;
import com.helger.photon.core.api.IAPIExecutor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class APIExecutorBusinessCardPut implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorBusinessCardPut.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. saveBusinessCard will not be executed.");
      aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_FOUND);
    }
    else
      if (!SMPMetaManager.getSettings ().isPEPPOLDirectoryIntegrationEnabled ())
      {
        // PD integration is disabled
        LOGGER.warn ("The " +
                     SMPWebAppConfiguration.getDirectoryName () +
                     " integration is disabled. saveBusinessCard will not be executed.");
        aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_FOUND);
      }
      else
      {
        // Parse main payload
        final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
        final PDBusinessCard aBC = PDBusinessCardHelper.parseBusinessCard (aPayload, (Charset) null);
        if (aBC == null)
        {
          // Cannot parse
          LOGGER.warn ("Failed to parse XML payload as BusinessCard.");
          aUnifiedResponse.setStatus (HttpServletResponse.SC_BAD_REQUEST);
        }
        else
        {
          final String sServiceGroupID = aPathVariables.get (Rest2Filter.PARAM_SERVICE_GROUP_ID);
          final ISMPServerAPIDataProvider aDataProvider = new Rest2DataProvider (aRequestScope);
          final BasicAuthClientCredentials aBasicAuth = Rest2RequestHelper.getAuth (aRequestScope.headers ());

          final ESuccess eSuccess = new BusinessCardServerAPI (aDataProvider).createBusinessCard (sServiceGroupID,
                                                                                                  aBC,
                                                                                                  aBasicAuth);
          if (eSuccess.isFailure ())
            aUnifiedResponse.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          else
            aUnifiedResponse.setStatus (HttpServletResponse.SC_OK);
        }
      }
  }
}
