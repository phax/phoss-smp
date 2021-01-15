/**
 * Copyright (C) 2014-2021 Philip Helger and contributors
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
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.CHttp;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.restapi.BDXR1ServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPServerAPI;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.bdxr1.marshal.BDXR1MarshallerServiceGroupType;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceGroupType;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.read.DOMReader;

public final class APIExecutorServiceGroupPut extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorServiceGroupPut.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. saveServiceGroup will not be executed.");
      aUnifiedResponse.setStatus (CHttp.HTTP_NOT_FOUND);
    }
    else
    {
      // Parse main payload
      final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
      final Document aServiceGroupDoc = DOMReader.readXMLDOM (aPayload);
      if (aServiceGroupDoc == null)
      {
        LOGGER.warn ("Failed to parse provided payload as XML.");
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
      }
      else
      {
        final String sServiceGroupID = aPathVariables.get (Rest2Filter.PARAM_SERVICE_GROUP_ID);
        final ISMPServerAPIDataProvider aDataProvider = new Rest2DataProvider (aRequestScope, sServiceGroupID);
        final BasicAuthClientCredentials aBasicAuth = Rest2RequestHelper.getMandatoryAuth (aRequestScope.headers ());

        ESuccess eSuccess = ESuccess.FAILURE;
        switch (SMPServerConfiguration.getRESTType ())
        {
          case PEPPOL:
          {
            final com.helger.smpclient.peppol.jaxb.ServiceGroupType aServiceGroup = new SMPMarshallerServiceGroupType (XML_SCHEMA_VALIDATION).read (aServiceGroupDoc);
            if (aServiceGroup != null)
            {
              new SMPServerAPI (aDataProvider).saveServiceGroup (sServiceGroupID, aServiceGroup, aBasicAuth);
              eSuccess = ESuccess.SUCCESS;
            }
            break;
          }
          case OASIS_BDXR_V1:
          {
            final com.helger.xsds.bdxr.smp1.ServiceGroupType aServiceGroup = new BDXR1MarshallerServiceGroupType (XML_SCHEMA_VALIDATION).read (aServiceGroupDoc);
            if (aServiceGroup != null)
            {
              new BDXR1ServerAPI (aDataProvider).saveServiceGroup (sServiceGroupID, aServiceGroup, aBasicAuth);
              eSuccess = ESuccess.SUCCESS;
            }
            break;
          }
          default:
            throw new UnsupportedOperationException ("Unsupported REST type specified!");
        }
        if (eSuccess.isFailure ())
          aUnifiedResponse.setStatus (CHttp.HTTP_INTERNAL_SERVER_ERROR);
        else
          aUnifiedResponse.setStatus (CHttp.HTTP_OK);
      }
    }
  }
}
