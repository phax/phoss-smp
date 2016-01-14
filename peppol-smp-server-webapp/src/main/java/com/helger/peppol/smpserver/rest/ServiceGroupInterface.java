/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.peppol.smp.ObjectFactory;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;
import com.helger.photon.core.app.CApplication;
import com.helger.web.mock.MockHttpServletResponse;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * This class implements the REST interface for getting ServiceGroup's. PUT and
 * DELETE are also implemented.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Path ("/{ServiceGroupId}")
public final class ServiceGroupInterface
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ServiceGroupInterface.class);

  @Context
  private HttpServletRequest m_aHttpRequest;

  @Context
  private HttpHeaders m_aHttpHeaders;

  @Context
  private UriInfo m_aUriInfo;

  private final ObjectFactory m_aObjFactory = new ObjectFactory ();

  public ServiceGroupInterface ()
  {}

  @GET
  @Produces (MediaType.TEXT_XML)
  public JAXBElement <ServiceGroupType> getServiceGroup (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    WebScopeManager.onRequestBegin (CApplication.APP_ID_PUBLIC, m_aHttpRequest, new MockHttpServletResponse ());
    try
    {
      final ServiceGroupType ret = new SMPServerAPI (new SMPServerAPIDataProvider (m_aUriInfo)).getServiceGroup (sServiceGroupID);
      return m_aObjFactory.createServiceGroup (ret);
    }
    finally
    {
      WebScopeManager.onRequestEnd ();
    }
  }

  @PUT
  public Response saveServiceGroup (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                    final ServiceGroupType aServiceGroup) throws Throwable
  {
    // Is the writable API disabled?
    if (SMPServerConfiguration.isRESTWritableAPIDisabled ())
    {
      s_aLogger.warn ("The writable REST API is disabled. saveServiceGroup will not be executed.");
      return Response.status (Response.Status.NOT_FOUND).build ();
    }

    WebScopeManager.onRequestBegin (CApplication.APP_ID_PUBLIC, m_aHttpRequest, new MockHttpServletResponse ());
    try
    {
      if (new SMPServerAPI (new SMPServerAPIDataProvider (m_aUriInfo)).saveServiceGroup (sServiceGroupID,
                                                                                         aServiceGroup,
                                                                                         RestRequestHelper.getAuth (m_aHttpHeaders))
                                                                      .isFailure ())
        return Response.status (Status.BAD_REQUEST).build ();
      return Response.ok ().build ();
    }
    finally
    {
      WebScopeManager.onRequestEnd ();
    }
  }

  @DELETE
  public Response deleteServiceGroup (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    // Is the writable API disabled?
    if (SMPServerConfiguration.isRESTWritableAPIDisabled ())
    {
      s_aLogger.warn ("The writable REST API is disabled. deleteServiceGroup will not be executed.");
      return Response.status (Response.Status.NOT_FOUND).build ();
    }

    WebScopeManager.onRequestBegin (CApplication.APP_ID_PUBLIC, m_aHttpRequest, new MockHttpServletResponse ());
    try
    {
      if (new SMPServerAPI (new SMPServerAPIDataProvider (m_aUriInfo)).deleteServiceGroup (sServiceGroupID,
                                                                                           RestRequestHelper.getAuth (m_aHttpHeaders))
                                                                      .isFailure ())
        return Response.status (Status.BAD_REQUEST).build ();
      return Response.ok ().build ();
    }
    finally
    {
      WebScopeManager.onRequestEnd ();
    }
  }
}
