/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationDirection;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationState;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * REST API to cancel an outbound migration for a participant
 *
 * @author Philip Helger
 * @since 5.6.0
 */
public final class APIExecutorMigrationOutboundCancelPut extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorMigrationOutboundCancelPut.class);

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. migrationOutboundCancel will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    final String sLogPrefix = "[REST API Migration-Outbound-Cancel] ";

    LOGGER.info (sLogPrefix +
                 "Cancelling outbound Participant Migration for Service Group ID '" +
                 sPathServiceGroupID +
                 "'");

    // Only authenticated user may do so
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());
    SMPUserManagerPhoton.validateUserCredentials (aCredentials);

    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();

    final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());
    }

    // Find matching migration object
    final ISMPParticipantMigration aMigration = aParticipantMigrationMgr.getParticipantMigrationOfParticipantID (EParticipantMigrationDirection.OUTBOUND,
                                                                                                                 EParticipantMigrationState.IN_PROGRESS,
                                                                                                                 aServiceGroupID);
    if (aMigration == null)
    {
      throw new SMPBadRequestException ("Failed to resolve outbound Participant Migration for Service Group ID '" +
                                        sPathServiceGroupID +
                                        "'",
                                        aDataProvider.getCurrentURI ());
    }

    final String sMigrationID = aMigration.getID ();
    LOGGER.info (sLogPrefix +
                 "Found the outbound Participant Migration ID '" +
                 sMigrationID +
                 "' with state " +
                 aMigration.getState () +
                 " for the Service Group ID '" +
                 sPathServiceGroupID +
                 "'");

    // Change migration state
    if (aParticipantMigrationMgr.setParticipantMigrationState (sMigrationID, EParticipantMigrationState.CANCELLED)
                                .isUnchanged ())
    {
      throw new SMPBadRequestException ("Failed to cancel the outbound Participant Migration with ID '" +
                                        sMigrationID +
                                        "'",
                                        aDataProvider.getCurrentURI ());
    }

    LOGGER.info (sLogPrefix +
                 "The outbound Participant Migration with ID '" +
                 sMigrationID +
                 "' on Service Group ID '" +
                 sPathServiceGroupID +
                 "' was successfully cancelled!");
    aUnifiedResponse.createOk ();
  }
}
