/**
 * Copyright (C) 2014-2020 Philip Helger and contributors
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

import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.CHttp;
import com.helger.commons.mime.CMimeType;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.restapi.BDXR1ServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPServerAPI;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.bdxr1.marshal.BDXR1MarshallerServiceGroupReferenceListType;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceGroupReferenceListType;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class APIExecutorListGet extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorListGet.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sUserID = aPathVariables.get (Rest2Filter.PARAM_USER_ID);
    // No service group available
    final ISMPServerAPIDataProvider aDataProvider = new Rest2DataProvider (aRequestScope, null);
    final BasicAuthClientCredentials aBasicAuth = Rest2RequestHelper.getMandatoryAuth (aRequestScope.headers ());

    final byte [] aBytes;
    switch (SMPServerConfiguration.getRESTType ())
    {
      case PEPPOL:
      {
        // Unspecified extension
        final com.helger.xsds.peppol.smp1.ServiceGroupReferenceListType ret = new SMPServerAPI (aDataProvider).getServiceGroupReferenceList (sUserID,
                                                                                                                                                  aBasicAuth);
        aBytes = new SMPMarshallerServiceGroupReferenceListType (XML_SCHEMA_VALIDATION).getAsBytes (ret);
        break;
      }
      case OASIS_BDXR_V1:
      {
        // Unspecified extension
        final com.helger.xsds.bdxr.smp1.ServiceGroupReferenceListType ret = new BDXR1ServerAPI (aDataProvider).getServiceGroupReferenceList (sUserID,
                                                                                                                                             aBasicAuth);
        aBytes = new BDXR1MarshallerServiceGroupReferenceListType (XML_SCHEMA_VALIDATION).getAsBytes (ret);
        break;
      }
      default:
        throw new UnsupportedOperationException ("Unsupported REST type specified!");
    }

    if (aBytes == null)
    {
      // Internal error serializing the payload
      LOGGER.warn ("Failed to convert the returned CompleteServiceGroup to XML");
      aUnifiedResponse.setStatus (CHttp.HTTP_INTERNAL_SERVER_ERROR);
    }
    else
    {
      aUnifiedResponse.setContent (aBytes).setMimeType (CMimeType.TEXT_XML);
    }
  }
}
