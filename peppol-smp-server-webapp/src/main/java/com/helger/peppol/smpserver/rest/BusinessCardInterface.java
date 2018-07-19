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
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.v1.ObjectFactory;
import com.helger.pd.businesscard.v1.PD1APIHelper;
import com.helger.pd.businesscard.v1.PD1BusinessCardMarshaller;
import com.helger.pd.businesscard.v1.PD1BusinessCardType;
import com.helger.pd.businesscard.v2.PD2APIHelper;
import com.helger.pd.businesscard.v2.PD2BusinessCardMarshaller;
import com.helger.pd.businesscard.v2.PD2BusinessCardType;
import com.helger.peppol.smpserver.app.AppConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.restapi.BusinessCardServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.web.scope.mgr.WebScoped;

/**
 * This class implements the "Business Card" REST interface to be used with the
 * PEPPOL Directory. The main logic behind the interfaces are available in class
 * {@link BusinessCardServerAPI}.
 *
 * @author Philip Helger
 */
@Path ("/businesscard/{ServiceGroupId}")
public final class BusinessCardInterface
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BusinessCardInterface.class);

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
  public JAXBElement <PD1BusinessCardType> getBusinessCard (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    // Is the PEPPOL Directory integration enabled?
    if (!SMPMetaManager.getSettings ().isPEPPOLDirectoryIntegrationEnabled ())
    {
      LOGGER.warn ("The " +
                      AppConfiguration.getDirectoryName () +
                      " integration is disabled. getBusinessCard will not be executed.");
      throw new WebApplicationException (404);
    }

    try (final WebScoped aWebScoped = new WebScoped (m_aHttpRequest))
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);
      // getBusinessCard throws an exception if non is found
      final PD1BusinessCardType ret = new BusinessCardServerAPI (aDataProvider).getBusinessCard (sServiceGroupID);
      return m_aBDOF.createBusinessCard (ret);
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
      LOGGER.warn ("The writable REST API is disabled. saveBusinessCard will not be executed.");
      return Response.status (Response.Status.NOT_FOUND).build ();
    }

    try (final WebScoped aWebScoped = new WebScoped (m_aHttpRequest))
    {
      PDBusinessCard aBC = null;
      final PD1BusinessCardType aV1 = new PD1BusinessCardMarshaller ().read (aServiceGroupDoc);
      if (aV1 != null)
      {
        // Convert to wider format
        aBC = PD1APIHelper.createBusinessCard (aV1);
      }
      else
      {
        final PD2BusinessCardType aV2 = new PD2BusinessCardMarshaller ().read (aServiceGroupDoc);
        if (aV2 != null)
        {
          // Convert to wider format
          aBC = PD2APIHelper.createBusinessCard (aV2);
        }
      }

      if (aBC == null)
        return Response.status (Response.Status.BAD_REQUEST).build ();

      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);
      final ESuccess eSuccess = new BusinessCardServerAPI (aDataProvider).createBusinessCard (sServiceGroupID,
                                                                                              aBC,
                                                                                              RestRequestHelper.getAuth (m_aHttpHeaders));
      if (eSuccess.isFailure ())
        return Response.status (Status.BAD_REQUEST).build ();
      return Response.ok ().build ();
    }
  }

  @DELETE
  public Response deleteBusinessCard (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. deleteBusinessCard will not be executed.");
      return Response.status (Response.Status.NOT_FOUND).build ();
    }

    try (final WebScoped aWebScoped = new WebScoped (m_aHttpRequest))
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);
      final ESuccess eSuccess = new BusinessCardServerAPI (aDataProvider).deleteBusinessCard (sServiceGroupID,
                                                                                              RestRequestHelper.getAuth (m_aHttpHeaders));
      if (eSuccess.isFailure ())
        return Response.status (Status.BAD_REQUEST).build ();
      return Response.ok ().build ();
    }
  }
}
