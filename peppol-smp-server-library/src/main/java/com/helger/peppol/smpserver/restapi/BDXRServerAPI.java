/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
import com.helger.commons.lang.ClassHelper;
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
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;

/**
 * This class implements all the service methods, that must be provided by the
 * REST service.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
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
  public CompleteServiceGroupType getCompleteServiceGroup (final String sServiceGroupID) throws Throwable
  {
    LOGGER.info (LOG_PREFIX + "GET /complete/" + sServiceGroupID);
    s_aStatsCounterInvocation.increment ("getCompleteServiceGroup");

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw new SMPNotFoundException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
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

      LOGGER.info (LOG_PREFIX + "Finished getCompleteServiceGroup(" + sServiceGroupID + ")");
      s_aStatsCounterSuccess.increment ("getCompleteServiceGroup");
      return aCompleteServiceGroup;
    }
    catch (final Throwable t)
    {
      LOGGER.warn (LOG_PREFIX +
                   "Error in getCompleteServiceGroup(" +
                   sServiceGroupID +
                   ") - " +
                   ClassHelper.getClassLocalName (t) +
                   " - " +
                   t.getMessage ());
      throw t;
    }
  }

  @Nonnull
  public ServiceGroupReferenceListType getServiceGroupReferenceList (@Nonnull final String sUserID,
                                                                     @Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    LOGGER.info (LOG_PREFIX + "GET /list/" + sUserID);
    s_aStatsCounterInvocation.increment ("getServiceGroupReferenceList");

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
      final List <ServiceGroupReferenceType> aReferenceTypes = aRefList.getServiceGroupReference ();
      for (final ISMPServiceGroup aServiceGroup : aServiceGroups)
      {
        final String sHref = m_aAPIProvider.getServiceGroupHref (aServiceGroup.getParticpantIdentifier ());

        final ServiceGroupReferenceType aServGroupRefType = new ServiceGroupReferenceType ();
        aServGroupRefType.setHref (sHref);
        aReferenceTypes.add (aServGroupRefType);
      }

      LOGGER.info (LOG_PREFIX + "Finished getServiceGroupReferenceList(" + sUserID + ")");
      s_aStatsCounterSuccess.increment ("getServiceGroupReferenceList");
      return aRefList;
    }
    catch (final Throwable t)
    {
      LOGGER.warn (LOG_PREFIX +
                   "Error in getServiceGroupReferenceList(" +
                   sUserID +
                   ") - " +
                   ClassHelper.getClassLocalName (t) +
                   " - " +
                   t.getMessage ());
      throw t;
    }
  }

  @Nonnull
  public ServiceGroupType getServiceGroup (final String sServiceGroupID) throws Throwable
  {
    LOGGER.info (LOG_PREFIX + "GET /" + sServiceGroupID);
    s_aStatsCounterInvocation.increment ("getServiceGroup");

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw new SMPNotFoundException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
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

      LOGGER.info (LOG_PREFIX + "Finished getServiceGroup(" + sServiceGroupID + ")");
      s_aStatsCounterSuccess.increment ("getServiceGroup");
      return aSG;
    }
    catch (final Throwable t)
    {
      LOGGER.warn (LOG_PREFIX +
                   "Error in getServiceGroup(" +
                   sServiceGroupID +
                   ") - " +
                   ClassHelper.getClassLocalName (t) +
                   " - " +
                   t.getMessage ());
      throw t;
    }
  }

  @Nonnull
  public ESuccess saveServiceGroup (@Nonnull final String sServiceGroupID,
                                    @Nonnull final ServiceGroupType aServiceGroup,
                                    @Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    LOGGER.info (LOG_PREFIX + "PUT /" + sServiceGroupID + " ==> " + aServiceGroup);
    s_aStatsCounterInvocation.increment ("saveServiceGroup");

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw new SMPNotFoundException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
                                        m_aAPIProvider.getCurrentURI ());
      }

      if (!aServiceGroupID.hasSameContent (aServiceGroup.getParticipantIdentifier ()))
      {
        // Business identifiers must be equal
        throw new SMPNotFoundException ("ServiceGroup inconsistency", m_aAPIProvider.getCurrentURI ());
      }

      final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
      final ISMPUser aSMPUser = aUserMgr.validateUserCredentials (aCredentials);

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final String sExtension = BDXRExtensionConverter.convertToString (aServiceGroup.getExtension ());
      if (aServiceGroupMgr.containsSMPServiceGroupWithID (aServiceGroupID))
        aServiceGroupMgr.updateSMPServiceGroup (sServiceGroupID, aSMPUser.getID (), sExtension);
      else
        if (aServiceGroupMgr.createSMPServiceGroup (aSMPUser.getID (), aServiceGroupID, sExtension) == null)
        {
          LOGGER.error (LOG_PREFIX +
                        "Finished saveServiceGroup(" +
                        sServiceGroupID +
                        "," +
                        aServiceGroup +
                        ") - failure");
          s_aStatsCounterError.increment ("saveServiceGroup");
          return ESuccess.FAILURE;
        }

      LOGGER.info (LOG_PREFIX + "Finished saveServiceGroup(" + sServiceGroupID + "," + aServiceGroup + ") - success");
      s_aStatsCounterSuccess.increment ("saveServiceGroup");
      return ESuccess.SUCCESS;
    }
    catch (final Throwable t)
    {
      LOGGER.warn (LOG_PREFIX +
                   "Error in saveServiceGroup(" +
                   sServiceGroupID +
                   "," +
                   aServiceGroup +
                   ") - " +
                   ClassHelper.getClassLocalName (t) +
                   " - " +
                   t.getMessage ());
      throw t;
    }
  }

  @Nonnull
  public ESuccess deleteServiceGroup (@Nonnull final String sServiceGroupID,
                                      @Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    LOGGER.info (LOG_PREFIX + "DELETE /" + sServiceGroupID);
    s_aStatsCounterInvocation.increment ("deleteServiceGroup");

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        LOGGER.info (LOG_PREFIX + "Failed to parse participant identifier '" + sServiceGroupID + "'");
        return ESuccess.FAILURE;
      }

      final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();

      final ISMPUser aSMPUser = aUserMgr.validateUserCredentials (aCredentials);
      aUserMgr.verifyOwnership (aServiceGroupID, aSMPUser);

      aServiceGroupMgr.deleteSMPServiceGroup (aServiceGroupID);

      LOGGER.info (LOG_PREFIX + "Finished deleteServiceGroup(" + sServiceGroupID + ")");
      s_aStatsCounterSuccess.increment ("deleteServiceGroup");
      return ESuccess.SUCCESS;
    }
    catch (final Throwable t)
    {
      LOGGER.warn (LOG_PREFIX +
                   "Error in deleteServiceGroup(" +
                   sServiceGroupID +
                   ") - " +
                   ClassHelper.getClassLocalName (t) +
                   " - " +
                   t.getMessage ());
      throw t;
    }
  }

  @Nonnull
  public SignedServiceMetadataType getServiceRegistration (@Nonnull final String sServiceGroupID,
                                                           @Nonnull final String sDocumentTypeID) throws Throwable
  {
    LOGGER.info (LOG_PREFIX + "GET /" + sServiceGroupID + "/services/" + sDocumentTypeID);
    s_aStatsCounterInvocation.increment ("getServiceRegistration");

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        throw new SMPNotFoundException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
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
        throw new SMPNotFoundException ("Failed to parse documentTypeID '" + sServiceGroupID + "'",
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

      LOGGER.info (LOG_PREFIX + "Finished getServiceRegistration(" + sServiceGroupID + "," + sDocumentTypeID + ")");
      s_aStatsCounterSuccess.increment ("getServiceRegistration");
      return aSignedServiceMetadata;
    }
    catch (final Throwable t)
    {
      LOGGER.warn (LOG_PREFIX +
                   "Error in getServiceRegistration(" +
                   sServiceGroupID +
                   "," +
                   sDocumentTypeID +
                   ") - " +
                   ClassHelper.getClassLocalName (t) +
                   " - " +
                   t.getMessage ());
      throw t;
    }
  }

  @Nonnull
  public ESuccess saveServiceRegistration (@Nonnull final String sServiceGroupID,
                                           @Nonnull final String sDocumentTypeID,
                                           @Nonnull final ServiceMetadataType aServiceMetadata,
                                           @Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    LOGGER.info (LOG_PREFIX + "PUT /" + sServiceGroupID + "/services/" + sDocumentTypeID + " ==> " + aServiceMetadata);
    s_aStatsCounterInvocation.increment ("saveServiceRegistration");

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        LOGGER.info (LOG_PREFIX + "Failed to parse participant identifier '" + sServiceGroupID + "'");
        return ESuccess.FAILURE;
      }

      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocumentTypeID);
      if (aDocTypeID == null)
      {
        // Invalid identifier
        LOGGER.info (LOG_PREFIX + "Failed to parse document type identifier '" + sDocumentTypeID + "'");
        return ESuccess.FAILURE;
      }

      // May be null for a Redirect!
      final ServiceInformationType aServiceInformation = aServiceMetadata.getServiceInformation ();
      if (aServiceInformation != null)
      {
        // Business identifiers from path (ServiceGroupID) and from service
        // metadata (body) must equal path
        if (!aServiceInformation.getParticipantIdentifier ().hasSameContent (aServiceGroupID))
        {
          LOGGER.info (LOG_PREFIX +
                       "Save service metadata was called with bad parameters. serviceInfo:" +
                       aServiceInformation.getParticipantIdentifier ().getURIEncoded () +
                       " param:" +
                       aServiceGroupID);
          return ESuccess.FAILURE;
        }

        if (!aServiceInformation.getDocumentIdentifier ().hasSameContent (aDocTypeID))
        {
          LOGGER.info (LOG_PREFIX +
                       "Save service metadata was called with bad parameters. serviceInfo:" +
                       aServiceInformation.getDocumentIdentifier ().getURIEncoded () +
                       " param:" +
                       aDocTypeID);
          // Document type must equal path
          return ESuccess.FAILURE;
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
        LOGGER.info (LOG_PREFIX + "ServiceGroup not found: " + sServiceGroupID);
        return ESuccess.FAILURE;
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
          LOGGER.error (LOG_PREFIX +
                        "Finished saveServiceRegistration(" +
                        sServiceGroupID +
                        "," +
                        sDocumentTypeID +
                        "," +
                        aServiceMetadata +
                        ") - Redirect - failure");
          s_aStatsCounterError.increment ("saveServiceRegistration");
          return ESuccess.FAILURE;
        }
      }
      else
      {
        // Handle service information
        final ProcessListType aJAXBProcesses = aServiceMetadata.getServiceInformation ().getProcessList ();
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
        if (aServiceInfoMgr.mergeSMPServiceInformation (new SMPServiceInformation (aServiceGroup,
                                                                                   aDocTypeID,
                                                                                   aProcesses,
                                                                                   BDXRExtensionConverter.convertToString (aServiceMetadata.getServiceInformation ()
                                                                                                                                           .getExtension ())))
                           .isFailure ())
        {
          LOGGER.error (LOG_PREFIX +
                        "Finished saveServiceRegistration(" +
                        sServiceGroupID +
                        "," +
                        sDocumentTypeID +
                        "," +
                        aServiceMetadata +
                        ") - ServiceInformation - failure");
          s_aStatsCounterError.increment ("saveServiceRegistration");
          return ESuccess.FAILURE;
        }
      }

      LOGGER.info (LOG_PREFIX +
                   "Finished saveServiceRegistration(" +
                   sServiceGroupID +
                   "," +
                   sDocumentTypeID +
                   "," +
                   aServiceMetadata +
                   ") - " +
                   (aServiceMetadata.getRedirect () != null ? "Redirect" : "ServiceInformation") +
                   " - success");
      s_aStatsCounterSuccess.increment ("saveServiceRegistration");
      return ESuccess.SUCCESS;
    }
    catch (final Throwable t)
    {
      LOGGER.warn (LOG_PREFIX +
                   "Error in saveServiceRegistration(" +
                   sServiceGroupID +
                   "," +
                   sDocumentTypeID +
                   "," +
                   aServiceMetadata +
                   ") - " +
                   ClassHelper.getClassLocalName (t) +
                   " - " +
                   t.getMessage ());
      throw t;
    }
  }

  @Nonnull
  public ESuccess deleteServiceRegistration (@Nonnull final String sServiceGroupID,
                                             @Nonnull final String sDocumentTypeID,
                                             @Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    LOGGER.info (LOG_PREFIX + "DELETE /" + sServiceGroupID + "/services/" + sDocumentTypeID);
    s_aStatsCounterInvocation.increment ("deleteServiceRegistration");

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        LOGGER.info (LOG_PREFIX + "Failed to parse participant identifier '" + sServiceGroupID + "'");
        return ESuccess.FAILURE;
      }

      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocumentTypeID);
      if (aDocTypeID == null)
      {
        // Invalid identifier
        LOGGER.info (LOG_PREFIX + "Failed to parse document type identifier '" + sDocumentTypeID + "'");
        return ESuccess.FAILURE;
      }

      final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
      final ISMPUser aSMPUser = aUserMgr.validateUserCredentials (aCredentials);
      aUserMgr.verifyOwnership (aServiceGroupID, aSMPUser);

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
      {
        LOGGER.info (LOG_PREFIX + "Service group '" + sServiceGroupID + "' not on this SMP");
        return ESuccess.FAILURE;
      }

      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
      final ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                                         aDocTypeID);
      if (aServiceInfo != null)
      {
        // Handle service information
        final EChange eChange = aServiceInfoMgr.deleteSMPServiceInformation (aServiceInfo);
        if (eChange.isChanged ())
        {
          LOGGER.info (LOG_PREFIX +
                       "Finished deleteServiceRegistration(" +
                       sServiceGroupID +
                       "," +
                       sDocumentTypeID +
                       ") - ServiceInformation");
          s_aStatsCounterSuccess.increment ("deleteServiceRegistration");
          return ESuccess.SUCCESS;
        }
      }
      else
      {
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        final ISMPRedirect aRedirect = aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                 aDocTypeID);
        if (aRedirect != null)
        {
          // Handle redirect
          final EChange eChange = aRedirectMgr.deleteSMPRedirect (aRedirect);
          if (eChange.isChanged ())
          {
            LOGGER.info (LOG_PREFIX +
                         "Finished deleteServiceRegistration(" +
                         sServiceGroupID +
                         "," +
                         sDocumentTypeID +
                         ") - Redirect");
            s_aStatsCounterSuccess.increment ("deleteServiceRegistration");
            return ESuccess.SUCCESS;
          }
        }
      }

      LOGGER.info (LOG_PREFIX +
                   "Service group '" +
                   sServiceGroupID +
                   "' has no document type '" +
                   sDocumentTypeID +
                   "' on this SMP!");
      return ESuccess.FAILURE;
    }
    catch (final Throwable t)
    {
      LOGGER.warn (LOG_PREFIX +
                   "Error in deleteServiceRegistration(" +
                   sServiceGroupID +
                   "," +
                   sDocumentTypeID +
                   ") - " +
                   ClassHelper.getClassLocalName (t) +
                   " - " +
                   t.getMessage ());
      throw t;
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
}
