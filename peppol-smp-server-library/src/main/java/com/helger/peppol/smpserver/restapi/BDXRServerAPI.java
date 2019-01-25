/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.restapi;

import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.base64.Base64;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.lang.BooleanHelper;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.statistics.IMutableStatisticsHandlerKeyedCounter;
import com.helger.commons.statistics.IStatisticsHandlerKeyedCounter;
import com.helger.commons.statistics.StatisticsManager;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.bdxr.BDXRExtensionConverter;
import com.helger.peppol.bdxr.CompleteServiceGroupType;
import com.helger.peppol.bdxr.EndpointType;
import com.helger.peppol.bdxr.ProcessListType;
import com.helger.peppol.bdxr.ProcessType;
import com.helger.peppol.bdxr.ServiceGroupReferenceListType;
import com.helger.peppol.bdxr.ServiceGroupReferenceType;
import com.helger.peppol.bdxr.ServiceGroupType;
import com.helger.peppol.bdxr.ServiceInformationType;
import com.helger.peppol.bdxr.ServiceMetadataReferenceCollectionType;
import com.helger.peppol.bdxr.ServiceMetadataReferenceType;
import com.helger.peppol.bdxr.ServiceMetadataType;
import com.helger.peppol.bdxr.SignedServiceMetadataType;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.exception.SMPBadRequestException;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPServerException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;

/**
 * This class implements all the service methods, that must be provided by the
 * OASIS BDXR SMP v1 REST service.
 *
 * @author Philip Helger
 */
