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

import java.util.Collection;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;

import org.busdox.servicemetadata.publishing._1.ObjectFactory;
import org.busdox.servicemetadata.publishing._1.ServiceGroupReferenceListType;
import org.busdox.servicemetadata.publishing._1.ServiceGroupReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.peppol.identifier.IdentifierUtils;
import com.helger.peppol.identifier.ParticipantIdentifierType;
import com.helger.peppol.smpserver.data.DataManagerFactory;
import com.helger.peppol.smpserver.data.IDataManager;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * This class implements a REST frontend for getting the list of service groups
 * for a given user.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Path ("/list/{UserId}")
public final class ListInterface
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ListInterface.class);

  @Context
  private HttpHeaders headers;
  @Context
  private UriInfo uriInfo;

  public ListInterface ()
  {}

  @GET
  @Produces (MediaType.TEXT_XML)
  public JAXBElement <ServiceGroupReferenceListType> getServiceGroup (@PathParam ("UserId") final String sUserId) throws Throwable
  {
    s_aLogger.info ("GET /list/" + sUserId);

    try
    {
      final ObjectFactory aObjFactory = new ObjectFactory ();
      final BasicAuthClientCredentials aCredentials = RestRequestHelper.getAuth (headers);
      if (!aCredentials.getUserName ().equals (sUserId))
      {
        throw new SMPUnauthorizedException ("URL user name '" +
                                         sUserId +
                                         "' does not match HTTP Basic Auth user name '" +
                                         aCredentials.getUserName () +
                                         "'");
      }

      final IDataManager aDataManager = DataManagerFactory.getInstance ();
      final Collection <ParticipantIdentifierType> aServiceGroupList = aDataManager.getServiceGroupList (aCredentials);

      final ServiceGroupReferenceListType aRefList = aObjFactory.createServiceGroupReferenceListType ();
      final List <ServiceGroupReferenceType> aReferenceTypes = aRefList.getServiceGroupReference ();
      for (final ParticipantIdentifierType aServiceGroupID : aServiceGroupList)
      {
        // Ensure that no context is emitted by using "replacePath" first!
        final String sHref = uriInfo.getBaseUriBuilder ()
                                    .replacePath ("")
                                    .path (CompleteServiceGroupInterface.class)
                                    .buildFromEncoded (IdentifierUtils.getIdentifierURIPercentEncoded (aServiceGroupID))
                                    .toString ();

        final ServiceGroupReferenceType aServGroupRefType = aObjFactory.createServiceGroupReferenceType ();
        aServGroupRefType.setHref (sHref);
        aReferenceTypes.add (aServGroupRefType);
      }

      s_aLogger.info ("Finished getServiceGroup(" + sUserId + ")");

      return aObjFactory.createServiceGroupReferenceList (aRefList);
    }
    catch (final Throwable ex)
    {
      s_aLogger.error ("A error occured when listing service groups for user: " + sUserId, ex);
      throw ex;
    }
  }
}
