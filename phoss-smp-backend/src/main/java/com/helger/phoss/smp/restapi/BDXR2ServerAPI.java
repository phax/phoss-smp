/*
 * Copyright (C) 2015-2024 Philip Helger and contributors
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
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.base64.Base64;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.statistics.IMutableStatisticsHandlerKeyedCounter;
import com.helger.commons.statistics.IStatisticsHandlerKeyedCounter;
import com.helger.commons.statistics.StatisticsManager;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
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
import com.helger.photon.security.user.IUser;
import com.helger.smpclient.extension.SMPExtensionList;
import com.helger.xsds.bdxr.smp2.ServiceGroupType;
import com.helger.xsds.bdxr.smp2.ServiceMetadataType;
import com.helger.xsds.bdxr.smp2.ac.EndpointType;
import com.helger.xsds.bdxr.smp2.ac.ProcessMetadataType;
import com.helger.xsds.bdxr.smp2.ac.ProcessType;
import com.helger.xsds.bdxr.smp2.ac.ServiceReferenceType;
import com.helger.xsds.bdxr.smp2.bc.IDType;

/**
 * This class implements all the service methods, that must be provided by the
 * OASIS BDXR SMP v2 REST service.
 *
 * @author Philip Helger
 * @since 5.7.0
 */
public final class BDXR2ServerAPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BDXR2ServerAPI.class);
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_INVOCATION = StatisticsManager.getKeyedCounterHandler (BDXR2ServerAPI.class.getName () +
                                                                                                                                  "$call");
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_SUCCESS = StatisticsManager.getKeyedCounterHandler (BDXR2ServerAPI.class.getName () +
                                                                                                                               "$success");
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_ERROR = StatisticsManager.getKeyedCounterHandler (BDXR2ServerAPI.class.getName () +
                                                                                                                             "$error");
  private static final String LOG_PREFIX = "[BDXR2 REST API] ";

  private final ISMPServerAPIDataProvider m_aAPIDataProvider;

  public BDXR2ServerAPI (@Nonnull final ISMPServerAPIDataProvider aDataProvider)
  {
    m_aAPIDataProvider = ValueEnforcer.notNull (aDataProvider, "DataProvider");
  }

  @Nullable
  public static String convertToJsonString (@Nullable final com.helger.xsds.bdxr.smp2.ec.SMPExtensionsType aExtensions)
  {
    final SMPExtensionList ret = SMPExtensionList.ofBDXR2 (aExtensions);
    return ret == null ? null : ret.getExtensionsAsJsonString ();
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

      // Retrieve the service group
      final ISMPServiceGroup aPathServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aPathServiceGroupID);
      if (aPathServiceGroup == null)
      {
        // No such service group
        throw new SMPNotFoundException ("Unknown Service Group '" + sPathServiceGroupID + "'",
                                        m_aAPIDataProvider.getCurrentURI ());
      }
      // Then add the service metadata references
      final ServiceGroupType aSG = aPathServiceGroup.getAsJAXBObjectBDXR2 ();
      for (final IDocumentTypeIdentifier aDocTypeID : aServiceInfoMgr.getAllSMPDocumentTypesOfServiceGroup (aPathServiceGroupID))
      {
        // Ignore all service information without endpoints
        final ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPathServiceGroupID,
                                                                                                                           aDocTypeID);
        if (aServiceInfo != null && aServiceInfo.getTotalEndpointCount () > 0)
        {
          final ServiceReferenceType aMetadataReference = new ServiceReferenceType ();
          {
            final IDType aID = new IDType ();
            aID.setSchemeID (aDocTypeID.getScheme ());
            aID.setValue (aDocTypeID.getValue ());
            aMetadataReference.setID (aID);
          }
          aSG.addServiceReference (aMetadataReference);
        }
      }
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
      if (aServiceGroup.getParticipantID () == null)
      {
        // Can happen when tampering with the input data
        aPayloadServiceGroupID = null;
      }
      else
      {
        aPayloadServiceGroupID = aIdentifierFactory.createParticipantIdentifier (aServiceGroup.getParticipantID ()
                                                                                              .getSchemeID (),
                                                                                 aServiceGroup.getParticipantID ()
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
      final String sExtension = convertToJsonString (aServiceGroup.getSMPExtensions ());
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
  public ServiceMetadataType getServiceRegistration (@Nonnull final String sPathServiceGroupID,
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

      final ServiceMetadataType aServiceMetadata;
      if (aRedirect != null)
      {
        aServiceMetadata = aRedirect.getAsJAXBObjectBDXR2 ();
      }
      else
      {
        // Get as regular service information
        final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
        final ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPathServiceGroupID,
                                                                                                                           aPathDocTypeID);
        if (aServiceInfo != null)
        {
          aServiceMetadata = aServiceInfo.getAsJAXBObjectBDXR2 ();
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
      return aServiceMetadata;
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
      // Business identifiers from path (ServiceGroupID) and from service
      // metadata (body) must equal path
      if (aServiceMetadata.getParticipantID () == null)
      {
        // Can happen when tampering with the input data
        throw new SMPBadRequestException ("Save Service Metadata has inconsistent values.\n" +
                                          "Service Metadata Participant ID: <none>\n" +
                                          "URL Parameter value: '" +
                                          aPathServiceGroupID.getURIEncoded () +
                                          "'",
                                          m_aAPIDataProvider.getCurrentURI ());
      }
      final IParticipantIdentifier aPayloadServiceGroupID = aIdentifierFactory.createParticipantIdentifier (aServiceMetadata.getParticipantID ()
                                                                                                                            .getSchemeID (),
                                                                                                            aServiceMetadata.getParticipantID ()
                                                                                                                            .getValue ());
      if (!aPathServiceGroupID.hasSameContent (aPayloadServiceGroupID))
      {
        // Participant ID in URL must match the one in XML structure
        throw new SMPBadRequestException ("Save Service Metadata was called with inconsistent values.\n" +
                                          "Service Metadata Participant ID: " +
                                          (aPayloadServiceGroupID == null ? "<none>" : "'" +
                                                                                       aPayloadServiceGroupID.getURIEncoded () +
                                                                                       "'") +
                                          "\n" +
                                          "URL parameter value: '" +
                                          aPathServiceGroupID.getURIEncoded () +
                                          "'",
                                          m_aAPIDataProvider.getCurrentURI ());
      }
      if (aServiceMetadata.getID () == null)
      {
        throw new SMPBadRequestException ("Save Service Metadata was called with inconsistent values.\n" +
                                          "Service Metadata ID: <none>\n" +
                                          "URL parameter value: '" +
                                          aPathDocTypeID.getURIEncoded () +
                                          "'",
                                          m_aAPIDataProvider.getCurrentURI ());
      }
      final IDocumentTypeIdentifier aPayloadDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (aServiceMetadata.getID ()
                                                                                                                         .getSchemeID (),
                                                                                                         aServiceMetadata.getID ()
                                                                                                                         .getValue ());
      if (!aPathDocTypeID.hasSameContent (aPayloadDocTypeID))
      {
        // Document type ID in URL must match the one in XML structure
        throw new SMPBadRequestException ("Save Service Metadata was called with inconsistent values.\n" +
                                          "Service Metadata ID: '" +
                                          aPayloadDocTypeID.getURIEncoded () +
                                          "'\n" +
                                          "URL parameter value: '" +
                                          aPathDocTypeID.getURIEncoded () +
                                          "'",
                                          m_aAPIDataProvider.getCurrentURI ());
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
      for (final ProcessMetadataType aPM : aServiceMetadata.getProcessMetadata ())
      {
        if (aPM.getRedirect () != null)
        {
          // Handle redirect
          final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
          // not available in OASIS BDXR SMP v2 mode
          final String sCertificateUID = null;
          final X509Certificate aCertificate = null;
          if (aRedirectMgr.createOrUpdateSMPRedirect (aPathServiceGroupID,
                                                      aPathDocTypeID,
                                                      aPM.getRedirect ().getPublisherURI ().getValue (),
                                                      sCertificateUID,
                                                      aCertificate,
                                                      convertToJsonString (aPM.getRedirect ().getSMPExtensions ())) ==
              null)
          {
            LOGGER.error (sLog + " - ERROR - Redirect");
            STATS_COUNTER_ERROR.increment (sAction);
            return ESuccess.FAILURE;
          }
          LOGGER.info (sLog + " SUCCESS - Redirect");
        }
        else
          if (aPM.getEndpoint () != null)
          {
            // Handle endpoints

            // Extract process IDs and their extensions
            final ICommonsMap <IProcessIdentifier, String> aProcIDs = new CommonsHashMap <> ();
            for (final ProcessType aProc : aPM.getProcess ())
            {
              final IDType aSrcID = aProc.getID ();

              final IProcessIdentifier aProcID = aIdentifierFactory.createProcessIdentifier (aSrcID.getSchemeID (),
                                                                                             aSrcID.getValue ());
              if (aProcID != null)
                aProcIDs.put (aProcID, convertToJsonString (aProc.getSMPExtensions ()));
              else
                LOGGER.warn ("Failed to parse process identifier '" +
                             aSrcID.getSchemeID () +
                             "' and '" +
                             aSrcID.getValue () +
                             "'");
            }
            if (aProcIDs.isEmpty ())
            {
              throw new SMPBadRequestException ("Save Service Metadata was called without any valid Process IDs",
                                                m_aAPIDataProvider.getCurrentURI ());
            }
            final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList <> ();
            for (final Map.Entry <IProcessIdentifier, String> aProcIDEntry : aProcIDs.entrySet ())
            {
              final ICommonsList <SMPEndpoint> aEndpoints = new CommonsArrayList <> ();
              for (final EndpointType aJAXBEndpoint : aPM.getEndpoint ())
              {
                // TODO BDXR2 use first cert only
                byte [] aCertBytes = null;
                if (aJAXBEndpoint.hasCertificateEntries ())
                  aCertBytes = aJAXBEndpoint.getCertificateAtIndex (0).getContentBinaryObjectValue ();

                final SMPEndpoint aEndpoint = new SMPEndpoint (aJAXBEndpoint.getTransportProfileIDValue (),
                                                               aJAXBEndpoint.getAddressURIValue (),
                                                               SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE,
                                                               null,
                                                               PDTFactory.createXMLOffsetDateTime (aJAXBEndpoint.getActivationDateValue ()),
                                                               PDTFactory.createXMLOffsetDateTime (aJAXBEndpoint.getExpirationDateValue ()),
                                                               aCertBytes == null ? null : Base64.encodeBytes (
                                                                                                               aCertBytes),
                                                               aJAXBEndpoint.getDescriptionValue (),
                                                               aJAXBEndpoint.getContactValue (),
                                                               null,
                                                               convertToJsonString (aJAXBEndpoint.getSMPExtensions ()));
                aEndpoints.add (aEndpoint);
              }
              final SMPProcess aProcess = new SMPProcess (aProcIDEntry.getKey (), aEndpoints, aProcIDEntry.getValue ());
              aProcesses.add (aProcess);
            }
            final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
            final String sExtensionXML = convertToJsonString (aServiceMetadata.getSMPExtensions ());
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
            throw new SMPBadRequestException ("Save Service Metadata was called with neither a Redirect nor an Endpoint",
                                              m_aAPIDataProvider.getCurrentURI ());
          }
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
