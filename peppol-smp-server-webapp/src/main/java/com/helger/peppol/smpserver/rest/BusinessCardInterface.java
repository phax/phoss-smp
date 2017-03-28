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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.state.ESuccess;
import com.helger.pd.businesscard.ObjectFactory;
import com.helger.pd.businesscard.PDBusinessCardType;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.restapi.BusinessCardServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.core.app.CApplication;
import com.helger.servlet.mock.MockHttpServletResponse;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * This class implements the "Business Card" interface to be used with the
 * PEPPOL Directory.
 *
 * @author Philip Helger
 */
@Path ("/businesscard/{ServiceGroupId}")
public final class BusinessCardInterface
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (BusinessCardInterface.class);

  @Context
  private HttpServletRequest m_aHttpRequest;

  @Context
  private HttpHeaders m_aHttpHeaders;

  @Context
  private UriInfo m_aUriInfo;

  private final ObjectFactory m_aBDOF = new ObjectFactory ();

  public BusinessCardInterface ()
  {}

  @GET
  @Produces (MediaType.TEXT_XML)
  public JAXBElement <PDBusinessCardType> getBusinessCard (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    // Is the PEPPOL Directory integration enabled?
    if (!SMPMetaManager.getSettings ().isPEPPOLDirectoryIntegrationEnabled ())
    {
      s_aLogger.warn ("The PEPPOL Directory integration is disabled. getBusinessCard will not be executed.");
      throw new WebApplicationException (404);
    }

    WebScopeManager.onRequestBegin (CApplication.APP_ID_PUBLIC, m_aHttpRequest, new MockHttpServletResponse ());
    try
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);

      // getBusinessCard trows an exception if non is found
      final PDBusinessCardType ret = new BusinessCardServerAPI (aDataProvider).getBusinessCard (sServiceGroupID);
      return m_aBDOF.createBusinessCard (ret);
    }
    finally
    {
      WebScopeManager.onRequestEnd ();
    }
  }

  @PUT
  @Consumes ({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
  public Response saveBusinessCard (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                    final Document aServiceGroupDoc) throws Throwable
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      s_aLogger.warn ("The writable REST API is disabled. saveBusinessCard will not be executed.");
      return Response.status (Response.Status.NOT_FOUND).build ();
    }

    WebScopeManager.onRequestBegin (CApplication.APP_ID_PUBLIC, m_aHttpRequest, new MockHttpServletResponse ());
    try
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);
      final ESuccess eSuccess = new BusinessCardServerAPI (aDataProvider).createBusinessCard (sServiceGroupID,
                                                                                              aServiceGroupDoc,
                                                                                              RestRequestHelper.getAuth (m_aHttpHeaders));
      if (eSuccess.isFailure ())
        return Response.status (Status.BAD_REQUEST).build ();
      return Response.ok ().build ();
    }
    finally
    {
      WebScopeManager.onRequestEnd ();
    }
  }

  @DELETE
  public Response deleteBusinessCard (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      s_aLogger.warn ("The writable REST API is disabled. deleteBusinessCard will not be executed.");
      return Response.status (Response.Status.NOT_FOUND).build ();
    }

    WebScopeManager.onRequestBegin (CApplication.APP_ID_PUBLIC, m_aHttpRequest, new MockHttpServletResponse ());
    try
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);
      final ESuccess eSuccess = new BusinessCardServerAPI (aDataProvider).deleteBusinessCard (sServiceGroupID,
                                                                                              RestRequestHelper.getAuth (m_aHttpHeaders));
      if (eSuccess.isFailure ())
        return Response.status (Status.BAD_REQUEST).build ();
      return Response.ok ().build ();
    }
    finally
    {
      WebScopeManager.onRequestEnd ();
    }
  }
}
