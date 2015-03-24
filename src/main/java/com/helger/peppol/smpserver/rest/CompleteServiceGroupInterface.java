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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;

import org.busdox.servicemetadata.publishing._1.CompleteServiceGroupType;
import org.busdox.servicemetadata.publishing._1.ObjectFactory;
import org.busdox.servicemetadata.publishing._1.ServiceGroupType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataReferenceCollectionType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataReferenceType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataType;
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
 * This class implements a REST frontend for getting the ServiceGroup as well as
 * all the corresponding ServiceMetadata's given a service group id. This
 * interface is not part of the official specification. The interface makes it
 * much faster to fetch the complete data about a service group and its service
 * metadata. The interface is used by the registration web site.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Path ("/complete/{ServiceGroupId}")
public final class CompleteServiceGroupInterface
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (CompleteServiceGroupInterface.class);

  @Context
  private UriInfo m_aUriInfo;

  public CompleteServiceGroupInterface ()
  {}

  @GET
  @Produces (MediaType.TEXT_XML)
  public JAXBElement <CompleteServiceGroupType> getServiceGroup (@PathParam ("ServiceGroupId") final String sServiceGroupID) throws Throwable
  {
    s_aLogger.info ("GET /complete/" + sServiceGroupID);

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

      final IDataManager aDataManager = DataManagerFactory.getInstance ();
      final ServiceGroupType aServiceGroup = aDataManager.getServiceGroup (aServiceGroupID);
      if (aServiceGroup == null)
      {
        // No such service group
        throw new NotFoundException ("serviceGroup", m_aUriInfo.getAbsolutePath ());
      }

      /*
       * Then add the service metadata references
       */
      final ServiceMetadataReferenceCollectionType aRefCollection = aObjFactory.createServiceMetadataReferenceCollectionType ();
      final List <ServiceMetadataReferenceType> aMetadataReferences = aRefCollection.getServiceMetadataReference ();

      final List <DocumentIdentifierType> aDocTypeIds = aDataManager.getDocumentTypes (aServiceGroupID);
      for (final DocumentIdentifierType aDocTypeId : aDocTypeIds)
      {
        final ServiceMetadataReferenceType aMetadataReference = aObjFactory.createServiceMetadataReferenceType ();
        // Ensure that no context is emitted by using "replacePath" first!
        aMetadataReference.setHref (m_aUriInfo.getBaseUriBuilder ()
                                              .replacePath ("")
                                              .path (ServiceMetadataInterface.class)
                                              .buildFromEncoded (IdentifierUtils.getIdentifierURIPercentEncoded (aServiceGroupID),
                                                                 IdentifierUtils.getIdentifierURIPercentEncoded (aDocTypeId))
                                              .toString ());
        aMetadataReferences.add (aMetadataReference);
      }
      aServiceGroup.setServiceMetadataReferenceCollection (aRefCollection);

      final CompleteServiceGroupType aCompleteServiceGroup = aObjFactory.createCompleteServiceGroupType ();
      aCompleteServiceGroup.setServiceGroup (aServiceGroup);
      for (final ServiceMetadataType aService : aDataManager.getServices (aServiceGroupID))
        aCompleteServiceGroup.getServiceMetadata ().add (aService);

      s_aLogger.info ("Finished getServiceGroup(" + sServiceGroupID + ")");

      return aObjFactory.createCompleteServiceGroup (aCompleteServiceGroup);
    }
    catch (final Throwable ex)
    {
      s_aLogger.error ("Error getting service group", ex);
      throw ex;
    }
  }
}
