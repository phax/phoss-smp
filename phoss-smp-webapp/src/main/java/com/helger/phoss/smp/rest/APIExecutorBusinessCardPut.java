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

import java.nio.charset.Charset;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.helper.PDBusinessCardHelper;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.BusinessCardServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class APIExecutorBusinessCardPut extends AbstractSMPAPIExecutor
{
  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. saveBusinessCard will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Check credentials first
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());

    if (!SMPMetaManager.getSettings ().isDirectoryIntegrationEnabled ())
    {
      // PD integration is disabled
      throw new SMPPreconditionFailedException ("The " +
                                                SMPWebAppConfiguration.getDirectoryName () +
                                                " integration is disabled. saveBusinessCard will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Parse main payload
    final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    final PDBusinessCard aBC = PDBusinessCardHelper.parseBusinessCard (aPayload, (Charset) null);
    if (aBC == null)
    {
      // Cannot parse
      throw new SMPBadRequestException ("Failed to parse XML payload as BusinessCard.", aDataProvider.getCurrentURI ());
    }

    final ESuccess eSuccess = new BusinessCardServerAPI (aDataProvider).createBusinessCard (sServiceGroupID,
                                                                                            aBC,
                                                                                            aCredentials);
    if (eSuccess.isFailure ())
      aUnifiedResponse.createInternalServerError ();
    else
      aUnifiedResponse.createOk ();
  }
}
