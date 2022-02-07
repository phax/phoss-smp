/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationState;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
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
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. migrationOutboundFinalize will not be executed.");
      aUnifiedResponse.setStatus (CHttp.HTTP_PRECONDITION_FAILED);
    }
    else
    {
      final String sMigrationID = aPathVariables.get (SMPRestFilter.PARAM_MIGRATION_ID);

      final String sLogPrefix = "[Migration-Outbound-Finalize] ";
      LOGGER.info (sLogPrefix + "Finalizing outbound migration");

      // Only authenticated user may do so
      final BasicAuthClientCredentials aBasicAuth = SMPRestRequestHelper.getMandatoryAuth (aRequestScope.headers ());
      SMPUserManagerPhoton.validateUserCredentials (aBasicAuth);

      final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, null);
      final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();

      // Remember the old state
      final ISMPParticipantMigration aMigration = aParticipantMigrationMgr.getParticipantMigrationOfID (sMigrationID);
      if (aMigration == null)
        throw new SMPBadRequestException ("Failed to resolve participant migration with ID '" + sMigrationID + "'",
                                          aDataProvider.getCurrentURI ());

      final EParticipantMigrationState eOldState = aMigration.getState ();
      final IParticipantIdentifier aParticipantID = aMigration.getParticipantIdentifier ();

      // Migrate state
      if (aParticipantMigrationMgr.setParticipantMigrationState (aMigration.getID (), EParticipantMigrationState.MIGRATED).isUnchanged ())
        throw new SMPBadRequestException ("The participant migration with ID '" + sMigrationID + "' is already finalized",
                                          aDataProvider.getCurrentURI ());
      LOGGER.info ("The outbound Participant Migration with ID '" +
                   sMigrationID +
                   "' for '" +
                   aParticipantID.getURIEncoded () +
                   "' was successfully finalized!");

      try
      {
        // Delete the service group only locally but not
        // in the SML
        if (aServiceGroupMgr.deleteSMPServiceGroup (aParticipantID, false).isChanged ())
        {
          LOGGER.info ("The SMP ServiceGroup for participant '" +
                       aParticipantID.getURIEncoded () +
                       "' was successfully deleted from this SMP (without SML)!");
        }
        else
        {
          throw new SMPBadRequestException ("The SMP ServiceGroup for participant '" +
                                            aParticipantID.getURIEncoded () +
                                            "' could not be deleted! Please check the logs.",
                                            aDataProvider.getCurrentURI ());
        }
      }
      catch (final SMPServerException ex)
      {
        // Restore old state in participant migration
        // manager
        if (aParticipantMigrationMgr.setParticipantMigrationState (aMigration.getID (), eOldState).isChanged ())
        {
          LOGGER.info ("Successfully reverted the state of the outbound Participant Migration for '" +
                       aParticipantID.getURIEncoded () +
                       "' to " +
                       eOldState +
                       "!");
        }
        else
        {
          // Error in error handling. Yeah
          LOGGER.error ("Failed to revert the state of the outbound Participant Migration for '" +
                        aParticipantID.getURIEncoded () +
                        "' to " +
                        eOldState +
                        "!");
        }

        throw ex;
      }

      aUnifiedResponse.setStatus (CHttp.HTTP_OK);
    }
  }
}
