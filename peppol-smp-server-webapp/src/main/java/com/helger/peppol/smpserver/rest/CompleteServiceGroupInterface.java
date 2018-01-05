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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.w3c.dom.Document;

import com.helger.peppol.bdxr.marshal.BDXRMarshallerCompleteServiceGroupType;
import com.helger.peppol.smp.marshal.SMPMarshallerCompleteServiceGroupType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.restapi.BDXRServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;
import com.helger.web.scope.mgr.WebScoped;

/**
 * This class implements a REST frontend for getting the ServiceGroup as well as
 * all the corresponding ServiceMetadata's given a service group id. This
 * interface is not part of the official specification. The interface makes it
 * much faster to fetch the complete data about a service group and its service
 * metadata.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Path ("/complete/{ServiceGroupId}")
public final class CompleteServiceGroupInterface
{
  @Context
  private HttpServletRequest m_aHttpRequest;

  @Context
  private UriInfo m_aUriInfo;

  public CompleteServiceGroupInterface ()
  {}

  @GET
  @Produces (MediaType.TEXT_XML)
  public Document getCompleteServiceGroup (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    try (final WebScoped aWebScoped = new WebScoped (m_aHttpRequest))
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPServerAPIDataProvider (m_aUriInfo);
      switch (SMPServerConfiguration.getRESTType ())
      {
        case PEPPOL:
        {
          // Unspecified extension
          final com.helger.peppol.smp.CompleteServiceGroupType ret = new SMPServerAPI (aDataProvider).getCompleteServiceGroup (sServiceGroupID);
          return new SMPMarshallerCompleteServiceGroupType ().getAsDocument (ret);
        }
        case BDXR:
        {
          // Unspecified extension
          final com.helger.peppol.bdxr.CompleteServiceGroupType ret = new BDXRServerAPI (aDataProvider).getCompleteServiceGroup (sServiceGroupID);
          return new BDXRMarshallerCompleteServiceGroupType ().getAsDocument (ret);
        }
        default:
          throw new UnsupportedOperationException ("Unsupported REST type specified!");
      }
    }
  }
}
