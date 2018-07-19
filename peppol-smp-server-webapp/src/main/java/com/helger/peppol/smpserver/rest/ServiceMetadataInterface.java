/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.state.ESuccess;
import com.helger.peppol.bdxr.marshal.BDXRMarshallerServiceMetadataType;
import com.helger.peppol.bdxr.marshal.BDXRMarshallerSignedServiceMetadataType;
import com.helger.peppol.smp.marshal.SMPMarshallerServiceMetadataType;
import com.helger.peppol.smp.marshal.SMPMarshallerSignedServiceMetadataType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.restapi.BDXRServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xml.serialize.write.EXMLIncorrectCharacterHandling;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.IXMLWriterSettings;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.helger.xml.transform.XMLTransformerFactory;

/**
 * This class implements the REST interface for getting SignedServiceMetadata's.
 * PUT and DELETE are also implemented.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Path ("/{ServiceGroupId}/services/{DocumentTypeId}")
public final class ServiceMetadataInterface
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceMetadataInterface.class);

  @Context
  private HttpServletRequest m_aHttpRequest;

  @Context
  private HttpHeaders m_aHttpHeaders;

  @Context
  private UriInfo m_aUriInfo;

  @GET
  @Produces (MediaType.TEXT_XML)
  public byte [] getServiceRegistration (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                         @PathParam ("DocumentTypeId") final String sDocumentTypeID) throws Throwable
  {
    try (final WebScoped aWebScoped = new WebScoped (m_aHttpRequest))
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);

      // Create the unsigned response document
      Document aDoc;
      switch (SMPServerConfiguration.getRESTType ())
      {
        case PEPPOL:
        {
          final com.helger.peppol.smp.SignedServiceMetadataType ret = new SMPServerAPI (aDataProvider).getServiceRegistration (sServiceGroupID,
                                                                                                                               sDocumentTypeID);

          // Convert to DOM document
          final SMPMarshallerSignedServiceMetadataType aMarshaller = new SMPMarshallerSignedServiceMetadataType ();
          aDoc = aMarshaller.getAsDocument (ret);
          break;
        }
        case BDXR:
        {
          final com.helger.peppol.bdxr.SignedServiceMetadataType ret = new BDXRServerAPI (aDataProvider).getServiceRegistration (sServiceGroupID,
                                                                                                                                 sDocumentTypeID);

          // Convert to DOM document
          final BDXRMarshallerSignedServiceMetadataType aMarshaller = new BDXRMarshallerSignedServiceMetadataType ();
          aDoc = aMarshaller.getAsDocument (ret);
          break;
        }
        default:
          throw new UnsupportedOperationException ("Unsupported REST type specified!");
      }

      // Sign the document
      try
      {
        SMPKeyManager.getInstance ().signXML (aDoc.getDocumentElement (),
                                              SMPServerConfiguration.getRESTType ().isBDXR ());
      }
      catch (final Exception ex)
      {
        throw new RuntimeException ("Error in signing xml", ex);
      }

      try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
      {
        if (false)
        {
          // IMPORTANT: no indent and no align!
          final IXMLWriterSettings aSettings = new XMLWriterSettings ().setIncorrectCharacterHandling (EXMLIncorrectCharacterHandling.THROW_EXCEPTION)
                                                                       .setIndent (EXMLSerializeIndent.NONE);

          // Write the result to a byte array
          if (XMLWriter.writeToStream (aDoc, aBAOS, aSettings).isFailure ())
            throw new RuntimeException ("Failed to serialize signed node!");
        }
        else
        {
          // Use this because it correctly serializes &#13; which is important
          // for
          // validating the signature!
          try
          {
            XMLTransformerFactory.newTransformer ().transform (new DOMSource (aDoc), new StreamResult (aBAOS));
          }
          catch (final TransformerException ex)
          {
            throw new IllegalStateException ("Failed to save to XML", ex);
          }
        }

        return aBAOS.toByteArray ();
      }
    }
  }

  @PUT
  @Consumes ({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
  public Response saveServiceRegistration (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                           @PathParam ("DocumentTypeId") final String sDocumentTypeID,
                                           final Document aServiceMetadataDoc) throws Throwable
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. saveServiceRegistration will not be executed.");
      return Response.status (Response.Status.NOT_FOUND).build ();
    }

    try (final WebScoped aWebScoped = new WebScoped (m_aHttpRequest))
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);
      ESuccess eSuccess = ESuccess.FAILURE;
      switch (SMPServerConfiguration.getRESTType ())
      {
        case PEPPOL:
        {
          final com.helger.peppol.smp.ServiceMetadataType aServiceMetadata = new SMPMarshallerServiceMetadataType ().read (aServiceMetadataDoc);
          if (aServiceMetadata != null)
            eSuccess = new SMPServerAPI (aDataProvider).saveServiceRegistration (sServiceGroupID,
                                                                                 sDocumentTypeID,
                                                                                 aServiceMetadata,
                                                                                 RestRequestHelper.getAuth (m_aHttpHeaders));
          break;
        }
        case BDXR:
        {
          final com.helger.peppol.bdxr.ServiceMetadataType aServiceMetadata = new BDXRMarshallerServiceMetadataType ().read (aServiceMetadataDoc);
          if (aServiceMetadata != null)
            eSuccess = new BDXRServerAPI (aDataProvider).saveServiceRegistration (sServiceGroupID,
                                                                                  sDocumentTypeID,
                                                                                  aServiceMetadata,
                                                                                  RestRequestHelper.getAuth (m_aHttpHeaders));
          break;
        }
        default:
          throw new UnsupportedOperationException ("Unsupported REST type specified!");
      }
      if (eSuccess.isFailure ())
        return Response.status (Status.BAD_REQUEST).build ();
      return Response.ok ().build ();
    }
  }

  @DELETE
  public Response deleteServiceRegistration (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                             @PathParam ("DocumentTypeId") final String sDocumentTypeID) throws Throwable
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. deleteServiceRegistration will not be executed.");
      return Response.status (Response.Status.NOT_FOUND).build ();
    }

    try (final WebScoped aWebScoped = new WebScoped (m_aHttpRequest))
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);
      ESuccess eSuccess;
      switch (SMPServerConfiguration.getRESTType ())
      {
        case PEPPOL:
          eSuccess = new SMPServerAPI (aDataProvider).deleteServiceRegistration (sServiceGroupID,
                                                                                 sDocumentTypeID,
                                                                                 RestRequestHelper.getAuth (m_aHttpHeaders));
          break;
        case BDXR:
          eSuccess = new BDXRServerAPI (aDataProvider).deleteServiceRegistration (sServiceGroupID,
                                                                                  sDocumentTypeID,
                                                                                  RestRequestHelper.getAuth (m_aHttpHeaders));
          break;
        default:
          throw new UnsupportedOperationException ("Unsupported REST type specified!");
      }

      if (eSuccess.isFailure ())
        return Response.status (Status.NOT_FOUND).build ();
      return Response.ok ().build ();
    }
  }
}
