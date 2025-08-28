/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.restapi;

import java.security.cert.X509Certificate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.codec.base64.Base64;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.rt.BooleanHelper;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.photon.security.user.IUser;
import com.helger.smpclient.extension.SMPExtensionList;
import com.helger.statistics.api.IMutableStatisticsHandlerKeyedCounter;
import com.helger.statistics.api.IStatisticsHandlerKeyedCounter;
import com.helger.statistics.impl.StatisticsManager;
import com.helger.xsds.bdxr.smp1.CompleteServiceGroupType;
import com.helger.xsds.bdxr.smp1.EndpointType;
import com.helger.xsds.bdxr.smp1.ProcessListType;
import com.helger.xsds.bdxr.smp1.ProcessType;
import com.helger.xsds.bdxr.smp1.ServiceGroupReferenceListType;
import com.helger.xsds.bdxr.smp1.ServiceGroupReferenceType;
import com.helger.xsds.bdxr.smp1.ServiceGroupType;
import com.helger.xsds.bdxr.smp1.ServiceInformationType;
import com.helger.xsds.bdxr.smp1.ServiceMetadataReferenceCollectionType;
import com.helger.xsds.bdxr.smp1.ServiceMetadataReferenceType;
import com.helger.xsds.bdxr.smp1.ServiceMetadataType;
import com.helger.xsds.bdxr.smp1.SignedServiceMetadataType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This class implements all the service methods, that must be provided by the OASIS BDXR SMP v1
 * REST service.
 *
 * @author Philip Helger
 */
