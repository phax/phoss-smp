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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.w3c.dom.Document;

import com.helger.peppol.bdxr.marshal.BDXRMarshallerServiceGroupReferenceListType;
import com.helger.peppol.smp.marshal.SMPMarshallerServiceGroupReferenceListType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.restapi.BDXRServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;
import com.helger.web.scope.mgr.WebScoped;

/**
 * This class implements a REST frontend for getting the list of service groups
 * for a given user. This REST service is not part of the official
 * specifications!
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Path ("/list/{UserId}")
public final class ListInterface
{
  @Context
  private HttpServletRequest m_aHttpRequest;

  @Context
  private HttpHeaders m_aHttpHeaders;

  @Context
  private UriInfo m_aUriInfo;

  public ListInterface ()
  {}

  @GET
  @Produces (MediaType.TEXT_XML)
  public Document getServiceGroupReferenceList (@PathParam ("UserId") final String sUserID) throws Throwable
  {
    try (final WebScoped aWebScoped = new WebScoped (m_aHttpRequest))
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);
      switch (SMPServerConfiguration.getRESTType ())
      {
        case PEPPOL:
        {
          // Unspecified extension
          final com.helger.peppol.smp.ServiceGroupReferenceListType ret = new SMPServerAPI (aDataProvider).getServiceGroupReferenceList (sUserID,
                                                                                                                                         RestRequestHelper.getAuth (m_aHttpHeaders));
          return new SMPMarshallerServiceGroupReferenceListType ().getAsDocument (ret);
        }
        case BDXR:
        {
          // Unspecified extension
          final com.helger.peppol.bdxr.ServiceGroupReferenceListType ret = new BDXRServerAPI (aDataProvider).getServiceGroupReferenceList (sUserID,
                                                                                                                                           RestRequestHelper.getAuth (m_aHttpHeaders));
          return new BDXRMarshallerServiceGroupReferenceListType ().getAsDocument (ret);
        }
        default:
          throw new UnsupportedOperationException ("Unsupported REST type specified!");
      }
    }
  }
}
