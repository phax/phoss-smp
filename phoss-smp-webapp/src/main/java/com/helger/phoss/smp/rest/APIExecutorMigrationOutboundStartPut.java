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
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPInternalErrorException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.exception.SMPSMLException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * REST API to start an outbound migration for a participant
 *
 * @author Philip Helger
 * @since 5.6.0
 */
public final class APIExecutorMigrationOutboundStartPut extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorMigrationOutboundStartPut.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. migrationOutboundStart will not be executed.");
      aUnifiedResponse.setStatus (CHttp.HTTP_PRECONDITION_FAILED);
    }
    else
    {
      final String sServiceGroupID = aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID);

      final String sLogPrefix = "[Migration-Outbound-Start] ";
      LOGGER.info (sLogPrefix + "Starting outbound migration");

      // Only authenticated user may do so
      final BasicAuthClientCredentials aBasicAuth = SMPRestRequestHelper.getMandatoryAuth (aRequestScope.headers ());
      SMPUserManagerPhoton.validateUserCredentials (aBasicAuth);

      final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, sServiceGroupID);
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final ISMPSettings aSettings = SMPMetaManager.getSettings ();
      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
      final ISMLInfo aSMLInfo = aSettings.getSMLInfo ();

      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw SMPBadRequestException.failedToParseSG (sServiceGroupID, aDataProvider.getCurrentURI ());
      }

      // Check that service group exists
      if (!aServiceGroupMgr.containsSMPServiceGroupWithID (aServiceGroupID))
      {
        throw new SMPBadRequestException ("The Service Group '" + sServiceGroupID + "' does not exist.", aDataProvider.getCurrentURI ());
      }

      // Ensure no existing migration is in process
      if (aParticipantMigrationMgr.containsOutboundMigrationInProgress (aServiceGroupID))
      {
        throw new SMPBadRequestException ("The migration of the Service Group '" + sServiceGroupID + "' is already in progress.",
                                          aDataProvider.getCurrentURI ());
      }

      if (aSMLInfo == null)
        throw new SMPPreconditionFailedException ("Currently no SML is available. Please select it in the UI at the 'SMP Settings' page.",
                                                  aDataProvider.getCurrentURI ());
      if (!aSettings.isSMLEnabled ())
        throw new SMPPreconditionFailedException ("SML Connection is not enabled hence no participant can be migrated.",
                                                  aDataProvider.getCurrentURI ());

      String sMigrationKey = null;
      try
      {
        final ManageParticipantIdentifierServiceCaller aCaller = new ManageParticipantIdentifierServiceCaller (aSMLInfo);
        aCaller.setSSLSocketFactory (SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ());

        // Create a random migration key,
        // Than call SML
        sMigrationKey = aCaller.prepareToMigrate (aServiceGroupID, SMPServerConfiguration.getSMLSMPID ());
        LOGGER.info ("Successfully called prepareToMigrate on SML. Created migration key is '" + sMigrationKey + "'");
      }
      catch (final BadRequestFault | InternalErrorFault | NotFoundFault | UnauthorizedFault ex)
      {
        throw new SMPSMLException ("Failed to call prepareToMigrate on SML for Service Group '" + sServiceGroupID + "'", ex);
      }

      // Remember internally
      final ISMPParticipantMigration aMigration = aParticipantMigrationMgr.createOutboundParticipantMigration (aServiceGroupID,
                                                                                                               sMigrationKey);
      if (aMigration == null)
        throw new SMPInternalErrorException ("Failed to create outbound participant migration for '" + sServiceGroupID + "' internally");

      LOGGER.info ("Successfully created outbound participant migration internally.");

      final IMicroDocument aResponseDoc = new MicroDocument ();
      final IMicroElement eRoot = aResponseDoc.appendElement ("migrationOutboundStartResponse");
      eRoot.setAttribute ("success", true);
      eRoot.appendElement ("migrationID", aMigration.getID ());
      eRoot.appendElement ("migrationKey").appendText (sMigrationKey);

      final XMLWriterSettings aXWS = new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN);
      aUnifiedResponse.setContentAndCharset (MicroWriter.getNodeAsString (aResponseDoc, aXWS), aXWS.getCharset ())
                      .setMimeType (new MimeType (CMimeType.APPLICATION_XML).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                           aXWS.getCharset ().name ()));
    }
  }
}
