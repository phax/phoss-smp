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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.state.ESuccess;
import com.helger.peppol.bdxr.marshal.BDXRMarshallerServiceGroupType;
import com.helger.peppol.smp.marshal.SMPMarshallerServiceGroupType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.restapi.BDXRServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;
import com.helger.web.scope.mgr.WebScoped;

/**
 * This class implements the REST interface for getting ServiceGroup's. PUT and
 * DELETE are also implemented.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Path ("/{ServiceGroupId}")
public final class ServiceGroupInterface
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceGroupInterface.class);

  @Context
  private HttpServletRequest m_aHttpRequest;

  @Context
  private HttpHeaders m_aHttpHeaders;

  @Context
  private UriInfo m_aUriInfo;

  public ServiceGroupInterface ()
  {}

  @GET
  @Produces (MediaType.TEXT_XML)
  public Document getServiceGroup (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    try (final WebScoped aWebScoped = new WebScoped (m_aHttpRequest))
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);
      switch (SMPServerConfiguration.getRESTType ())
      {
        case PEPPOL:
        {
          final com.helger.peppol.smp.ServiceGroupType ret = new SMPServerAPI (aDataProvider).getServiceGroup (sServiceGroupID);
          return new SMPMarshallerServiceGroupType ().getAsDocument (ret);
        }
        case BDXR:
        {
          final com.helger.peppol.bdxr.ServiceGroupType ret = new BDXRServerAPI (aDataProvider).getServiceGroup (sServiceGroupID);
          return new BDXRMarshallerServiceGroupType ().getAsDocument (ret);
        }
        default:
          throw new UnsupportedOperationException ("Unsupported REST type specified!");
      }
    }
  }

  @PUT
  @Consumes ({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
  public Response saveServiceGroup (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                    final Document aServiceGroupDoc) throws Throwable
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. saveServiceGroup will not be executed.");
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
          final com.helger.peppol.smp.ServiceGroupType aServiceGroup = new SMPMarshallerServiceGroupType ().read (aServiceGroupDoc);
          if (aServiceGroup != null)
            eSuccess = new SMPServerAPI (aDataProvider).saveServiceGroup (sServiceGroupID,
                                                                          aServiceGroup,
                                                                          RestRequestHelper.getAuth (m_aHttpHeaders));
          break;
        }
        case BDXR:
        {
          final com.helger.peppol.bdxr.ServiceGroupType aServiceGroup = new BDXRMarshallerServiceGroupType ().read (aServiceGroupDoc);
          if (aServiceGroup != null)
            eSuccess = new BDXRServerAPI (aDataProvider).saveServiceGroup (sServiceGroupID,
                                                                           aServiceGroup,
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
  public Response deleteServiceGroup (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. deleteServiceGroup will not be executed.");
      return Response.status (Response.Status.NOT_FOUND).build ();
    }

    try (final WebScoped aWebScoped = new WebScoped (m_aHttpRequest))
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);
      ESuccess eSuccess;
      switch (SMPServerConfiguration.getRESTType ())
      {
        case PEPPOL:
          eSuccess = new SMPServerAPI (aDataProvider).deleteServiceGroup (sServiceGroupID,
                                                                          RestRequestHelper.getAuth (m_aHttpHeaders));

          break;
        case BDXR:
          eSuccess = new BDXRServerAPI (aDataProvider).deleteServiceGroup (sServiceGroupID,
                                                                           RestRequestHelper.getAuth (m_aHttpHeaders));

          break;
        default:
          throw new UnsupportedOperationException ("Unsupported REST type specified!");
      }
      if (eSuccess.isFailure ())
        return Response.status (Status.BAD_REQUEST).build ();
      return Response.ok ().build ();
    }
  }
}