public final class BDXR1ServerAPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BDXR1ServerAPI.class);
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_INVOCATION = StatisticsManager.getKeyedCounterHandler (BDXR1ServerAPI.class.getName () +
                                                                                                                                  "$call");
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_SUCCESS = StatisticsManager.getKeyedCounterHandler (BDXR1ServerAPI.class.getName () +
                                                                                                                               "$success");
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_ERROR = StatisticsManager.getKeyedCounterHandler (BDXR1ServerAPI.class.getName () +
                                                                                                                             "$error");
  private static final String LOG_PREFIX = "[BDXR1 REST API] ";

  private final ISMPServerAPIDataProvider m_aAPIDataProvider;

  public BDXR1ServerAPI (@Nonnull final ISMPServerAPIDataProvider aDataProvider)
  {
    m_aAPIDataProvider = ValueEnforcer.notNull (aDataProvider, "DataProvider");
  }

  @Nullable
  public static String convertToJsonString (@Nullable final List <com.helger.xsds.bdxr.smp1.ExtensionType> aExtensions)
  {
    final SMPExtensionList ret = SMPExtensionList.ofBDXR1 (aExtensions);
    return ret == null ? null : ret.getExtensionsAsJsonString ();
  }

  @Nonnull
  public CompleteServiceGroupType getCompleteServiceGroup (final String sPathServiceGroupID) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /complete/" + sPathServiceGroupID;
    final String sAction = "getCompleteServiceGroup";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aPathServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aPathServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIDataProvider.getCurrentURI ());
      }

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
      final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();

      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aPathServiceGroupID);
      if (aServiceGroup == null)
      {
        // No such service group
        throw new SMPNotFoundException ("Unknown Service Group ID '" + sPathServiceGroupID + "'",
                                        m_aAPIDataProvider.getCurrentURI ());
      }

      // Then add the service metadata references
      final ServiceMetadataReferenceCollectionType aRefCollection = new ServiceMetadataReferenceCollectionType ();
      for (final IDocumentTypeIdentifier aDocTypeID : aServiceInfoMgr.getAllSMPDocumentTypesOfServiceGroup (aPathServiceGroupID))
      {
        // Ignore all service information without endpoints
        final ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPathServiceGroupID,
                                                                                                                           aDocTypeID);
        if (aServiceInfo != null && aServiceInfo.getTotalEndpointCount () > 0)
        {
          final ServiceMetadataReferenceType aMetadataReference = new ServiceMetadataReferenceType ();
          aMetadataReference.setHref (m_aAPIDataProvider.getServiceMetadataReferenceHref (aPathServiceGroupID,
                                                                                          aDocTypeID));
          aRefCollection.addServiceMetadataReference (aMetadataReference);
        }
      }

      // Now add all redirects
      for (final ISMPRedirect aRedirect : aRedirectMgr.getAllSMPRedirectsOfServiceGroup (aPathServiceGroupID))
      {
        final ServiceMetadataReferenceType aMetadataReference = new ServiceMetadataReferenceType ();
        aMetadataReference.setHref (m_aAPIDataProvider.getServiceMetadataReferenceHref (aPathServiceGroupID,
                                                                                        aRedirect.getDocumentTypeIdentifier ()));
        aRefCollection.addServiceMetadataReference (aMetadataReference);
      }

      final ServiceGroupType aSG = aServiceGroup.getAsJAXBObjectBDXR1 ();
      aSG.setServiceMetadataReferenceCollection (aRefCollection);

      // a CompleteSG may be empty
      final CompleteServiceGroupType aCompleteServiceGroup = new CompleteServiceGroupType ();
      aCompleteServiceGroup.setServiceGroup (aSG);

      for (final ISMPServiceInformation aServiceInfo : aServiceInfoMgr.getAllSMPServiceInformationOfServiceGroup (aPathServiceGroupID))
      {
        final ServiceMetadataType aSM = aServiceInfo.getAsJAXBObjectBDXR1 ();
        if (aSM != null)
          aCompleteServiceGroup.addServiceMetadata (aSM);
      }

      LOGGER.info (sLog + " SUCCESS");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return aCompleteServiceGroup;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  @Nonnull
  public ServiceGroupReferenceListType getServiceGroupReferenceList (@Nonnull final String sPathUserID,
                                                                     @Nonnull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /list/" + sPathUserID;
    final String sAction = "getServiceGroupReferenceList";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);

    try
    {
      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);

      if (!aSMPUser.getLoginName ().equals (sPathUserID))
      {
        throw new SMPUnauthorizedException ("URL user name '" +
                                            sPathUserID +
                                            "' does not match the user name '" +
                                            aSMPUser.getLoginName () +
                                            "' derived from the credentials",
                                            m_aAPIDataProvider.getCurrentURI ());
      }

      final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
      final ICommonsList <ISMPServiceGroup> aServiceGroups = aSGMgr.getAllSMPServiceGroupsOfOwner (aSMPUser.getID ());

      final ServiceGroupReferenceListType aRefList = new ServiceGroupReferenceListType ();
      for (final ISMPServiceGroup aServiceGroup : aServiceGroups)
      {
        final String sHref = m_aAPIDataProvider.getServiceGroupHref (aServiceGroup.getParticipantIdentifier ());

        final ServiceGroupReferenceType aServGroupRefType = new ServiceGroupReferenceType ();
        aServGroupRefType.setHref (sHref);
        aRefList.addServiceGroupReference (aServGroupRefType);
      }

      LOGGER.info (sLog + " SUCCESS");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return aRefList;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  @Nonnull
  public ServiceGroupType getServiceGroup (final String sPathServiceGroupID) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /" + sPathServiceGroupID;
    final String sAction = "getServiceGroup";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aPathServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aPathServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIDataProvider.getCurrentURI ());
      }

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
      final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();

      // Retrieve the service group
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aPathServiceGroupID);
      if (aServiceGroup == null)
      {
        // No such service group
        throw new SMPNotFoundException ("Unknown Service Group '" + sPathServiceGroupID + "'",
                                        m_aAPIDataProvider.getCurrentURI ());
      }

      // Then add the service metadata references
      final ServiceGroupType aSG = aServiceGroup.getAsJAXBObjectBDXR1 ();
      final ServiceMetadataReferenceCollectionType aRefCollection = new ServiceMetadataReferenceCollectionType ();
      for (final IDocumentTypeIdentifier aDocTypeID : aServiceInfoMgr.getAllSMPDocumentTypesOfServiceGroup (aPathServiceGroupID))
      {
        // Ignore all service information without endpoints
        final ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPathServiceGroupID,
                                                                                                                           aDocTypeID);
        if (aServiceInfo != null && aServiceInfo.getTotalEndpointCount () > 0)
        {
          final ServiceMetadataReferenceType aMetadataReference = new ServiceMetadataReferenceType ();
          aMetadataReference.setHref (m_aAPIDataProvider.getServiceMetadataReferenceHref (aPathServiceGroupID,
                                                                                          aDocTypeID));
          aRefCollection.addServiceMetadataReference (aMetadataReference);
        }
      }

      // Now add all redirects
      for (final ISMPRedirect aRedirect : aRedirectMgr.getAllSMPRedirectsOfServiceGroup (aPathServiceGroupID))
      {
        final ServiceMetadataReferenceType aMetadataReference = new ServiceMetadataReferenceType ();
        aMetadataReference.setHref (m_aAPIDataProvider.getServiceMetadataReferenceHref (aPathServiceGroupID,
                                                                                        aRedirect.getDocumentTypeIdentifier ()));
        aRefCollection.addServiceMetadataReference (aMetadataReference);
      }

      aSG.setServiceMetadataReferenceCollection (aRefCollection);

      LOGGER.info (sLog + " SUCCESS");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return aSG;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  public void saveServiceGroup (@Nonnull final String sPathServiceGroupID,
                                @Nonnull final ServiceGroupType aServiceGroup,
                                final boolean bCreateInSML,
                                @Nonnull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX +
                        "PUT /" +
                        sPathServiceGroupID +
                        (bCreateInSML ? "" : CSMPServer.LOG_SUFFIX_NO_SML_INTERACTION);
    final String sAction = "saveServiceGroup";

    LOGGER.info (sLog + " ==> " + aServiceGroup);
    STATS_COUNTER_INVOCATION.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aPathServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aPathServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIDataProvider.getCurrentURI ());
      }

      // Parse the content of the payload with the same identifier factory to
      // ensure same case sensitivity
      final IParticipantIdentifier aPayloadServiceGroupID;
      if (aServiceGroup.getParticipantIdentifier () == null)
      {
        // Can happen when tampering with the input data
        aPayloadServiceGroupID = null;
      }
      else
      {
        aPayloadServiceGroupID = aIdentifierFactory.createParticipantIdentifier (aServiceGroup.getParticipantIdentifier ()
                                                                                              .getScheme (),
                                                                                 aServiceGroup.getParticipantIdentifier ()
                                                                                              .getValue ());
      }
      if (!aPathServiceGroupID.hasSameContent (aPayloadServiceGroupID))
      {
        // Business identifiers must be equal
        throw new SMPBadRequestException ("Service Group Inconsistency. The URL points to '" +
                                          aPathServiceGroupID.getURIEncoded () +
                                          "' whereas the Service Group contains " +
                                          (aPayloadServiceGroupID == null ? "<none>" : "'" +
                                                                                       aPayloadServiceGroupID.getURIEncoded () +
                                                                                       "'"),
                                          m_aAPIDataProvider.getCurrentURI ());
      }

      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final String sExtension = convertToJsonString (aServiceGroup.getExtension ());
      if (aServiceGroupMgr.containsSMPServiceGroupWithID (aPathServiceGroupID))
        aServiceGroupMgr.updateSMPServiceGroup (aPathServiceGroupID, aSMPUser.getID (), sExtension);
      else
        aServiceGroupMgr.createSMPServiceGroup (aSMPUser.getID (), aPathServiceGroupID, sExtension, bCreateInSML);

      LOGGER.info (sLog + " SUCCESS");
      STATS_COUNTER_SUCCESS.increment (sAction);
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  @Nonnull
  public EChange deleteServiceGroup (@Nonnull final String sPathServiceGroupID,
                                     final boolean bDeleteInSML,
                                     @Nonnull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX +
                        "DELETE /" +
                        sPathServiceGroupID +
                        (bDeleteInSML ? "" : CSMPServer.LOG_SUFFIX_NO_SML_INTERACTION);
    final String sAction = "deleteServiceGroup";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aPathServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aPathServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIDataProvider.getCurrentURI ());
      }

      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
      SMPUserManagerPhoton.verifyOwnership (aPathServiceGroupID, aSMPUser);

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final EChange eDeleted = aServiceGroupMgr.deleteSMPServiceGroup (aPathServiceGroupID, bDeleteInSML);

      LOGGER.info (sLog + " SUCCESS");
      STATS_COUNTER_SUCCESS.increment (sAction);

      return eDeleted;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  @Nonnull
  public SignedServiceMetadataType getServiceRegistration (@Nonnull final String sPathServiceGroupID,
                                                           @Nonnull final String sPathDocTypeID) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /" + sPathServiceGroupID + "/services/" + sPathDocTypeID;
    final String sAction = "getServiceRegistration";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aPathServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aPathServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIDataProvider.getCurrentURI ());
      }

      final ISMPServiceGroup aPathServiceGroup = SMPMetaManager.getServiceGroupMgr ()
                                                               .getSMPServiceGroupOfID (aPathServiceGroupID);
      if (aPathServiceGroup == null)
      {
        throw new SMPNotFoundException ("No such Service Group '" + sPathServiceGroupID + "'",
                                        m_aAPIDataProvider.getCurrentURI ());
      }

      final IDocumentTypeIdentifier aPathDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sPathDocTypeID);
      if (aPathDocTypeID == null)
      {
        throw SMPBadRequestException.failedToParseDocType (sPathDocTypeID, m_aAPIDataProvider.getCurrentURI ());
      }

      // First check for redirection, then for actual service
      final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
      final ISMPRedirect aRedirect = aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPathServiceGroupID,
                                                                                               aPathDocTypeID);

      final SignedServiceMetadataType aSignedServiceMetadata = new SignedServiceMetadataType ();
      if (aRedirect != null)
      {
        aSignedServiceMetadata.setServiceMetadata (aRedirect.getAsJAXBObjectBDXR1 ());
      }
      else
      {
        // Get as regular service information
        final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
        final ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPathServiceGroupID,
                                                                                                                           aPathDocTypeID);
        final ServiceMetadataType aSM = aServiceInfo == null ? null : aServiceInfo.getAsJAXBObjectBDXR1 ();
        if (aSM != null)
        {
          aSignedServiceMetadata.setServiceMetadata (aSM);
        }
        else
        {
          // Neither nor is present, or no endpoint is available
          throw new SMPNotFoundException ("service(" + sPathServiceGroupID + "," + sPathDocTypeID + ")",
                                          m_aAPIDataProvider.getCurrentURI ());
        }
      }

      // Signature must be added by the rest service

      LOGGER.info (sLog + " SUCCESS");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return aSignedServiceMetadata;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  @Nonnull
  public ESuccess saveServiceRegistration (@Nonnull final String sPathServiceGroupID,
                                           @Nonnull final String sPathDocumentTypeID,
                                           @Nonnull final ServiceMetadataType aServiceMetadata,
                                           @Nonnull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "PUT /" + sPathServiceGroupID + "/services/" + sPathDocumentTypeID;
    final String sAction = "saveServiceRegistration";

    LOGGER.info (sLog + " ==> " + aServiceMetadata);
    STATS_COUNTER_INVOCATION.increment (sAction);

    try
    {
      // Parse provided identifiers
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aPathServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aPathServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIDataProvider.getCurrentURI ());
      }

      final IDocumentTypeIdentifier aPathDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sPathDocumentTypeID);
      if (aPathDocTypeID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseDocType (sPathDocumentTypeID, m_aAPIDataProvider.getCurrentURI ());
      }

      // May be null for a Redirect!
      final ServiceInformationType aServiceInformation = aServiceMetadata.getServiceInformation ();
      if (aServiceInformation != null)
      {
        // Business identifiers from path (ServiceGroupID) and from service
        // metadata (body) must equal path
        if (aServiceInformation.getParticipantIdentifier () == null)
        {
          // Can happen when tampering with the input data
          throw new SMPBadRequestException ("Save Service Metadata has inconsistent values.\n" +
                                            "Service Information Participant ID: <none>\n" +
                                            "URL Parameter value: '" +
                                            aPathServiceGroupID.getURIEncoded () +
                                            "'",
                                            m_aAPIDataProvider.getCurrentURI ());
        }
        final IParticipantIdentifier aPayloadServiceGroupID = aIdentifierFactory.createParticipantIdentifier (aServiceInformation.getParticipantIdentifier ()
                                                                                                                                 .getScheme (),
                                                                                                              aServiceInformation.getParticipantIdentifier ()
                                                                                                                                 .getValue ());

        if (!aPathServiceGroupID.hasSameContent (aPayloadServiceGroupID))
        {
          // Participant ID in URL must match the one in XML structure
          throw new SMPBadRequestException ("Save Service Metadata was called with inconsistent values.\n" +
                                            "Service Infoformation Participant ID: " +
                                            (aPayloadServiceGroupID == null ? "<none>" : "'" +
                                                                                         aPayloadServiceGroupID.getURIEncoded () +
                                                                                         "'") +
                                            "\n" +
                                            "URL parameter value: '" +
                                            aPathServiceGroupID.getURIEncoded () +
                                            "'",
                                            m_aAPIDataProvider.getCurrentURI ());
        }

        if (aServiceInformation.getDocumentIdentifier () == null)
        {
          throw new SMPBadRequestException ("Save Service Metadata was called with inconsistent values.\n" +
                                            "Service Information Document Type ID: <none>\n" +
                                            "URL parameter value: '" +
                                            aPathDocTypeID.getURIEncoded () +
                                            "'",
                                            m_aAPIDataProvider.getCurrentURI ());
        }
        final IDocumentTypeIdentifier aPayloadDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (aServiceInformation.getDocumentIdentifier ()
                                                                                                                              .getScheme (),
                                                                                                           aServiceInformation.getDocumentIdentifier ()
                                                                                                                              .getValue ());
        if (!aPathDocTypeID.hasSameContent (aPayloadDocTypeID))
        {
          // Document type ID in URL must match the one in XML structure
          throw new SMPBadRequestException ("Save Service Metadata was called with inconsistent values.\n" +
                                            "Service Information Document Type ID: '" +
                                            aPayloadDocTypeID.getURIEncoded () +
                                            "'\n" +
                                            "URL parameter value: '" +
                                            aPathDocTypeID.getURIEncoded () +
                                            "'",
                                            m_aAPIDataProvider.getCurrentURI ());
        }
      }

      // Main save
      final IUser aDataUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
      SMPUserManagerPhoton.verifyOwnership (aPathServiceGroupID, aDataUser);

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aPathServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aPathServiceGroupID);
      if (aPathServiceGroup == null)
      {
        // Service group not found
        throw new SMPNotFoundException ("Service Group '" + sPathServiceGroupID + "' is not on this SMP",
                                        m_aAPIDataProvider.getCurrentURI ());
      }

      if (aServiceMetadata.getRedirect () != null)
      {
        // Handle redirect
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        // not available in OASIS BDXR SMP v1 mode
        final X509Certificate aCertificate = null;
        if (aRedirectMgr.createOrUpdateSMPRedirect (aPathServiceGroupID,
                                                    aPathDocTypeID,
                                                    aServiceMetadata.getRedirect ().getHref (),
                                                    aServiceMetadata.getRedirect ().getCertificateUID (),
                                                    aCertificate,
                                                    convertToJsonString (aServiceMetadata.getRedirect ()
                                                                                         .getExtension ())) == null)
        {
          LOGGER.error (sLog + " - ERROR - Redirect");
          STATS_COUNTER_ERROR.increment (sAction);
          return ESuccess.FAILURE;
        }
        LOGGER.info (sLog + " SUCCESS - Redirect");
      }
      else
        if (aServiceInformation != null)
        {
          // Handle service information
          final ProcessListType aJAXBProcesses = aServiceInformation.getProcessList ();
          final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList <> ();
          for (final ProcessType aJAXBProcess : aJAXBProcesses.getProcess ())
          {
            final ICommonsList <SMPEndpoint> aEndpoints = new CommonsArrayList <> ();
            for (final EndpointType aJAXBEndpoint : aJAXBProcess.getServiceEndpointList ().getEndpoint ())
            {
              final SMPEndpoint aEndpoint = new SMPEndpoint (aJAXBEndpoint.getTransportProfile (),
                                                             aJAXBEndpoint.getEndpointURI (),
                                                             BooleanHelper.getBooleanValue (aJAXBEndpoint.isRequireBusinessLevelSignature (),
                                                                                            SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE),
                                                             aJAXBEndpoint.getMinimumAuthenticationLevel (),
                                                             aJAXBEndpoint.getServiceActivationDate (),
                                                             aJAXBEndpoint.getServiceExpirationDate (),
                                                             Base64.encodeBytes (aJAXBEndpoint.getCertificate ()),
                                                             aJAXBEndpoint.getServiceDescription (),
                                                             aJAXBEndpoint.getTechnicalContactUrl (),
                                                             aJAXBEndpoint.getTechnicalInformationUrl (),
                                                             convertToJsonString (aJAXBEndpoint.getExtension ()));
              aEndpoints.add (aEndpoint);
            }
            final SMPProcess aProcess = new SMPProcess (SimpleProcessIdentifier.wrap (aJAXBProcess.getProcessIdentifier ()),
                                                        aEndpoints,
                                                        convertToJsonString (aJAXBProcess.getExtension ()));
            aProcesses.add (aProcess);
          }

          final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
          final String sExtensionXML = convertToJsonString (aServiceInformation.getExtension ());
          if (aServiceInfoMgr.mergeSMPServiceInformation (new SMPServiceInformation (aPathServiceGroup.getParticipantIdentifier (),
                                                                                     aPathDocTypeID,
                                                                                     aProcesses,
                                                                                     sExtensionXML)).isFailure ())
          {
            LOGGER.error (sLog + " - ERROR - ServiceInformation");
            STATS_COUNTER_ERROR.increment (sAction);
            return ESuccess.FAILURE;
          }

          LOGGER.info (sLog + " SUCCESS - ServiceInformation");
        }
        else
        {
          throw new SMPBadRequestException ("Save Service Metadata was called with neither a Redirect nor a ServiceInformation",
                                            m_aAPIDataProvider.getCurrentURI ());
        }

      if (false)
        LOGGER.info (sLog + " SUCCESS");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return ESuccess.SUCCESS;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  public void deleteServiceRegistration (@Nonnull final String sPathServiceGroupID,
                                         @Nonnull final String sPathDocTypeID,
                                         @Nonnull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "DELETE /" + sPathServiceGroupID + "/services/" + sPathDocTypeID;
    final String sAction = "deleteServiceRegistration";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aPathServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aPathServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIDataProvider.getCurrentURI ());
      }

      final IDocumentTypeIdentifier aPathDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sPathDocTypeID);
      if (aPathDocTypeID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseDocType (sPathDocTypeID, m_aAPIDataProvider.getCurrentURI ());
      }

      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
      SMPUserManagerPhoton.verifyOwnership (aPathServiceGroupID, aSMPUser);

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aPathServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aPathServiceGroupID);
      if (aPathServiceGroup == null)
      {
        throw new SMPNotFoundException ("Service Group '" + sPathServiceGroupID + "' is not on this SMP",
                                        m_aAPIDataProvider.getCurrentURI ());
      }

      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
      final ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPathServiceGroupID,
                                                                                                                         aPathDocTypeID);
      if (aServiceInfo != null)
      {
        // Handle service information
        final EChange eChange = aServiceInfoMgr.deleteSMPServiceInformation (aServiceInfo);
        if (eChange.isUnchanged ())
        {
          // Most likely an internal error or an inconsistency
          throw new SMPNotFoundException ("serviceInformation (" +
                                          aPathServiceGroupID.getURIEncoded () +
                                          ", " +
                                          aPathDocTypeID.getURIEncoded () +
                                          ")",
                                          m_aAPIDataProvider.getCurrentURI ());
        }
        LOGGER.info (sLog + " SUCCESS - ServiceInformation");
        STATS_COUNTER_SUCCESS.increment (sAction);
      }
      else
      {
        // No Service Info, so should be a redirect
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        final ISMPRedirect aRedirect = aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPathServiceGroupID,
                                                                                                 aPathDocTypeID);
        if (aRedirect != null)
        {
          // Handle redirect
          final EChange eChange = aRedirectMgr.deleteSMPRedirect (aRedirect);
          if (eChange.isUnchanged ())
          {
            // Most likely an internal error or an inconsistency
            throw new SMPNotFoundException ("redirect(" +
                                            aPathServiceGroupID.getURIEncoded () +
                                            ", " +
                                            aPathDocTypeID.getURIEncoded () +
                                            ")",
                                            m_aAPIDataProvider.getCurrentURI ());
          }
          LOGGER.info (sLog + " SUCCESS - Redirect");
          STATS_COUNTER_SUCCESS.increment (sAction);
        }
        else
        {
          // Neither redirect nor endpoint found
          throw new SMPNotFoundException ("service(" + sPathServiceGroupID + "," + sPathDocTypeID + ")",
                                          m_aAPIDataProvider.getCurrentURI ());
        }
      }
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  public void deleteServiceRegistrations (@Nonnull final String sPathServiceGroupID,
                                          @Nonnull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "DELETE /" + sPathServiceGroupID + "/services/";
    final String sAction = "deleteServiceRegistrations";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aPathServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aPathServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIDataProvider.getCurrentURI ());
      }

      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
      SMPUserManagerPhoton.verifyOwnership (aPathServiceGroupID, aSMPUser);

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aPathServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aPathServiceGroupID);
      if (aPathServiceGroup == null)
      {
        throw new SMPNotFoundException ("Service Group '" + sPathServiceGroupID + "' is not on this SMP",
                                        m_aAPIDataProvider.getCurrentURI ());
      }

      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
      EChange eChange = aServiceInfoMgr.deleteAllSMPServiceInformationOfServiceGroup (aPathServiceGroupID);

      final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
      eChange = eChange.or (aRedirectMgr.deleteAllSMPRedirectsOfServiceGroup (aPathServiceGroupID));

      LOGGER.info (sLog + " SUCCESS - " + eChange);

      STATS_COUNTER_SUCCESS.increment (sAction);
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  /**
   * @return The statistics data with the invocation counter.
   */
  @Nonnull
  public static IStatisticsHandlerKeyedCounter getInvocationCounter ()
  {
    return STATS_COUNTER_INVOCATION;
  }

  /**
   * @return The statistics data with the successful invocation counter.
   */
  @Nonnull
  public static IStatisticsHandlerKeyedCounter getSuccessCounter ()
  {
    return STATS_COUNTER_SUCCESS;
  }

  /**
   * @return The statistics data with the error invocation counter.
   */
  @Nonnull
  public static IStatisticsHandlerKeyedCounter getErrorCounter ()
  {
    return STATS_COUNTER_ERROR;
  }
}
