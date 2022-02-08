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

import java.security.GeneralSecurityException;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppol.smlclient.participant.BadRequestFault;
import com.helger.peppol.smlclient.participant.InternalErrorFault;
import com.helger.peppol.smlclient.participant.NotFoundFault;
import com.helger.peppol.smlclient.participant.UnauthorizedFault;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.exception.SMPSMLException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.security.user.IUser;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.sun.xml.ws.client.ClientTransportException;

/**
 * REST API to perform an inbound migration for a participant
 *
 * @author Philip Helger
 * @since 5.6.0
 */
public final class APIExecutorMigrationInboundFromPathPut extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorMigrationInboundFromPathPut.class);

  public static void migrationInbound (@Nonnull final String sServiceGroupID,
                                       @Nonnull final String sMigrationKey,
                                       @Nonnull final String sLogPrefix,
                                       @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                       @Nonnull final UnifiedResponse aUnifiedResponse) throws SMPServerException, GeneralSecurityException
  {
    LOGGER.info (sLogPrefix +
                 "Starting inbound migration for Service Group ID '" +
                 sServiceGroupID +
                 "' and migration key '" +
                 sMigrationKey +
                 "'");

    // Only authenticated user may do so
    final BasicAuthClientCredentials aBasicAuth = getMandatoryAuth (aRequestScope.headers ());
    final IUser aOwningUser = SMPUserManagerPhoton.validateUserCredentials (aBasicAuth);

    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, sServiceGroupID);
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final ISMLInfo aSMLInfo = aSettings.getSMLInfo ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();

    if (aSMLInfo == null)
    {
      throw new SMPPreconditionFailedException ("Currently no SML is available. Please select it in the UI at the 'SMP Settings' page",
                                                aDataProvider.getCurrentURI ());
    }
    if (!aSettings.isSMLEnabled ())
    {
      throw new SMPPreconditionFailedException ("SML Connection is not enabled hence no participant can be migrated",
                                                aDataProvider.getCurrentURI ());
    }

    final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
    if (aParticipantID == null)
    {
      // Invalid identifier
      throw SMPBadRequestException.failedToParseSG (sServiceGroupID, aDataProvider.getCurrentURI ());
    }

    // Check that service group does not exist yet
    if (aServiceGroupMgr.containsSMPServiceGroupWithID (aParticipantID))
    {
      throw new SMPBadRequestException ("The Service Group '" + sServiceGroupID + "' already exists.", aDataProvider.getCurrentURI ());
    }

    // Ensure no existing migration is in process
    if (aParticipantMigrationMgr.containsInboundMigration (aParticipantID))
    {
      throw new SMPBadRequestException ("The inbound migration of the Service Group '" + sServiceGroupID + "' is already contained.",
                                        aDataProvider.getCurrentURI ());
    }

    // First get the SMK/SML migration result and if that was successful,
    // create the Service Group locally
    try
    {
      final ManageParticipantIdentifierServiceCaller aCaller = new ManageParticipantIdentifierServiceCaller (aSettings.getSMLInfo ());
      aCaller.setSSLSocketFactory (SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ());

      // SML call
      aCaller.migrate (aParticipantID, sMigrationKey, SMPServerConfiguration.getSMLSMPID ());
      LOGGER.info (sLogPrefix +
                   "Successfully migrated '" +
                   aParticipantID.getURIEncoded () +
                   "' in the SML to this SMP using migration key '" +
                   sMigrationKey +
                   "'");
    }
    catch (final BadRequestFault | InternalErrorFault | NotFoundFault | UnauthorizedFault | ClientTransportException ex)
    {
      throw new SMPSMLException ("Failed to confirm the migration for participant '" +
                                 aParticipantID.getURIEncoded () +
                                 "' in SML, hence the migration failed." +
                                 " Please check the participant identifier and the migration key.",
                                 ex);
    }

    // Now create the service group locally (it was already checked that the
    // PID is available on this SMP)
    ISMPServiceGroup aSG = null;
    Exception aCaughtEx = null;
    try
    {
      // Do not allow any Extension here
      // Do NOT create in SMK/SML
      aSG = aServiceGroupMgr.createSMPServiceGroup (aOwningUser.getID (), aParticipantID, (String) null, false);
    }
    catch (final Exception ex)
    {
      aCaughtEx = ex;
    }

    if (aSG != null)
    {
      LOGGER.info (sLogPrefix +
                   "The new SMP Service Group for participant '" +
                   aParticipantID.getURIEncoded () +
                   "' was successfully created.");
    }
    else
    {
      // No exception here
      LOGGER.error (sLogPrefix + "Error creating the new SMP Service Group for participant '" + aParticipantID.getURIEncoded () + "'.",
                    aCaughtEx);
    }

    // Remember internally
    final ISMPParticipantMigration aMigration = aParticipantMigrationMgr.createInboundParticipantMigration (aParticipantID, sMigrationKey);
    if (aMigration != null)
    {
      LOGGER.info (sLogPrefix +
                   "The participant migration for '" +
                   aParticipantID.getURIEncoded () +
                   "' with migration key '" +
                   sMigrationKey +
                   "' was successfully performed. Please inform the source SMP that the migration was successful.");
    }
    else
    {
      // No exception here
      LOGGER.error (sLogPrefix + "Failed to store the participant migration for '" + aParticipantID.getURIEncoded () + "'.");
    }

    final IMicroDocument aResponseDoc = new MicroDocument ();
    final IMicroElement eRoot = aResponseDoc.appendElement ("migrationInboundResponse");
    eRoot.setAttribute ("success", aSG != null && aMigration != null);
    eRoot.setAttribute ("serviceGroupCreated", aSG != null);
    eRoot.setAttribute ("migrationCreated", aMigration != null);

    final XMLWriterSettings aXWS = new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN);
    aUnifiedResponse.setContentAndCharset (MicroWriter.getNodeAsString (aResponseDoc, aXWS), aXWS.getCharset ())
                    .setMimeType (new MimeType (CMimeType.APPLICATION_XML).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                         aXWS.getCharset ().name ()))
                    .disableCaching ();
  }

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, null);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. migrationInbound will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    final String sLogPrefix = "[REST API Migration-Inbound] ";
    final String sServiceGroupID = aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID);
    final String sMigrationKey = aPathVariables.get (SMPRestFilter.PARAM_MIGRATION_KEY);

    migrationInbound (sServiceGroupID, sMigrationKey, sLogPrefix, aRequestScope, aUnifiedResponse);
  }
}
