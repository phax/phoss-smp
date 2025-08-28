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
import com.helger.mime.CMimeType;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.exception.SMPInternalErrorException;
import com.helger.phoss.smp.restapi.BDXR1ServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.phoss.smp.restapi.SMPServerAPI;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.bdxr1.marshal.BDXR1MarshallerServiceGroupReferenceListType;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceGroupReferenceListType;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nonnull;

public final class APIExecutorUserListGet extends AbstractSMPAPIExecutor
{
  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathUserID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_USER_ID));

    // No service group available
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());

    final byte [] aBytes;
    switch (SMPServerConfiguration.getRESTType ())
    {
      case PEPPOL:
      {
        // Unspecified extension
        final com.helger.xsds.peppol.smp1.ServiceGroupReferenceListType ret = new SMPServerAPI (aDataProvider).getServiceGroupReferenceList (sPathUserID,
                                                                                                                                             aCredentials);
        aBytes = new SMPMarshallerServiceGroupReferenceListType ().setUseSchema (XML_SCHEMA_VALIDATION)
                                                                  .getAsBytes (ret);
        break;
      }
      case OASIS_BDXR_V1:
      {
        // Unspecified extension
        final com.helger.xsds.bdxr.smp1.ServiceGroupReferenceListType ret = new BDXR1ServerAPI (aDataProvider).getServiceGroupReferenceList (sPathUserID,
                                                                                                                                             aCredentials);
        aBytes = new BDXR1MarshallerServiceGroupReferenceListType ().setUseSchema (XML_SCHEMA_VALIDATION)
                                                                    .getAsBytes (ret);
        break;
      }
      // Not available in OASIS BDXR v2
      default:
        throw new UnsupportedOperationException ("Unsupported REST type specified!");
    }

    if (aBytes == null)
    {
      // Internal error serializing the payload
      throw new SMPInternalErrorException ("Failed to convert the returned CompleteServiceGroup to XML");
    }

    aUnifiedResponse.setContent (aBytes).setMimeType (CMimeType.TEXT_XML);
  }
}
