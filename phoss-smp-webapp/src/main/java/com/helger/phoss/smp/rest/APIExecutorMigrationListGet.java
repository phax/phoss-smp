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
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationDirection;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationState;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;

/**
 * REST API to list the participant migrations of a certain direction, optionally filtered by
 * state.
 *
 * @author Philip Helger
 * @since 8.6.2
 */
public final class APIExecutorMigrationListGet extends AbstractSMPAPIExecutor
{
  public static final String PARAM_STATE = "state";
  public static final String XML_ELEMENT_INITIATION_DATE_TIME = "initiationDateTime";

  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorMigrationListGet.class);

  private final EParticipantMigrationDirection m_eDirection;

  public APIExecutorMigrationListGet (@NonNull final EParticipantMigrationDirection eDirection)
  {
    ValueEnforcer.notNull (eDirection, "Direction");
    m_eDirection = eDirection;
  }

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sLogPrefix = "[REST API Migration-List-" + m_eDirection.getID () + "] ";

    // Only authenticated user may do so
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());
    SMPUserManagerPhoton.validateUserCredentials (aCredentials);

    // Optional state filter
    final String sState = StringHelper.trim (aRequestScope.params ().getAsString (PARAM_STATE));
    EParticipantMigrationState eState = null;
    if (StringHelper.isNotEmpty (sState))
    {
      eState = EParticipantMigrationState.getFromIDOrNull (sState);
      if (eState == null)
      {
        throw new SMPBadRequestException ("The migration state '" +
                                          sState +
                                          "' is invalid. Allowed values are 'inprogress', 'cancelled' and 'migrated'",
                                          new SMPRestDataProvider (aRequestScope).getCurrentURI ());
      }
    }

    LOGGER.info (sLogPrefix + "Listing migrations with state " + (eState == null ? "any" : "'" + eState.getID () + "'"));

    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    final ICommonsList <ISMPParticipantMigration> aMigrations = aParticipantMigrationMgr.getAllParticipantMigrations (m_eDirection,
                                                                                                                      eState);

    // Build result
    final IMicroDocument aResponseDoc = new MicroDocument ();
    final IMicroElement eRoot = aResponseDoc.addElement ("migrationList");
    eRoot.setAttribute ("direction", m_eDirection.getID ());
    if (eState != null)
      eRoot.setAttribute (PARAM_STATE, eState.getID ());
    eRoot.setAttribute ("count", aMigrations.size ());
    for (final ISMPParticipantMigration aMigration : aMigrations)
    {
      final IMicroElement eMigration = eRoot.addElement ("migration");
      eMigration.setAttribute ("id", aMigration.getID ());
      eMigration.setAttribute (PARAM_STATE, aMigration.getState ().getID ());
      eMigration.addElement (APIExecutorMigrationOutboundStartPut.XML_ELEMENT_PARTICIPANT_ID)
                .addText (aMigration.getParticipantIdentifier ().getURIEncoded ());
      eMigration.addElement (XML_ELEMENT_INITIATION_DATE_TIME).addText (aMigration.getInitiationDateTime ().toString ());
      eMigration.addElement (APIExecutorMigrationOutboundStartPut.XML_ELEMENT_MIGRATION_KEY)
                .addText (aMigration.getMigrationKey ());
    }

    aUnifiedResponse.xml (aResponseDoc);
  }
}
