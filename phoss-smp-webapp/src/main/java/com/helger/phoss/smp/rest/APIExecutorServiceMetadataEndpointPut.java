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

import java.util.Map;

import javax.xml.namespace.QName;

import org.jspecify.annotations.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.annotation.Nonempty;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.jaxb.Bdxr1EndpointMarshaller;
import com.helger.phoss.smp.jaxb.Bdxr2EndpointMarshaller;
import com.helger.phoss.smp.jaxb.SmpEndpointMarshaller;
import com.helger.phoss.smp.restapi.BDXR1ServerAPI;
import com.helger.phoss.smp.restapi.BDXR2ServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.phoss.smp.restapi.SMPServerAPI;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.read.DOMReader;

public final class APIExecutorServiceMetadataEndpointPut extends AbstractSMPAPIExecutor
{
  /**
   * The Peppol SMP and the OASIS BDXR SMP 1.0 XSD declare no global Endpoint element, so the
   * payload cannot be validated against a schema there. Ensure at least that the root element is
   * the expected one, because JAXB happily unmarshals any other element into an empty Endpoint.
   *
   * @param aDoc
   *        The parsed payload. May not be <code>null</code>.
   * @param aExpected
   *        The expected root element name. May not be <code>null</code>.
   * @param aDataProvider
   *        The data provider for the error message. May not be <code>null</code>.
   * @throws SMPBadRequestException
   *         If the root element does not match
   */
  private static void _checkRootElement (@NonNull final Document aDoc,
                                         @NonNull final QName aExpected,
                                         @NonNull final ISMPServerAPIDataProvider aDataProvider) throws SMPBadRequestException
  {
    final Element aRoot = aDoc.getDocumentElement ();
    if (!aExpected.equals (new QName (aRoot.getNamespaceURI (), aRoot.getLocalName ())))
    {
      throw new SMPBadRequestException ("The provided payload is not an '" +
                                        aExpected.getLocalPart () +
                                        "' element of the namespace URI '" +
                                        aExpected.getNamespaceURI () +
                                        "'",
                                        aDataProvider.getCurrentURI ());
    }
  }

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final String sPathDocumentTypeID = aPathVariables.get (SMPRestFilter.PARAM_DOCUMENT_TYPE_ID);
    final String sPathProcessID = aPathVariables.get (SMPRestFilter.PARAM_PROCESS_ID);
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. saveServiceRegistrationEndpoint will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Parse main payload
    final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    final Document aEndpointDoc = DOMReader.readXMLDOM (aPayload);
    if (aEndpointDoc == null)
    {
      throw new SMPBadRequestException ("Failed to parse provided payload as XML", aDataProvider.getCurrentURI ());
    }

    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());

    final ESuccess eSuccess;
    switch (SMPServerConfiguration.getRESTType ())
    {
      case PEPPOL:
      {
        _checkRootElement (aEndpointDoc, SmpEndpointMarshaller.ENDPOINT_QNAME, aDataProvider);
        final var aEndpoint = new SmpEndpointMarshaller ().read (aEndpointDoc);
        if (aEndpoint == null)
        {
          throw new SMPBadRequestException ("Failed to parse provided payload as an Endpoint",
                                            aDataProvider.getCurrentURI ());
        }
        eSuccess = new SMPServerAPI (aDataProvider).saveServiceRegistrationEndpoint (sPathServiceGroupID,
                                                                                     sPathDocumentTypeID,
                                                                                     sPathProcessID,
                                                                                     aEndpoint,
                                                                                     aCredentials);
        break;
      }
      case OASIS_BDXR_V1:
      {
        _checkRootElement (aEndpointDoc, Bdxr1EndpointMarshaller.ENDPOINT_QNAME, aDataProvider);
        final var aEndpoint = new Bdxr1EndpointMarshaller ().read (aEndpointDoc);
        if (aEndpoint == null)
        {
          throw new SMPBadRequestException ("Failed to parse provided payload as an Endpoint",
                                            aDataProvider.getCurrentURI ());
        }
        eSuccess = new BDXR1ServerAPI (aDataProvider).saveServiceRegistrationEndpoint (sPathServiceGroupID,
                                                                                       sPathDocumentTypeID,
                                                                                       sPathProcessID,
                                                                                       aEndpoint,
                                                                                       aCredentials);
        break;
      }
      case OASIS_BDXR_V2:
      {
        _checkRootElement (aEndpointDoc, Bdxr2EndpointMarshaller.ENDPOINT_QNAME, aDataProvider);
        // In contrast to the other two REST types, this payload can be validated
        final var aEndpoint = new Bdxr2EndpointMarshaller ().setUseSchema (XML_SCHEMA_VALIDATION)
                                                            .read (aEndpointDoc);
        if (aEndpoint == null)
        {
          throw new SMPBadRequestException ("Failed to parse provided payload as an Endpoint",
                                            aDataProvider.getCurrentURI ());
        }
        eSuccess = new BDXR2ServerAPI (aDataProvider).saveServiceRegistrationEndpoint (sPathServiceGroupID,
                                                                                       sPathDocumentTypeID,
                                                                                       sPathProcessID,
                                                                                       aEndpoint,
                                                                                       aCredentials);
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