public final class BDXRServerAPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BDXRServerAPI.class);
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterInvocation = StatisticsManager.getKeyedCounterHandler (BDXRServerAPI.class.getName () +
                                                                                                                                   "$call");
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterSuccess = StatisticsManager.getKeyedCounterHandler (BDXRServerAPI.class.getName () +
                                                                                                                                "$success");
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterError = StatisticsManager.getKeyedCounterHandler (BDXRServerAPI.class.getName () +
                                                                                                                              "$error");
  private static final String LOG_PREFIX = "[BDXR REST API] ";

  private final ISMPServerAPIDataProvider m_aAPIProvider;

  public BDXRServerAPI (@Nonnull final ISMPServerAPIDataProvider aDataProvider)
  {
    m_aAPIProvider = ValueEnforcer.notNull (aDataProvider, "DataProvider");
  }

  @Nonnull
  public CompleteServiceGroupType getCompleteServiceGroup (final String sServiceGroupID) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /complete/" + sServiceGroupID;
    final String sAction = "getCompleteServiceGroup";

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLog);
    s_aStatsCounterInvocation.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw new SMPBadRequestException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
                                          m_aAPIProvider.getCurrentURI ());
      }

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
      {
        // No such service group
        throw new SMPNotFoundException ("Unknown serviceGroup '" + sServiceGroupID + "'",
                                        m_aAPIProvider.getCurrentURI ());
      }

      /*
       * Then add the service metadata references
       */
      final ServiceMetadataReferenceCollectionType aRefCollection = new ServiceMetadataReferenceCollectionType ();
      final List <ServiceMetadataReferenceType> aMetadataReferences = aRefCollection.getServiceMetadataReference ();

      for (final IDocumentTypeIdentifier aDocTypeID : aServiceInfoMgr.getAllSMPDocumentTypesOfServiceGroup (aServiceGroup))
      {
        // Ignore all service information without endpoints
        final ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                                           aDocTypeID);
        if (aServiceInfo == null)
          continue;

        final ServiceMetadataReferenceType aMetadataReference = new ServiceMetadataReferenceType ();
        aMetadataReference.setHref (m_aAPIProvider.getServiceMetadataReferenceHref (aServiceGroupID, aDocTypeID));
        aMetadataReferences.add (aMetadataReference);
      }

      final ServiceGroupType aSG = aServiceGroup.getAsJAXBObjectBDXR ();
      aSG.setServiceMetadataReferenceCollection (aRefCollection);

      final CompleteServiceGroupType aCompleteServiceGroup = new CompleteServiceGroupType ();
      aCompleteServiceGroup.setServiceGroup (aSG);

      for (final ISMPServiceInformation aServiceInfo : aServiceInfoMgr.getAllSMPServiceInformationOfServiceGroup (aServiceGroup))
      {
        aCompleteServiceGroup.addServiceMetadata (aServiceInfo.getAsJAXBObjectBDXR ());
      }

      if (LOGGER.isInfoEnabled ())
        LOGGER.info (sLog + " SUCCESS");
      s_aStatsCounterSuccess.increment (sAction);
      return aCompleteServiceGroup;
    }
    catch (final SMPServerException ex)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      s_aStatsCounterError.increment (sAction);
      throw ex;
    }
  }

  @Nonnull
  public ServiceGroupReferenceListType getServiceGroupReferenceList (@Nonnull final String sUserID,
                                                                     @Nonnull final BasicAuthClientCredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /list/" + sUserID;
    final String sAction = "getServiceGroupReferenceList";

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLog);
    s_aStatsCounterInvocation.increment (sAction);

    try
    {
      if (!aCredentials.getUserName ().equals (sUserID))
      {
        throw new SMPUnauthorizedException ("URL user name '" +
                                            sUserID +
                                            "' does not match HTTP Basic Auth user name '" +
                                            aCredentials.getUserName () +
                                            "'");
      }

      final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
      final ISMPUser aSMPUser = aUserMgr.validateUserCredentials (aCredentials);
      final ICommonsList <ISMPServiceGroup> aServiceGroups = SMPMetaManager.getServiceGroupMgr ()
                                                                           .getAllSMPServiceGroupsOfOwner (aSMPUser.getID ());

      final ServiceGroupReferenceListType aRefList = new ServiceGroupReferenceListType ();
      for (final ISMPServiceGroup aServiceGroup : aServiceGroups)
      {
        final String sHref = m_aAPIProvider.getServiceGroupHref (aServiceGroup.getParticpantIdentifier ());

        final ServiceGroupReferenceType aServGroupRefType = new ServiceGroupReferenceType ();
        aServGroupRefType.setHref (sHref);
        aRefList.addServiceGroupReference (aServGroupRefType);
      }

      if (LOGGER.isInfoEnabled ())
        LOGGER.info (sLog + " SUCCESS");
      s_aStatsCounterSuccess.increment (sAction);
      return aRefList;
    }
    catch (final SMPServerException ex)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      s_aStatsCounterError.increment (sAction);
      throw ex;
    }
  }

  @Nonnull
  public ServiceGroupType getServiceGroup (final String sServiceGroupID) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /" + sServiceGroupID;
    final String sAction = "getServiceGroup";

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLog);
    s_aStatsCounterInvocation.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw new SMPBadRequestException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
                                          m_aAPIProvider.getCurrentURI ());
      }

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

      // Retrieve the service group
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
      {
        // No such service group
        throw new SMPNotFoundException ("Unknown serviceGroup '" + sServiceGroupID + "'",
                                        m_aAPIProvider.getCurrentURI ());
      }

      // Then add the service metadata references
      final ServiceGroupType aSG = aServiceGroup.getAsJAXBObjectBDXR ();
      final ServiceMetadataReferenceCollectionType aCollectionType = new ServiceMetadataReferenceCollectionType ();
      final List <ServiceMetadataReferenceType> aMetadataReferences = aCollectionType.getServiceMetadataReference ();
      for (final IDocumentTypeIdentifier aDocTypeID : aServiceInfoMgr.getAllSMPDocumentTypesOfServiceGroup (aServiceGroup))
      {
        // Ignore all service information without endpoints
        final ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                                           aDocTypeID);
        if (aServiceInfo == null)
          continue;

        final ServiceMetadataReferenceType aMetadataReference = new ServiceMetadataReferenceType ();
        aMetadataReference.setHref (m_aAPIProvider.getServiceMetadataReferenceHref (aServiceGroupID, aDocTypeID));
        aMetadataReferences.add (aMetadataReference);
      }
      aSG.setServiceMetadataReferenceCollection (aCollectionType);

      if (LOGGER.isInfoEnabled ())
        LOGGER.info (sLog + " SUCCESS");
      s_aStatsCounterSuccess.increment (sAction);
      return aSG;
    }
    catch (final SMPServerException ex)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      s_aStatsCounterError.increment (sAction);
      throw ex;
    }
  }

  public void saveServiceGroup (@Nonnull final String sServiceGroupID,
                                @Nonnull final ServiceGroupType aServiceGroup,
                                @Nonnull final BasicAuthClientCredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "PUT /" + sServiceGroupID;
    final String sAction = "saveServiceGroup";

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLog + " ==> " + aServiceGroup);
    s_aStatsCounterInvocation.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw new SMPBadRequestException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
                                          m_aAPIProvider.getCurrentURI ());
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
      if (!aServiceGroupID.hasSameContent (aPayloadServiceGroupID))
      {
        // Business identifiers must be equal
        throw new SMPBadRequestException ("ServiceGroup Inconsistency. The URL points to " +
                                          aServiceGroupID.getURIEncoded () +
                                          " whereas the ServiceGroup contains " +
                                          (aPayloadServiceGroupID == null ? "!NO PARTICIPANT ID!"
                                                                          : aPayloadServiceGroupID.getURIEncoded ()),
                                          m_aAPIProvider.getCurrentURI ());
      }

      final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
      final ISMPUser aSMPUser = aUserMgr.validateUserCredentials (aCredentials);

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final String sExtension = BDXRExtensionConverter.convertToString (aServiceGroup.getExtension ());
      if (aServiceGroupMgr.containsSMPServiceGroupWithID (aServiceGroupID))
        aServiceGroupMgr.updateSMPServiceGroup (aServiceGroupID, aSMPUser.getID (), sExtension);
      else
        aServiceGroupMgr.createSMPServiceGroup (aSMPUser.getID (), aServiceGroupID, sExtension);

      if (LOGGER.isInfoEnabled ())
        LOGGER.info (sLog + " SUCCESS");
      s_aStatsCounterSuccess.increment (sAction);
    }
    catch (final SMPServerException ex)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      s_aStatsCounterError.increment (sAction);
      throw ex;
    }
  }

  public void deleteServiceGroup (@Nonnull final String sServiceGroupID,
                                  @Nonnull final BasicAuthClientCredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "DELETE /" + sServiceGroupID;
    final String sAction = "deleteServiceGroup";

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLog);
    s_aStatsCounterInvocation.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw new SMPBadRequestException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
                                          m_aAPIProvider.getCurrentURI ());
      }

      final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();

      final ISMPUser aSMPUser = aUserMgr.validateUserCredentials (aCredentials);
      aUserMgr.verifyOwnership (aServiceGroupID, aSMPUser);

      aServiceGroupMgr.deleteSMPServiceGroup (aServiceGroupID);

      if (LOGGER.isInfoEnabled ())
        LOGGER.info (sLog + " SUCCESS");
      s_aStatsCounterSuccess.increment (sAction);
    }
    catch (final SMPServerException ex)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      s_aStatsCounterError.increment (sAction);
      throw ex;
    }
  }

  @Nonnull
  public SignedServiceMetadataType getServiceRegistration (@Nonnull final String sServiceGroupID,
                                                           @Nonnull final String sDocumentTypeID) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /" + sServiceGroupID + "/services/" + sDocumentTypeID;
    final String sAction = "getServiceRegistration";

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLog);
    s_aStatsCounterInvocation.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw new SMPBadRequestException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
                                          m_aAPIProvider.getCurrentURI ());
      }

      final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ()
                                                           .getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
      {
        throw new SMPNotFoundException ("No such serviceGroup '" + sServiceGroupID + "'",
                                        m_aAPIProvider.getCurrentURI ());
      }

      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocumentTypeID);
      if (aDocTypeID == null)
      {
        throw new SMPBadRequestException ("Failed to parse documentTypeID '" + sServiceGroupID + "'",
                                          m_aAPIProvider.getCurrentURI ());
      }

      // First check for redirection, then for actual service
      final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
      final ISMPRedirect aRedirect = aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                               aDocTypeID);

      final SignedServiceMetadataType aSignedServiceMetadata = new SignedServiceMetadataType ();
      if (aRedirect != null)
      {
        aSignedServiceMetadata.setServiceMetadata (aRedirect.getAsJAXBObjectBDXR ());
      }
      else
      {
        // Get as regular service information
        final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
        final ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                                           aDocTypeID);
        if (aServiceInfo != null)
        {
          aSignedServiceMetadata.setServiceMetadata (aServiceInfo.getAsJAXBObjectBDXR ());
        }
        else
        {
          // Neither nor is present
          throw new SMPNotFoundException ("service(" + sServiceGroupID + "," + sDocumentTypeID + ")",
                                          m_aAPIProvider.getCurrentURI ());
        }
      }

      // Signature must be added by the rest service

      if (LOGGER.isInfoEnabled ())
        LOGGER.info (sLog + " SUCCESS");
      s_aStatsCounterSuccess.increment (sAction);
      return aSignedServiceMetadata;
    }
    catch (final SMPServerException ex)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      s_aStatsCounterError.increment (sAction);
      throw ex;
    }
  }

  @Nonnull
  public ESuccess saveServiceRegistration (@Nonnull final String sServiceGroupID,
                                           @Nonnull final String sDocumentTypeID,
                                           @Nonnull final ServiceMetadataType aServiceMetadata,
                                           @Nonnull final BasicAuthClientCredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "PUT /" + sServiceGroupID + "/services/" + sDocumentTypeID;
    final String sAction = "saveServiceRegistration";

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLog + " ==> " + aServiceMetadata);
    s_aStatsCounterInvocation.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw new SMPBadRequestException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
                                          m_aAPIProvider.getCurrentURI ());
      }

      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocumentTypeID);
      if (aDocTypeID == null)
      {
        // Invalid identifier
        throw new SMPBadRequestException ("Failed to parse documentTypeID '" + sDocumentTypeID + "'",
                                          m_aAPIProvider.getCurrentURI ());
      }

      // May be null for a Redirect!
      final ServiceInformationType aServiceInformation = aServiceMetadata.getServiceInformation ();
      if (aServiceInformation != null)
      {
        // Business identifiers from path (ServiceGroupID) and from service
        // metadata (body) must equal path
        if (aServiceInformation.getParticipantIdentifier () == null)
        {
          throw new SMPBadRequestException ("Save service metadata was called with bad parameters. serviceInfo:!NO PARTICIPANT ID! param:" +
                                            aServiceGroupID.getURIEncoded (),
                                            m_aAPIProvider.getCurrentURI ());
        }
        final IParticipantIdentifier aPayloadServiceGroupID;
        if (aServiceInformation.getParticipantIdentifier () == null)
        {
          // Can happen when tampering with the input data
          aPayloadServiceGroupID = null;
        }
        else
        {
          aPayloadServiceGroupID = aIdentifierFactory.createParticipantIdentifier (aServiceInformation.getParticipantIdentifier ()
                                                                                                      .getScheme (),
                                                                                   aServiceInformation.getParticipantIdentifier ()
                                                                                                      .getValue ());
        }

        if (!aServiceGroupID.hasSameContent (aPayloadServiceGroupID))
        {
          // Participant ID in URL must match the one in XML structure
          throw new SMPBadRequestException ("Save service metadata was called with bad parameters. serviceInfo:" +
                                            (aPayloadServiceGroupID == null ? "!NO PARTICIPANT ID!"
                                                                            : aPayloadServiceGroupID.getURIEncoded ()) +
                                            " param:" +
                                            aServiceGroupID.getURIEncoded (),
                                            m_aAPIProvider.getCurrentURI ());
        }

        if (aServiceInformation.getDocumentIdentifier () == null)
        {
          throw new SMPBadRequestException ("Save service metadata was called with bad parameters. serviceInfo:!NO DOCUMENT TYPE ID! param:" +
                                            aDocTypeID.getURIEncoded (),
                                            m_aAPIProvider.getCurrentURI ());
        }
        final IDocumentTypeIdentifier aPayloadDocumentTypeID = aIdentifierFactory.createDocumentTypeIdentifier (aServiceInformation.getDocumentIdentifier ()
                                                                                                                                   .getScheme (),
                                                                                                                aServiceInformation.getDocumentIdentifier ()
                                                                                                                                   .getValue ());
        if (!aDocTypeID.hasSameContent (aPayloadDocumentTypeID))
        {
          // Document type ID in URL must match the one in XML structure
          throw new SMPBadRequestException ("Save service metadata was called with bad parameters. serviceInfo:" +
                                            aPayloadDocumentTypeID.getURIEncoded () +
                                            " param:" +
                                            aDocTypeID.getURIEncoded (),
                                            m_aAPIProvider.getCurrentURI ());
        }
      }

      // Main save
      final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
      final ISMPUser aDataUser = aUserMgr.validateUserCredentials (aCredentials);
      aUserMgr.verifyOwnership (aServiceGroupID, aDataUser);

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
      {
        // Service group not found
        throw new SMPNotFoundException ("No such serviceGroup '" + sServiceGroupID + "'",
                                        m_aAPIProvider.getCurrentURI ());
      }

      if (aServiceMetadata.getRedirect () != null)
      {
        // Handle redirect
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        if (aRedirectMgr.createOrUpdateSMPRedirect (aServiceGroup,
                                                    aDocTypeID,
                                                    aServiceMetadata.getRedirect ().getHref (),
                                                    aServiceMetadata.getRedirect ().getCertificateUID (),
                                                    BDXRExtensionConverter.convertToString (aServiceMetadata.getRedirect ()
                                                                                                            .getExtension ())) == null)
        {
          if (LOGGER.isErrorEnabled ())
            LOGGER.error (sLog + " - Redirect - failure");
          s_aStatsCounterError.increment (sAction);
          return ESuccess.FAILURE;
        }
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
                                                                                            false),
                                                             aJAXBEndpoint.getMinimumAuthenticationLevel (),
                                                             aJAXBEndpoint.getServiceActivationDate (),
                                                             aJAXBEndpoint.getServiceExpirationDate (),
                                                             Base64.encodeBytes (aJAXBEndpoint.getCertificate ()),
                                                             aJAXBEndpoint.getServiceDescription (),
                                                             aJAXBEndpoint.getTechnicalContactUrl (),
                                                             aJAXBEndpoint.getTechnicalInformationUrl (),
                                                             BDXRExtensionConverter.convertToString (aJAXBEndpoint.getExtension ()));
              aEndpoints.add (aEndpoint);
            }
            final SMPProcess aProcess = new SMPProcess (aJAXBProcess.getProcessIdentifier (),
                                                        aEndpoints,
                                                        BDXRExtensionConverter.convertToString (aJAXBProcess.getExtension ()));
            aProcesses.add (aProcess);
          }

          final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
          final String sExtensionXML = BDXRExtensionConverter.convertToString (aServiceInformation.getExtension ());
          if (aServiceInfoMgr.mergeSMPServiceInformation (new SMPServiceInformation (aServiceGroup,
                                                                                     aDocTypeID,
                                                                                     aProcesses,
                                                                                     sExtensionXML))
                             .isFailure ())
          {
            if (LOGGER.isErrorEnabled ())
              LOGGER.error (sLog + " - ServiceInformation - failure");
            s_aStatsCounterError.increment (sAction);
            return ESuccess.FAILURE;
          }
        }
        else
        {
          if (LOGGER.isErrorEnabled ())
            LOGGER.error (sLog + " - neither Redirect nor ServiceInformation");
          s_aStatsCounterError.increment (sAction);
          return ESuccess.FAILURE;
        }

      if (LOGGER.isInfoEnabled ())
        LOGGER.info (sLog + " SUCCESS");
      s_aStatsCounterSuccess.increment (sAction);
      return ESuccess.SUCCESS;
    }
    catch (final SMPServerException ex)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      s_aStatsCounterError.increment (sAction);
      throw ex;
    }
  }

  public void deleteServiceRegistration (@Nonnull final String sServiceGroupID,
                                         @Nonnull final String sDocumentTypeID,
                                         @Nonnull final BasicAuthClientCredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "DELETE /" + sServiceGroupID + "/services/" + sDocumentTypeID;
    final String sAction = "deleteServiceRegistration";

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLog);
    s_aStatsCounterInvocation.increment (sAction);

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw new SMPBadRequestException ("Failed to parse participant identifier '" + sServiceGroupID + "'",
                                          m_aAPIProvider.getCurrentURI ());
      }

      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocumentTypeID);
      if (aDocTypeID == null)
      {
        // Invalid identifier
        throw new SMPBadRequestException ("Failed to parse document type identifier '" + sDocumentTypeID + "'",
                                          m_aAPIProvider.getCurrentURI ());
      }

      final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
      final ISMPUser aSMPUser = aUserMgr.validateUserCredentials (aCredentials);
      aUserMgr.verifyOwnership (aServiceGroupID, aSMPUser);

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
      {
        throw new SMPNotFoundException ("Service group '" + sServiceGroupID + "' not on this SMP");
      }

      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
      final ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                                         aDocTypeID);
      if (aServiceInfo != null)
      {
        // Handle service information
        final EChange eChange = aServiceInfoMgr.deleteSMPServiceInformation (aServiceInfo);
        if (eChange.isUnchanged ())
        {
          // Most likely an internal error or an inconsistency
          throw new SMPNotFoundException ("serviceInformation (" +
                                          aServiceGroupID.getURIEncoded () +
                                          ", " +
                                          aDocTypeID.getURIEncoded () +
                                          ")",
                                          m_aAPIProvider.getCurrentURI ());
        }
        if (LOGGER.isInfoEnabled ())
          LOGGER.info (sLog + " SUCCESS - ServiceInformation");
        s_aStatsCounterSuccess.increment (sAction);
      }
      else
      {
        // No Service Info, so should be a redirect
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        final ISMPRedirect aRedirect = aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                 aDocTypeID);
        if (aRedirect != null)
        {
          // Handle redirect
          final EChange eChange = aRedirectMgr.deleteSMPRedirect (aRedirect);
          if (eChange.isUnchanged ())
          {
            // Most likely an internal error or an inconsistency
            throw new SMPNotFoundException ("redirect(" +
                                            aServiceGroupID.getURIEncoded () +
                                            ", " +
                                            aDocTypeID.getURIEncoded () +
                                            ")",
                                            m_aAPIProvider.getCurrentURI ());
          }
          if (LOGGER.isInfoEnabled ())
            LOGGER.info (sLog + " SUCCESS - Redirect");
          s_aStatsCounterSuccess.increment (sAction);
        }
        else
        {
          // Neither redirect nor endpoint found
          throw new SMPNotFoundException ("service(" + sServiceGroupID + "," + sDocumentTypeID + ")",
                                          m_aAPIProvider.getCurrentURI ());
        }
      }
    }
    catch (final SMPServerException ex)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      s_aStatsCounterError.increment (sAction);
      throw ex;
    }
  }

  /**
   * @return The statistics data with the invocation counter.
   */
  @Nonnull
  public static IStatisticsHandlerKeyedCounter getInvocationCounter ()
  {
    return s_aStatsCounterInvocation;
  }

  /**
   * @return The statistics data with the successful invocation counter.
   */
  @Nonnull
  public static IStatisticsHandlerKeyedCounter getSuccessCounter ()
  {
    return s_aStatsCounterSuccess;
  }

  /**
   * @return The statistics data with the error invocation counter.
   */
  @Nonnull
  public static IStatisticsHandlerKeyedCounter getErrorCounter ()
  {
    return s_aStatsCounterError;
  }
}
