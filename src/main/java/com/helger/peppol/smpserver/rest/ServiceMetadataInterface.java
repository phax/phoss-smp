/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.rest;

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
import javax.xml.bind.JAXBElement;

import org.busdox.servicemetadata.publishing._1.ObjectFactory;
import org.busdox.servicemetadata.publishing._1.ServiceInformationType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataType;
import org.busdox.servicemetadata.publishing._1.SignedServiceMetadataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.peppol.identifier.DocumentIdentifierType;
import com.helger.peppol.identifier.IdentifierUtils;
import com.helger.peppol.identifier.ParticipantIdentifierType;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.data.DataManagerFactory;
import com.helger.peppol.smpserver.data.IDataManager;
import com.sun.jersey.api.NotFoundException;

/**
 * This class implements the REST interface for getting SignedServiceMetadata's.
 * PUT and DELETE are also implemented.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Path ("/{ServiceGroupId}/services/{DocumentTypeId}")
public final class ServiceMetadataInterface
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ServiceMetadataInterface.class);

  @Context
  private HttpHeaders headers;
  @Context
  private UriInfo uriInfo;

  public ServiceMetadataInterface ()
  {}

  @GET
  // changed Produced media type to match the smp specification.
  @Produces (MediaType.TEXT_XML)
  public JAXBElement <SignedServiceMetadataType> getServiceRegistration (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                                                         @PathParam ("DocumentTypeId") final String sDocumentTypeID) throws Throwable
  {
    s_aLogger.info ("GET /" + sServiceGroupID + "/services/" + sDocumentTypeID);

    final ParticipantIdentifierType aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse participant identifier '" + sServiceGroupID + "'");
      return null;
    }

    final DocumentIdentifierType aDocTypeID = IdentifierUtils.createDocumentTypeIdentifierFromURIPartOrNull (sDocumentTypeID);
    if (aDocTypeID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse document type identifier '" + sDocumentTypeID + "'");
      return null;
    }

    try
    {
      final ObjectFactory aObjFactory = new ObjectFactory ();
      final IDataManager aDataManager = DataManagerFactory.getInstance ();

      // First check for redirection, then for actual service
      ServiceMetadataType aService = aDataManager.getRedirection (aServiceGroupID, aDocTypeID);
      if (aService == null)
      {
        aService = aDataManager.getService (aServiceGroupID, aDocTypeID);
        if (aService == null)
          throw new NotFoundException ("service", uriInfo.getAbsolutePath ());
      }

      final SignedServiceMetadataType aSignedServiceMetadata = aObjFactory.createSignedServiceMetadataType ();
      aSignedServiceMetadata.setServiceMetadata (aService);
      // Signature is added by a handler

      s_aLogger.info ("Finished getServiceRegistration(" + sServiceGroupID + "," + sDocumentTypeID + ")");
      return aObjFactory.createSignedServiceMetadata (aSignedServiceMetadata);
    }
    catch (final Throwable ex)
    {
      s_aLogger.error ("Error in returning service metadata.", ex);
      throw ex;
    }
  }

  @PUT
  public Response saveServiceRegistration (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                           @PathParam ("DocumentTypeId") final String sDocumentTypeID,
                                           final ServiceMetadataType aServiceMetadata) throws Throwable
  {
    s_aLogger.info ("PUT /" + sServiceGroupID + "/services/" + sDocumentTypeID + " ==> " + aServiceMetadata);

    final SimpleParticipantIdentifier aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse participant identifier '" + sServiceGroupID + "'");
      return Response.status (Status.BAD_REQUEST).build ();
    }

    final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createFromURIPartOrNull (sDocumentTypeID);
    if (aDocTypeID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse document type identifier '" + sDocumentTypeID + "'");
      return null;
    }

    try
    {
      final ServiceInformationType aServiceInformationType = aServiceMetadata.getServiceInformation ();

      // Business identifiers from path (ServiceGroupID) and from service
      // metadata (body) must equal path
      if (!IdentifierUtils.areIdentifiersEqual (aServiceInformationType.getParticipantIdentifier (), aServiceGroupID))
      {
        s_aLogger.info ("Save service metadata was called with bad parameters. serviceInfo:" +
                        IdentifierUtils.getIdentifierURIEncoded (aServiceInformationType.getParticipantIdentifier ()) +
                        " param:" +
                        aServiceGroupID);
        return Response.status (Status.BAD_REQUEST).build ();
      }

      if (!IdentifierUtils.areIdentifiersEqual (aServiceInformationType.getDocumentIdentifier (), aDocTypeID))
      {
        s_aLogger.info ("Save service metadata was called with bad parameters. serviceInfo:" +
                        IdentifierUtils.getIdentifierURIEncoded (aServiceInformationType.getDocumentIdentifier ()) +
                        " param:" +
                        aDocTypeID);
        // Document type must equal path
        return Response.status (Status.BAD_REQUEST).build ();
      }

      // Main save
      final IDataManager aDataManager = DataManagerFactory.getInstance ();
      aDataManager.saveService (aServiceMetadata, RestRequestHelper.getAuth (headers));

      s_aLogger.info ("Finished saveServiceRegistration(" +
                      sServiceGroupID +
                      "," +
                      sDocumentTypeID +
                      "," +
                      aServiceMetadata +
                      ")");

      return Response.ok ().build ();
    }
    catch (final Throwable ex)
    {
      s_aLogger.error ("Error in saving Service metadata.", ex);
      throw ex;
    }
  }

  @DELETE
  public Response deleteServiceRegistration (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                             @PathParam ("DocumentTypeId") final String sDocumentTypeID) throws Throwable
  {
    s_aLogger.info ("DELETE /" + sServiceGroupID + "/services/" + sDocumentTypeID);

    final SimpleParticipantIdentifier aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse participant identifier '" + sServiceGroupID + "'");
      return Response.status (Status.BAD_REQUEST).build ();
    }

    final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createFromURIPartOrNull (sDocumentTypeID);
    if (aDocTypeID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse document type identifier '" + sDocumentTypeID + "'");
      return null;
    }

    try
    {
      final IDataManager aDataManager = DataManagerFactory.getInstance ();
      aDataManager.deleteService (aServiceGroupID, aDocTypeID, RestRequestHelper.getAuth (headers));

      s_aLogger.info ("Finished deleteServiceRegistration(" + sServiceGroupID + "," + sDocumentTypeID);

      return Response.ok ().build ();
    }
    catch (final Throwable ex)
    {
      s_aLogger.error ("Error in deleting Service metadata.", ex);
      throw ex;
    }
  }
}
