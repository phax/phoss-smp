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

import org.w3c.dom.Document;

import com.helger.annotation.Nonempty;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.BDXR1ServerAPI;
import com.helger.phoss.smp.restapi.BDXR2ServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.phoss.smp.restapi.SMPServerAPI;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.smpclient.bdxr1.marshal.BDXR1MarshallerServiceMetadataType;
import com.helger.smpclient.bdxr2.marshal.BDXR2MarshallerServiceMetadata;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceMetadataType;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.read.DOMReader;

import jakarta.annotation.Nonnull;

public final class APIExecutorServiceMetadataPut extends AbstractSMPAPIExecutor
{
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
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. saveServiceRegistration will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Parse main payload
    final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    final Document aServiceMetadataDoc = DOMReader.readXMLDOM (aPayload);
    if (aServiceMetadataDoc == null)
    {
      throw new SMPBadRequestException ("Failed to parse provided payload as XML", aDataProvider.getCurrentURI ());
    }

    final String sDocumentTypeID = aPathVariables.get (SMPRestFilter.PARAM_DOCUMENT_TYPE_ID);
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());

    ESuccess eSuccess = ESuccess.FAILURE;
    switch (SMPServerConfiguration.getRESTType ())
    {
      case PEPPOL:
      {
        final var aServiceMetadata = new SMPMarshallerServiceMetadataType ().setUseSchema (XML_SCHEMA_VALIDATION)
                                                                            .read (aServiceMetadataDoc);
        if (aServiceMetadata != null)
        {
          eSuccess = new SMPServerAPI (aDataProvider).saveServiceRegistration (sPathServiceGroupID,
                                                                               sDocumentTypeID,
                                                                               aServiceMetadata,
                                                                               aCredentials);
        }
        break;
      }
      case OASIS_BDXR_V1:
      {
        final var aServiceMetadata = new BDXR1MarshallerServiceMetadataType ().setUseSchema (XML_SCHEMA_VALIDATION)
                                                                              .read (aServiceMetadataDoc);
        if (aServiceMetadata != null)
        {
          eSuccess = new BDXR1ServerAPI (aDataProvider).saveServiceRegistration (sPathServiceGroupID,
                                                                                 sDocumentTypeID,
                                                                                 aServiceMetadata,
                                                                                 aCredentials);
        }
        break;
      }
      case OASIS_BDXR_V2:
      {
        final var aServiceMetadata = new BDXR2MarshallerServiceMetadata ().setUseSchema (XML_SCHEMA_VALIDATION)
                                                                          .read (aServiceMetadataDoc);
        if (aServiceMetadata != null)
        {
          eSuccess = new BDXR2ServerAPI (aDataProvider).saveServiceRegistration (sPathServiceGroupID,
                                                                                 sDocumentTypeID,
                                                                                 aServiceMetadata,
                                                                                 aCredentials);
        }
        break;
      }
      default:
        throw new UnsupportedOperationException ("Unsupported REST type specified!");
    }

    if (eSuccess.isFailure ())
      aUnifiedResponse.createInternalServerError ();
    else
      aUnifiedResponse.createOk ();
  }
}
