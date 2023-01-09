/*
 * Copyright (C) 2014-2023 Philip Helger and contributors
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

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.CHttp;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationDirection;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationState;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * REST API to finalize an outbound migration for a participant. This implies
 * the local deletion of the respective Service Group.
 *
 * @author Philip Helger
 * @since 5.6.0
 */
public final class APIExecutorMigrationOutboundFinalizePut extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorMigrationOutboundFinalizePut.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sServiceGroupID = aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID);
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, sServiceGroupID);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. migrationOutboundFinalize will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    final String sLogPrefix = "[REST API Migration-Outbound-Finalize] ";
    LOGGER.info (sLogPrefix + "Finalizing outbound migration for Service Group ID '" + sServiceGroupID + "'");

    // Only authenticated user may do so
    final BasicAuthClientCredentials aBasicAuth = getMandatoryAuth (aRequestScope.headers ());
    SMPUserManagerPhoton.validateUserCredentials (aBasicAuth);

    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();

    final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      throw SMPBadRequestException.failedToParseSG (sServiceGroupID, aDataProvider.getCurrentURI ());
    }

    // Find matching migration object
    final ISMPParticipantMigration aMigration = aParticipantMigrationMgr.getParticipantMigrationOfParticipantID (EParticipantMigrationDirection.OUTBOUND,
                                                                                                                 EParticipantMigrationState.IN_PROGRESS,
                                                                                                                 aServiceGroupID);
    if (aMigration == null)
    {
      throw new SMPBadRequestException ("Failed to resolve outbound participant migration for Service Group ID '" + sServiceGroupID + "'",
                                        aDataProvider.getCurrentURI ());
    }

    // Remember the old state
    final String sMigrationID = aMigration.getID ();
    final EParticipantMigrationState eOldState = aMigration.getState ();

    // Migrate state
    if (aParticipantMigrationMgr.setParticipantMigrationState (sMigrationID, EParticipantMigrationState.MIGRATED).isUnchanged ())
    {
      throw new SMPBadRequestException ("The participant migration with ID '" + sMigrationID + "' is already finalized",
                                        aDataProvider.getCurrentURI ());
    }

    LOGGER.info (sLogPrefix +
                 "The outbound Participant Migration with ID '" +
                 sMigrationID +
                 "' for '" +
                 sServiceGroupID +
                 "' was successfully finalized!");

    try
    {
      // Delete the service group only locally but not
      // in the SML
      if (aServiceGroupMgr.deleteSMPServiceGroup (aServiceGroupID, false).isChanged ())
      {
        LOGGER.info (sLogPrefix +
                     "The SMP Service Group for participant '" +
                     sServiceGroupID +
                     "' was successfully deleted from this SMP (without SML)!");
      }
      else
      {
        throw new SMPBadRequestException ("The SMP Service Group for participant '" + sServiceGroupID + "' could not be deleted",
                                          aDataProvider.getCurrentURI ());
      }
    }
    catch (final SMPServerException ex)
    {
      // Restore old state in participant migration
      // manager
      if (aParticipantMigrationMgr.setParticipantMigrationState (sMigrationID, eOldState).isChanged ())
      {
        LOGGER.warn (sLogPrefix +
                     "Successfully reverted the state of the outbound Participant Migration for '" +
                     sServiceGroupID +
                     "' to " +
                     eOldState +
                     "!");
      }
      else
      {
        // Error in error handling. Yeah
        LOGGER.error (sLogPrefix +
                      "Failed to revert the state of the outbound Participant Migration for '" +
                      sServiceGroupID +
                      "' to " +
                      eOldState +
                      "!");
      }

      throw ex;
    }

    aUnifiedResponse.setStatus (CHttp.HTTP_OK).disableCaching ();
  }
}
