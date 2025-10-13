/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.BDXR1ServerAPI;
import com.helger.phoss.smp.restapi.BDXR2ServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.phoss.smp.restapi.SMPServerAPI;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nonnull;

public final class APIExecutorServiceGroupDelete extends AbstractSMPAPIExecutor
{
  public static final String PARAM_DELETE_IN_SML = "delete-in-sml";

  @Override
  protected void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                            @Nonnull @Nonempty final String sPath,
                            @Nonnull final Map <String, String> aPathVariables,
                            @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                            @Nonnull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. deleteServiceGroup will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());
    final boolean bDeleteInSML = !"false".equalsIgnoreCase (aRequestScope.params ().getAsString (PARAM_DELETE_IN_SML));

    switch (SMPServerConfiguration.getRESTType ())
    {
      case PEPPOL:
        new SMPServerAPI (aDataProvider).deleteServiceGroup (sPathServiceGroupID, bDeleteInSML, aCredentials);
        break;
      case OASIS_BDXR_V1:
        new BDXR1ServerAPI (aDataProvider).deleteServiceGroup (sPathServiceGroupID, bDeleteInSML, aCredentials);
        break;
      case OASIS_BDXR_V2:
        new BDXR2ServerAPI (aDataProvider).deleteServiceGroup (sPathServiceGroupID, bDeleteInSML, aCredentials);
        break;
      default:
        throw new UnsupportedOperationException ("Unsupported REST type specified!");
    }
    aUnifiedResponse.createOk ();
  }
}
