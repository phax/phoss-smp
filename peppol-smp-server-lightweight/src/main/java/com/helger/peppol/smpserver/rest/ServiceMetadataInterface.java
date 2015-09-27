/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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

import com.helger.peppol.smp.ObjectFactory;
import com.helger.peppol.smp.ServiceMetadataType;
import com.helger.peppol.smp.SignedServiceMetadataType;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;

/**
 * This class implements the REST interface for getting SignedServiceMetadata's.
 * PUT and DELETE are also implemented.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Path ("/{ServiceGroupId}/services/{DocumentTypeId}")
public final class ServiceMetadataInterface
{
  @Context
  private HttpHeaders m_aHttpHeaders;

  @Context
  private UriInfo m_aUriInfo;

  private final ObjectFactory m_aObjFactory = new ObjectFactory ();

  @GET
  // changed Produced media type to match the smp specification.
  @Produces (MediaType.TEXT_XML)
  public JAXBElement <SignedServiceMetadataType> getServiceRegistration (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                                                         @PathParam ("DocumentTypeId") final String sDocumentTypeID) throws Throwable
  {
    final SignedServiceMetadataType ret = new SMPServerAPI (new SMPServerAPIDataProvider (m_aUriInfo)).getServiceRegistration (sServiceGroupID,
                                                                                                                               sDocumentTypeID);
    return m_aObjFactory.createSignedServiceMetadata (ret);
  }

  @PUT
  public Response saveServiceRegistration (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                           @PathParam ("DocumentTypeId") final String sDocumentTypeID,
                                           final ServiceMetadataType aServiceMetadata) throws Throwable
  {
    if (new SMPServerAPI (new SMPServerAPIDataProvider (m_aUriInfo)).saveServiceRegistration (sServiceGroupID,
                                                                                              sDocumentTypeID,
                                                                                              aServiceMetadata,
                                                                                              RestRequestHelper.getAuth (m_aHttpHeaders))
                                                                    .isFailure ())
      return Response.status (Status.BAD_REQUEST).build ();
    return Response.ok ().build ();
  }

  @DELETE
  public Response deleteServiceRegistration (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                             @PathParam ("DocumentTypeId") final String sDocumentTypeID) throws Throwable
  {
    if (new SMPServerAPI (new SMPServerAPIDataProvider (m_aUriInfo)).deleteServiceRegistration (sServiceGroupID,
                                                                                                sDocumentTypeID,
                                                                                                RestRequestHelper.getAuth (m_aHttpHeaders))
                                                                    .isFailure ())
      return Response.status (Status.BAD_REQUEST).build ();
    return Response.ok ().build ();
  }
}
