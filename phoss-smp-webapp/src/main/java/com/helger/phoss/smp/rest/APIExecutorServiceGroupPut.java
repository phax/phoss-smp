/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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

import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.CHttp;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.BDXR1ServerAPI;
import com.helger.phoss.smp.restapi.BDXR2ServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPServerAPI;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.bdxr1.marshal.BDXR1MarshallerServiceGroupType;
import com.helger.smpclient.bdxr2.marshal.BDXR2ServiceGroupMarshaller;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceGroupType;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.read.DOMReader;

public final class APIExecutorServiceGroupPut extends AbstractSMPAPIExecutor
{
  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID);
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, sPathServiceGroupID);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. saveServiceGroup will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Parse main payload
    final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    final Document aServiceGroupDoc = DOMReader.readXMLDOM (aPayload);
    if (aServiceGroupDoc == null)
    {
      throw new SMPBadRequestException ("Failed to parse provided payload as XML", aDataProvider.getCurrentURI ());
    }

    final BasicAuthClientCredentials aBasicAuth = getMandatoryAuth (aRequestScope.headers ());
    final boolean bCreateInSML = !"false".equalsIgnoreCase (aRequestScope.params ().getAsString ("create-in-sml"));

    ESuccess eSuccess = ESuccess.FAILURE;
    switch (SMPServerConfiguration.getRESTType ())
    {
      case PEPPOL:
      {
        final com.helger.xsds.peppol.smp1.ServiceGroupType aServiceGroup = new SMPMarshallerServiceGroupType (XML_SCHEMA_VALIDATION).read (aServiceGroupDoc);
        if (aServiceGroup != null)
        {
          new SMPServerAPI (aDataProvider).saveServiceGroup (sPathServiceGroupID,
                                                             aServiceGroup,
                                                             bCreateInSML,
                                                             aBasicAuth);
          eSuccess = ESuccess.SUCCESS;
        }
        break;
      }
      case OASIS_BDXR_V1:
      {
        final com.helger.xsds.bdxr.smp1.ServiceGroupType aServiceGroup = new BDXR1MarshallerServiceGroupType (XML_SCHEMA_VALIDATION).read (aServiceGroupDoc);
        if (aServiceGroup != null)
        {
          new BDXR1ServerAPI (aDataProvider).saveServiceGroup (sPathServiceGroupID,
                                                               aServiceGroup,
                                                               bCreateInSML,
                                                               aBasicAuth);
          eSuccess = ESuccess.SUCCESS;
        }
        break;
      }
      case OASIS_BDXR_V2:
      {
        final com.helger.xsds.bdxr.smp2.ServiceGroupType aServiceGroup = new BDXR2ServiceGroupMarshaller (XML_SCHEMA_VALIDATION).read (aServiceGroupDoc);
        if (aServiceGroup != null)
        {
          new BDXR2ServerAPI (aDataProvider).saveServiceGroup (sPathServiceGroupID,
                                                               aServiceGroup,
                                                               bCreateInSML,
                                                               aBasicAuth);
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
      aUnifiedResponse.setStatus (CHttp.HTTP_OK).disableCaching ();
  }
}
