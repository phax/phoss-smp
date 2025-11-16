/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.rest;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.app.PDClientProvider;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPInternalErrorException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.restapi.BusinessCardServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.security.user.IUser;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class APIExecutorBusinessCardPush extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorBusinessCardPush.class);

  private static void _pushBusinessCard (@NonNull final ISMPServerAPIDataProvider aDataProvider,
                                         @NonNull final String sServiceGroupID,
                                         @NonNull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = BusinessCardServerAPI.LOG_PREFIX + "POST /businesscard/" + sServiceGroupID + "/push";
    final String sAction = "pushBusinessCard";

    LOGGER.info (sLog);
    BusinessCardServerAPI.STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sServiceGroupID, aDataProvider.getCurrentURI ());
      }
      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
      SMPUserManagerPhoton.verifyOwnership (aServiceGroupID, aSMPUser);

      final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
      if (aBusinessCardMgr == null)
      {
        throw new SMPBadRequestException ("This SMP server does not support the Business Card API",
                                          aDataProvider.getCurrentURI ());
      }

      // Only if a business card is present
      if (!aBusinessCardMgr.containsSMPBusinessCardOfID (aServiceGroupID))
        throw new SMPBadRequestException ("The provided Service Group ID '" +
                                          sServiceGroupID +
                                          "' has no BusinessCard on this SMP",
                                          aDataProvider.getCurrentURI ());

      // Notify PD server: update
      if (PDClientProvider.getInstance ().getPDClient ().addServiceGroupToIndex (aServiceGroupID).isFailure ())
        throw new SMPInternalErrorException ("Failed to inform the Directory to index '" +
                                             sServiceGroupID +
                                             "' - see server log file for details");

      LOGGER.info (sLog + " SUCCESS");
      BusinessCardServerAPI.STATS_COUNTER_SUCCESS.increment (sAction);
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      BusinessCardServerAPI.STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    if (!SMPMetaManager.getSettings ().isDirectoryIntegrationEnabled ())
    {
      // PD integration is disabled
      throw new SMPPreconditionFailedException ("The " +
                                                SMPWebAppConfiguration.getDirectoryName () +
                                                " integration is disabled. pushBusinessCard will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Parse main payload
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());

    _pushBusinessCard (aDataProvider, sServiceGroupID, aCredentials);
    aUnifiedResponse.createOk ();
  }
}
