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

import java.util.List;

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
import org.busdox.servicemetadata.publishing._1.ServiceGroupType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataReferenceCollectionType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.peppol.identifier.DocumentIdentifierType;
import com.helger.peppol.identifier.IdentifierUtils;
import com.helger.peppol.identifier.ParticipantIdentifierType;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.data.DataManagerFactory;
import com.helger.peppol.smpserver.data.IDataManager;
import com.sun.jersey.api.NotFoundException;

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
  private HttpHeaders headers;
  @Context
  private UriInfo uriInfo;

  public ServiceGroupInterface ()
  {}

  @GET
  @Produces (MediaType.TEXT_XML)
  public JAXBElement <ServiceGroupType> getServiceGroup (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    s_aLogger.info ("GET /" + sServiceGroupID);

    final ParticipantIdentifierType aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse participant identifier '" + sServiceGroupID + "'");
      return null;
    }

    try
    {
      final ObjectFactory aObjFactory = new ObjectFactory ();

      // Retrieve the service group
      final IDataManager aDataManager = DataManagerFactory.getInstance ();
      final ServiceGroupType aServiceGroup = aDataManager.getServiceGroup (aServiceGroupID);
      if (aServiceGroup == null)
      {
        // No such service group
        throw new NotFoundException ("serviceGroup", uriInfo.getAbsolutePath ());
      }

      // Then add the service metadata references
      final ServiceMetadataReferenceCollectionType aCollectionType = aObjFactory.createServiceMetadataReferenceCollectionType ();
      final List <ServiceMetadataReferenceType> aMetadataReferences = aCollectionType.getServiceMetadataReference ();

      final List <DocumentIdentifierType> aDocTypeIds = aDataManager.getDocumentTypes (aServiceGroupID);
      for (final DocumentIdentifierType aDocTypeId : aDocTypeIds)
      {
        final ServiceMetadataReferenceType aMetadataReference = aObjFactory.createServiceMetadataReferenceType ();
        // Ensure that no context is emitted by using "replacePath" first!
        aMetadataReference.setHref (uriInfo.getBaseUriBuilder ()
                                           .replacePath ("")
                                           .path (ServiceGroupInterface.class)
                                           .buildFromEncoded (IdentifierUtils.getIdentifierURIPercentEncoded (aServiceGroupID),
                                                              IdentifierUtils.getIdentifierURIPercentEncoded (aDocTypeId))
                                           .toString ());
        aMetadataReferences.add (aMetadataReference);
      }
      aServiceGroup.setServiceMetadataReferenceCollection (aCollectionType);

      s_aLogger.info ("Finished getServiceGroup(" + sServiceGroupID + ")");

      /*
       * Finally return it
       */
      return aObjFactory.createServiceGroup (aServiceGroup);
    }
    catch (final NotFoundException ex)
    {
      // No logging needed here - already logged in DB
      throw ex;
    }
    catch (final Throwable ex)
    {
      s_aLogger.error ("Error getting service group " + aServiceGroupID, ex);
      throw ex;
    }
  }

  @PUT
  public Response saveServiceGroup (@PathParam ("ServiceGroupId") final String sServiceGroupID,
                                    final ServiceGroupType aServiceGroup) throws Throwable
  {
    s_aLogger.info ("PUT /" + sServiceGroupID + " ==> " + aServiceGroup);

    final ParticipantIdentifierType aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse participant identifier '" + sServiceGroupID + "'");
      return Response.status (Status.BAD_REQUEST).build ();
    }

    try
    {
      if (!IdentifierUtils.areIdentifiersEqual (aServiceGroupID, aServiceGroup.getParticipantIdentifier ()))
      {
        // Business identifier must equal path
        return Response.status (Status.BAD_REQUEST).build ();
      }

      final IDataManager aDataManager = DataManagerFactory.getInstance ();
      aDataManager.saveServiceGroup (aServiceGroup, RestRequestHelper.getAuth (headers));

      s_aLogger.info ("Finished saveServiceGroup(" + sServiceGroupID + "," + aServiceGroup + ")");

      return Response.ok ().build ();
    }
    catch (final Throwable ex)
    {
      s_aLogger.error ("Error saving service group " + aServiceGroupID, ex);
      throw ex;
    }
  }

  @DELETE
  public Response deleteServiceGroup (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    s_aLogger.info ("DELETE /" + sServiceGroupID);

    final ParticipantIdentifierType aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse participant identifier '" + sServiceGroupID + "'");
      return Response.status (Status.BAD_REQUEST).build ();
    }

    try
    {
      final IDataManager aDataManager = DataManagerFactory.getInstance ();
      aDataManager.deleteServiceGroup (aServiceGroupID, RestRequestHelper.getAuth (headers));

      s_aLogger.info ("Finished deleteServiceGroup(" + sServiceGroupID + ")");

      return Response.ok ().build ();
    }
    catch (final Throwable ex)
    {
      s_aLogger.error ("Error deleting service group " + aServiceGroupID, ex);
      throw ex;
    }
  }
}
