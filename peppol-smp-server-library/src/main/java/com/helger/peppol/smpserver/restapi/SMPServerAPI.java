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
package com.helger.peppol.smpserver.restapi;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.state.ESuccess;
import com.helger.commons.statistics.IMutableStatisticsHandlerKeyedCounter;
import com.helger.commons.statistics.StatisticsManager;
import com.helger.peppol.identifier.DocumentIdentifierType;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.ParticipantIdentifierType;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smp.CompleteServiceGroupType;
import com.helger.peppol.smp.ServiceGroupReferenceListType;
import com.helger.peppol.smp.ServiceGroupReferenceType;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smp.ServiceInformationType;
import com.helger.peppol.smp.ServiceMetadataReferenceCollectionType;
import com.helger.peppol.smp.ServiceMetadataReferenceType;
import com.helger.peppol.smp.ServiceMetadataType;
import com.helger.peppol.smp.SignedServiceMetadataType;
import com.helger.peppol.smpserver.data.DataManagerFactory;
import com.helger.peppol.smpserver.data.IDataManagerSPI;
import com.helger.peppol.smpserver.data.IDataUser;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * This class implements all the service methods, that must be provided by the
 * REST service.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class SMPServerAPI
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPServerAPI.class);
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterCall = StatisticsManager.getKeyedCounterHandler (SMPServerAPI.class.getName () +
                                                                                                                             "$call");
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterSuccess = StatisticsManager.getKeyedCounterHandler (SMPServerAPI.class.getName () +
                                                                                                                                "$success");
  private final ISMPServerAPIDataProvider m_aDataProvider;

  public SMPServerAPI (@Nonnull final ISMPServerAPIDataProvider aDataProvider)
  {
    m_aDataProvider = ValueEnforcer.notNull (aDataProvider, "DataProvider");
  }

  @Nonnull
  public CompleteServiceGroupType getCompleteServiceGroup (final String sServiceGroupID) throws Throwable
  {
    s_aLogger.info ("getCompleteServiceGroup - GET /complete/" + sServiceGroupID);
    s_aStatsCounterCall.increment ("getCompleteServiceGroup");

    final SimpleParticipantIdentifier aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      throw new SMPNotFoundException ("Failed to parse serviceGroup '" +
                                      sServiceGroupID +
                                      "'",
                                      m_aDataProvider.getCurrentURI ());
    }

    final IDataManagerSPI aDataManager = DataManagerFactory.getInstance ();
    final ServiceGroupType aServiceGroup = aDataManager.getServiceGroup (aServiceGroupID);
    if (aServiceGroup == null)
    {
      // No such service group
      throw new SMPNotFoundException ("Unknown serviceGroup '" +
                                      sServiceGroupID +
                                      "'",
                                      m_aDataProvider.getCurrentURI ());
    }

    /*
     * Then add the service metadata references
     */
    final ServiceMetadataReferenceCollectionType aRefCollection = new ServiceMetadataReferenceCollectionType ();
    final List <ServiceMetadataReferenceType> aMetadataReferences = aRefCollection.getServiceMetadataReference ();

    final List <DocumentIdentifierType> aDocTypeIDs = aDataManager.getDocumentTypes (aServiceGroupID);
    for (final DocumentIdentifierType aDocTypeID : aDocTypeIDs)
    {
      final ServiceMetadataReferenceType aMetadataReference = new ServiceMetadataReferenceType ();
      aMetadataReference.setHref (m_aDataProvider.getServiceMetadataReferenceHref (aServiceGroupID, aDocTypeID));
      aMetadataReferences.add (aMetadataReference);
    }
    aServiceGroup.setServiceMetadataReferenceCollection (aRefCollection);

    final CompleteServiceGroupType aCompleteServiceGroup = new CompleteServiceGroupType ();
    aCompleteServiceGroup.setServiceGroup (aServiceGroup);
    for (final ServiceMetadataType aService : aDataManager.getServices (aServiceGroupID))
      aCompleteServiceGroup.getServiceMetadata ().add (aService);

    s_aLogger.info ("Finished getCompleteServiceGroup(" + sServiceGroupID + ")");
    s_aStatsCounterSuccess.increment ("getCompleteServiceGroup");
    return aCompleteServiceGroup;
  }

  @Nonnull
  public ServiceGroupReferenceListType getServiceGroupReferenceList (@Nonnull final String sUserID,
                                                                     @Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    s_aLogger.info ("getServiceGroupReferenceList - GET /list/" + sUserID);
    s_aStatsCounterCall.increment ("getServiceGroupReferenceList");

    if (!aCredentials.getUserName ().equals (sUserID))
    {
      throw new SMPUnauthorizedException ("URL user name '" +
                                          sUserID +
                                          "' does not match HTTP Basic Auth user name '" +
                                          aCredentials.getUserName () +
                                          "'");
    }

    final IDataManagerSPI aDataManager = DataManagerFactory.getInstance ();
    final IDataUser aDataUser = aDataManager.getUserFromCredentials (aCredentials);
    final Collection <ParticipantIdentifierType> aServiceGroupList = aDataManager.getServiceGroupList (aDataUser);

    final ServiceGroupReferenceListType aRefList = new ServiceGroupReferenceListType ();
    final List <ServiceGroupReferenceType> aReferenceTypes = aRefList.getServiceGroupReference ();
    for (final IParticipantIdentifier aServiceGroupID : aServiceGroupList)
    {
      final String sHref = m_aDataProvider.getServiceGroupHref (aServiceGroupID);

      final ServiceGroupReferenceType aServGroupRefType = new ServiceGroupReferenceType ();
      aServGroupRefType.setHref (sHref);
      aReferenceTypes.add (aServGroupRefType);
    }

    s_aLogger.info ("Finished getServiceGroupReferenceList(" + sUserID + ")");
    s_aStatsCounterSuccess.increment ("getServiceGroupReferenceList");
    return aRefList;
  }

  @Nonnull
  public ServiceGroupType getServiceGroup (final String sServiceGroupID) throws Throwable
  {
    s_aLogger.info ("getServiceGroup - GET /" + sServiceGroupID);
    s_aStatsCounterCall.increment ("getServiceGroup");

    final ParticipantIdentifierType aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      throw new SMPNotFoundException ("Failed to parse serviceGroup '" +
                                      sServiceGroupID +
                                      "'",
                                      m_aDataProvider.getCurrentURI ());
    }

    // Retrieve the service group
    final IDataManagerSPI aDataManager = DataManagerFactory.getInstance ();
    final ServiceGroupType aServiceGroup = aDataManager.getServiceGroup (aServiceGroupID);
    if (aServiceGroup == null)
    {
      // No such service group
      throw new SMPNotFoundException ("Unknown serviceGroup '" +
                                      sServiceGroupID +
                                      "'",
                                      m_aDataProvider.getCurrentURI ());
    }

    // Then add the service metadata references
    final ServiceMetadataReferenceCollectionType aCollectionType = new ServiceMetadataReferenceCollectionType ();
    final List <ServiceMetadataReferenceType> aMetadataReferences = aCollectionType.getServiceMetadataReference ();

    final List <DocumentIdentifierType> aDocTypeIDs = aDataManager.getDocumentTypes (aServiceGroupID);
    for (final DocumentIdentifierType aDocTypeID : aDocTypeIDs)
    {
      final ServiceMetadataReferenceType aMetadataReference = new ServiceMetadataReferenceType ();
      aMetadataReference.setHref (m_aDataProvider.getServiceMetadataReferenceHref (aServiceGroupID, aDocTypeID));
      aMetadataReferences.add (aMetadataReference);
    }
    aServiceGroup.setServiceMetadataReferenceCollection (aCollectionType);

    s_aLogger.info ("Finished getServiceGroup(" + sServiceGroupID + ")");
    s_aStatsCounterSuccess.increment ("getServiceGroup");
    return aServiceGroup;
  }

  @Nonnull
  public ESuccess saveServiceGroup (@Nonnull final String sServiceGroupID,
                                    @Nonnull final ServiceGroupType aServiceGroup,
                                    @Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    s_aLogger.info ("saveServiceGroup - PUT /" + sServiceGroupID + " ==> " + aServiceGroup);
    s_aStatsCounterCall.increment ("saveServiceGroup");

    final ParticipantIdentifierType aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      throw new SMPNotFoundException ("Failed to parse serviceGroup '" +
                                      sServiceGroupID +
                                      "'",
                                      m_aDataProvider.getCurrentURI ());
    }

    if (!IdentifierHelper.areParticipantIdentifiersEqual (aServiceGroupID, aServiceGroup.getParticipantIdentifier ()))
    {
      // Business identifiers must be equal
      throw new SMPNotFoundException ("ServiceGroup inconsistency", m_aDataProvider.getCurrentURI ());
    }

    final IDataManagerSPI aDataManager = DataManagerFactory.getInstance ();
    final IDataUser aDataUser = aDataManager.getUserFromCredentials (aCredentials);
    aDataManager.saveServiceGroup (aServiceGroup, aDataUser);

    s_aLogger.info ("Finished saveServiceGroup(" + sServiceGroupID + "," + aServiceGroup + ")");
    s_aStatsCounterSuccess.increment ("saveServiceGroup");
    return ESuccess.SUCCESS;
  }

  @Nonnull
  public ESuccess deleteServiceGroup (@Nonnull final String sServiceGroupID,
                                      @Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    s_aLogger.info ("deleteServiceGroup - DELETE /" + sServiceGroupID);
    s_aStatsCounterCall.increment ("deleteServiceGroup");

    final ParticipantIdentifierType aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse participant identifier '" + sServiceGroupID + "'");
      return ESuccess.FAILURE;
    }

    final IDataManagerSPI aDataManager = DataManagerFactory.getInstance ();
    final IDataUser aDataUser = aDataManager.getUserFromCredentials (aCredentials);
    aDataManager.deleteServiceGroup (aServiceGroupID, aDataUser);

    s_aLogger.info ("Finished deleteServiceGroup(" + sServiceGroupID + ")");
    s_aStatsCounterSuccess.increment ("deleteServiceGroup");
    return ESuccess.SUCCESS;
  }

  @Nonnull
  public SignedServiceMetadataType getServiceRegistration (@Nonnull final String sServiceGroupID,
                                                           @Nonnull final String sDocumentTypeID) throws Throwable
  {
    s_aLogger.info ("getServiceRegistration - GET /" + sServiceGroupID + "/services/" + sDocumentTypeID);
    s_aStatsCounterCall.increment ("getServiceRegistration");

    final ParticipantIdentifierType aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      throw new SMPNotFoundException ("Failed to parse serviceGroup '" +
                                      sServiceGroupID +
                                      "'",
                                      m_aDataProvider.getCurrentURI ());
    }

    final DocumentIdentifierType aDocTypeID = IdentifierHelper.createDocumentTypeIdentifierFromURIPartOrNull (sDocumentTypeID);
    if (aDocTypeID == null)
    {
      throw new SMPNotFoundException ("Failed to parse documentTypeID '" +
                                      sServiceGroupID +
                                      "'",
                                      m_aDataProvider.getCurrentURI ());
    }

    final IDataManagerSPI aDataManager = DataManagerFactory.getInstance ();

    // First check for redirection, then for actual service
    ServiceMetadataType aService = aDataManager.getRedirection (aServiceGroupID, aDocTypeID);
    if (aService == null)
    {
      // Get as regular service information
      aService = aDataManager.getService (aServiceGroupID, aDocTypeID);
      if (aService == null)
      {
        // Neither nor is present
        throw new SMPNotFoundException ("service(" +
                                        sServiceGroupID +
                                        "," +
                                        sDocumentTypeID +
                                        ")",
                                        m_aDataProvider.getCurrentURI ());
      }
    }

    final SignedServiceMetadataType aSignedServiceMetadata = new SignedServiceMetadataType ();
    aSignedServiceMetadata.setServiceMetadata (aService);
    // Signature is added by a handler

    s_aLogger.info ("Finished getServiceRegistration(" + sServiceGroupID + "," + sDocumentTypeID + ")");
    s_aStatsCounterSuccess.increment ("getServiceRegistration");
    return aSignedServiceMetadata;
  }

  @Nonnull
  public ESuccess saveServiceRegistration (@Nonnull final String sServiceGroupID,
                                           @Nonnull final String sDocumentTypeID,
                                           @Nonnull final ServiceMetadataType aServiceMetadata,
                                           @Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    s_aLogger.info ("saveServiceRegistration - PUT /" +
                    sServiceGroupID +
                    "/services/" +
                    sDocumentTypeID +
                    " ==> " +
                    aServiceMetadata);
    s_aStatsCounterCall.increment ("saveServiceRegistration");

    final SimpleParticipantIdentifier aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse participant identifier '" + sServiceGroupID + "'");
      return ESuccess.FAILURE;
    }

    final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createFromURIPartOrNull (sDocumentTypeID);
    if (aDocTypeID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse document type identifier '" + sDocumentTypeID + "'");
      return ESuccess.FAILURE;
    }

    final ServiceInformationType aServiceInformationType = aServiceMetadata.getServiceInformation ();

    // Business identifiers from path (ServiceGroupID) and from service
    // metadata (body) must equal path
    if (!IdentifierHelper.areParticipantIdentifiersEqual (aServiceInformationType.getParticipantIdentifier (),
                                                          aServiceGroupID))
    {
      s_aLogger.info ("Save service metadata was called with bad parameters. serviceInfo:" +
                      IdentifierHelper.getIdentifierURIEncoded (aServiceInformationType.getParticipantIdentifier ()) +
                      " param:" +
                      aServiceGroupID);
      return ESuccess.FAILURE;
    }

    if (!IdentifierHelper.areDocumentTypeIdentifiersEqual (aServiceInformationType.getDocumentIdentifier (),
                                                           aDocTypeID))
    {
      s_aLogger.info ("Save service metadata was called with bad parameters. serviceInfo:" +
                      IdentifierHelper.getIdentifierURIEncoded (aServiceInformationType.getDocumentIdentifier ()) +
                      " param:" +
                      aDocTypeID);
      // Document type must equal path
      return ESuccess.FAILURE;
    }

    // Main save
    final IDataManagerSPI aDataManager = DataManagerFactory.getInstance ();
    final IDataUser aDataUser = aDataManager.getUserFromCredentials (aCredentials);
    aDataManager.saveService (aServiceMetadata.getServiceInformation (), aDataUser);

    s_aLogger.info ("Finished saveServiceRegistration(" +
                    sServiceGroupID +
                    "," +
                    sDocumentTypeID +
                    "," +
                    aServiceMetadata +
                    ")");
    s_aStatsCounterSuccess.increment ("saveServiceRegistration");
    return ESuccess.SUCCESS;
  }

  @Nonnull
  public ESuccess deleteServiceRegistration (@Nonnull final String sServiceGroupID,
                                             @Nonnull final String sDocumentTypeID,
                                             @Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    s_aLogger.info ("deleteServiceRegistration - DELETE /" + sServiceGroupID + "/services/" + sDocumentTypeID);
    s_aStatsCounterCall.increment ("deleteServiceRegistration");

    final SimpleParticipantIdentifier aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse participant identifier '" + sServiceGroupID + "'");
      return ESuccess.FAILURE;
    }

    final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createFromURIPartOrNull (sDocumentTypeID);
    if (aDocTypeID == null)
    {
      // Invalid identifier
      s_aLogger.info ("Failed to parse document type identifier '" + sDocumentTypeID + "'");
      return ESuccess.FAILURE;
    }

    final IDataManagerSPI aDataManager = DataManagerFactory.getInstance ();
    final IDataUser aDataUser = aDataManager.getUserFromCredentials (aCredentials);
    aDataManager.deleteService (aServiceGroupID, aDocTypeID, aDataUser);

    s_aLogger.info ("Finished deleteServiceRegistration(" + sServiceGroupID + "," + sDocumentTypeID);
    s_aStatsCounterSuccess.increment ("deleteServiceRegistration");
    return ESuccess.SUCCESS;
  }
}
