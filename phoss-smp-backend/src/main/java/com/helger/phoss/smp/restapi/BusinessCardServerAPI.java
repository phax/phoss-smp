/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.restapi;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.generic.PDBusinessEntity;
import com.helger.peppol.businesscard.v3.PD3BusinessCardType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.photon.security.user.IUser;
import com.helger.statistics.api.IMutableStatisticsHandlerKeyedCounter;
import com.helger.statistics.api.IStatisticsHandlerKeyedCounter;
import com.helger.statistics.impl.StatisticsManager;

/**
 * This class implements all the service methods, that must be provided by the BusinessCard REST
 * service - this service is the same for BDXR and SMP.
 *
 * @author Philip Helger
 */
public final class BusinessCardServerAPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BusinessCardServerAPI.class);
  public static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_INVOCATION = StatisticsManager.getKeyedCounterHandler (BusinessCardServerAPI.class.getName () +
                                                                                                                                 "$call");
  public static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_SUCCESS = StatisticsManager.getKeyedCounterHandler (BusinessCardServerAPI.class.getName () +
                                                                                                                              "$success");
  public static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_ERROR = StatisticsManager.getKeyedCounterHandler (BusinessCardServerAPI.class.getName () +
                                                                                                                            "$error");
  public static final String LOG_PREFIX = "[BusinessCard REST API] ";

  private final ISMPServerAPIDataProvider m_aAPIProvider;

  public BusinessCardServerAPI (@NonNull final ISMPServerAPIDataProvider aDataProvider)
  {
    m_aAPIProvider = ValueEnforcer.notNull (aDataProvider, "DataProvider");
  }

  @NonNull
  public PD3BusinessCardType getBusinessCard (@NonNull final String sPathServiceGroupID) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /businesscard/" + sPathServiceGroupID;
    final String sAction = "getBusinessCard";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());
      }
      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
      {
        // No such service group
        throw SMPNotFoundException.unknownSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());
      }
      final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
      if (aBusinessCardMgr == null)
      {
        throw new SMPBadRequestException ("This SMP server does not support the Business Card API",
                                          m_aAPIProvider.getCurrentURI ());
      }
      final ISMPBusinessCard aBusinessCard = aBusinessCardMgr.getSMPBusinessCardOfID (aServiceGroupID);
      if (aBusinessCard == null)
      {
        // No such business card
        throw new SMPNotFoundException ("No Business Card assigned to Service Group '" + sPathServiceGroupID + "'",
                                        m_aAPIProvider.getCurrentURI ());
      }
      LOGGER.info (sLog + " SUCCESS");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return aBusinessCard.getAsJAXBObject ();
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  @NonNull
  public ESuccess createBusinessCard (@NonNull final String sPathServiceGroupID,
                                      @NonNull final PDBusinessCard aBusinessCard,
                                      @NonNull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "PUT /businesscard/" + sPathServiceGroupID;
    final String sAction = "createBusinessCard";

    LOGGER.info (sLog + " ==> " + aBusinessCard);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      // Parse and validate identifier
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());
      }
      final IParticipantIdentifier aPayloadServiceGroupID = aIdentifierFactory.createParticipantIdentifier (aBusinessCard.getParticipantIdentifier ()
                                                                                                                         .getScheme (),
                                                                                                            aBusinessCard.getParticipantIdentifier ()
                                                                                                                         .getValue ());
      if (!aServiceGroupID.hasSameContent (aPayloadServiceGroupID))
      {
        // Business identifiers must be equal
        throw new SMPBadRequestException ("Participant Inconsistency. The URL points to '" +
                                          aServiceGroupID.getURIEncoded () +
                                          "' whereas the BusinessCard contains '" +
                                          aPayloadServiceGroupID.getURIEncoded () +
                                          "'",
                                          m_aAPIProvider.getCurrentURI ());
      }
      // Retrieve the service group
      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
      {
        // No such service group (on this server)
        throw SMPNotFoundException.unknownSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());
      }
      // Check credentials and verify service group is owned by provided user
      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
      SMPUserManagerPhoton.verifyOwnership (aServiceGroupID, aSMPUser);

      final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
      if (aBusinessCardMgr == null)
      {
        throw new SMPBadRequestException ("This SMP server does not support the BusinessCard API",
                                          m_aAPIProvider.getCurrentURI ());
      }
      final ICommonsList <SMPBusinessCardEntity> aEntities = new CommonsArrayList <> ();
      for (final PDBusinessEntity aEntity : aBusinessCard.businessEntities ())
        aEntities.add (SMPBusinessCardEntity.createFromGenericObject (aEntity));
      if (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aServiceGroup.getParticipantIdentifier (), aEntities, true) ==
          null)
      {
        LOGGER.warn (sLog + " ERROR");
        STATS_COUNTER_ERROR.increment (sAction);
        return ESuccess.FAILURE;
      }
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

  /**
   * Delete an existing business card.
   *
   * @param sServiceGroupID
   *        The service group (participant) ID.
   * @param aCredentials
   *        The credentials to be used. May not be <code>null</code>.
   * @throws SMPServerException
   *         In case of error
   * @since 5.0.2
   */
  public void deleteBusinessCard (@NonNull final String sServiceGroupID, @NonNull final SMPAPICredentials aCredentials)
                                                                                                                        throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "DELETE /businesscard/" + sServiceGroupID;
    final String sAction = "deleteBusinessCard";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sServiceGroupID, m_aAPIProvider.getCurrentURI ());
      }
      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
      SMPUserManagerPhoton.verifyOwnership (aServiceGroupID, aSMPUser);

      final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
      if (aBusinessCardMgr == null)
      {
        throw new SMPBadRequestException ("This SMP server does not support the Business Card API",
                                          m_aAPIProvider.getCurrentURI ());
      }
      final ISMPBusinessCard aBusinessCard = aBusinessCardMgr.getSMPBusinessCardOfID (aServiceGroupID);
      if (aBusinessCard == null)
      {
        // No such business card
        throw new SMPNotFoundException ("No Business Card assigned to Service Group '" + sServiceGroupID + "'",
                                        m_aAPIProvider.getCurrentURI ());
      }
      aBusinessCardMgr.deleteSMPBusinessCard (aBusinessCard, true);

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

  /**
   * @return The statistics data with the invocation counter.
   */
  @NonNull
  public static IStatisticsHandlerKeyedCounter getInvocationCounter ()
  {
    return STATS_COUNTER_INVOCATION;
  }

  /**
   * @return The statistics data with the successful invocation counter.
   */
  @NonNull
  public static IStatisticsHandlerKeyedCounter getSuccessCounter ()
  {
    return STATS_COUNTER_SUCCESS;
  }

  /**
   * @return The statistics data with the error invocation counter.
   */
  @NonNull
  public static IStatisticsHandlerKeyedCounter getErrorCounter ()
  {
    return STATS_COUNTER_ERROR;
  }
}
