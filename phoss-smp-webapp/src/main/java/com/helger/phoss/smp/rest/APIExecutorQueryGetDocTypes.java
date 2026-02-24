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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.CGlobal;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsTreeMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.collection.commons.ICommonsSortedMap;
import com.helger.datetime.helper.PDTFactory;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.peppol.api.rest.PeppolAPIHelper;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.sml.ESMPAPIType;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.ui.types.feedbackcb.FeedbackCallbackLog;
import com.helger.peppol.ui.types.smp.ISMPClientCreationCallback;
import com.helger.peppol.ui.types.smp.ISMPExtensionsCallback;
import com.helger.peppol.ui.types.smp.SMPQueryParams;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.smpclient.bdxr1.BDXRClientReadOnly;
import com.helger.smpclient.bdxr2.BDXR2ClientReadOnly;
import com.helger.smpclient.exception.SMPClientException;
import com.helger.smpclient.json.SMPJsonResponse;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class APIExecutorQueryGetDocTypes extends AbstractSMPAPIExecutorQuery
{
  public static final String PARAM_XML_SCHEMA_VALIDATION = "xmlSchemaValidation";
  public static final String PARAM_BUSINESS_CARD = "businessCard";

  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorQueryGetDocTypes.class);

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sLogPrefix = "[QueryAPI] ";

    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Is the remote query API disabled?
    if (SMPServerConfiguration.isRestRemoteQueryAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The remote query API is disabled. getRemoteDocTypes will not be executed",
                                                aDataProvider.getCurrentURI ());
    }
    final IIdentifierFactory aIF = SMPMetaManager.getIdentifierFactory ();
    final ESMPAPIType eAPIType = SMPServerConfiguration.getRESTType ().getAPIType ();
    final ISMLInfo aSMLInfo = SMPMetaManager.getSettings ().getSMLInfo ();
    if (aSMLInfo == null)
    {
      throw new SMPPreconditionFailedException ("Currently no SML is available. Please select it in the UI at the 'SMP Settings' page",
                                                aDataProvider.getCurrentURI ());
    }

    final IParticipantIdentifier aParticipantID = aIF.parseParticipantIdentifier (sPathServiceGroupID);
    if (aParticipantID == null)
      throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());

    final SMPQueryParams aSMPQueryParams = SMPQueryParams.createForSMLOrNull (aSMLInfo,
                                                                              eAPIType,
                                                                              aIF,
                                                                              aParticipantID.getScheme (),
                                                                              aParticipantID.getValue (),
                                                                              true);
    if (aSMPQueryParams == null)
    {
      LOGGER.error (sLogPrefix + "Participant ID '" + sPathServiceGroupID + "' is not registered in the DNS");
      aUnifiedResponse.createNotFound ();
      return;
    }

    final boolean bQueryBusinessCard = aRequestScope.params ().getAsBoolean (PARAM_BUSINESS_CARD, false);
    final boolean bXMLSchemaValidation = aRequestScope.params ().getAsBoolean (PARAM_XML_SCHEMA_VALIDATION, true);
    final boolean bVerifySignature = true;

    final ZonedDateTime aQueryDT = PDTFactory.getCurrentZonedDateTimeUTC ();
    final StopWatch aSW = StopWatch.createdStarted ();

    final ISMPClientCreationCallback aSmpCcc = new ISMPClientCreationCallback ()
    {
      public void onPeppolSMPClient (@NonNull final SMPClientReadOnly aSMPClient)
      {}

      public void onBDXR1Client (@NonNull final BDXRClientReadOnly aSMPClient)
      {
        if (SMPServerConfiguration.isHREdeliveryExtensionMode ())
        {
          // Disable server hostname check for Croatia
          aSMPClient.httpClientSettings ().setHostnameVerifierVerifyAll ();
        }
      }

      public void onBDXR2Client (@NonNull final BDXR2ClientReadOnly aSMPClient)
      {}
    };

    // Main querying
    final Wrapper <SMPClientException> aExceptionWrapper = new Wrapper <> ();
    final ICommonsOrderedMap <String, String> aOrigSGHrefs = PeppolAPIHelper.retrieveAllDocumentTypes (sLogPrefix,
                                                                                                       aSMPQueryParams,
                                                                                                       hcs -> {},
                                                                                                       bXMLSchemaValidation,
                                                                                                       bVerifySignature,
                                                                                                       aSmpCcc,
                                                                                                       sHref -> LOGGER.warn (sLogPrefix +
                                                                                                                             "The ServiceGroup list contains the duplicate URL '" +
                                                                                                                             sHref +
                                                                                                                             "'"),
                                                                                                       m -> {},
                                                                                                       ISMPExtensionsCallback.IGNORE,
                                                                                                       aExceptionWrapper::set);

    IJsonObject aJson = null;
    if (aOrigSGHrefs != null)
    {
      final ICommonsSortedMap <String, String> aSGHrefs = new CommonsTreeMap <> (aOrigSGHrefs);
      aJson = SMPJsonResponse.convert (eAPIType, aParticipantID, aSGHrefs, aIF);
    }

    if (bQueryBusinessCard)
    {
      // Retrieve Business Card as well
      final Wrapper <Exception> aBCExceptionWrapper = new Wrapper <> ();
      final PDBusinessCard aBC = PeppolAPIHelper.retrieveBusinessCardParsed (sLogPrefix,
                                                                             aSMPQueryParams,
                                                                             hcs -> {},
                                                                             new FeedbackCallbackLog (LOGGER,
                                                                                                      sLogPrefix),
                                                                             aBCExceptionWrapper::set);
      if (aBC != null)
      {
        // Business Card found
        if (aJson == null)
          aJson = new JsonObject ();
        aJson.add (PARAM_BUSINESS_CARD, aBC.getAsJson ());
      }
    }

    aSW.stop ();

    if (aJson == null)
    {
      LOGGER.error (sLogPrefix + "Failed to perform the SMP lookup");
      aUnifiedResponse.createNotFound ();
    }
    else
    {
      LOGGER.info (sLogPrefix + "Succesfully finished lookup lookup after " + aSW.getMillis () + " milliseconds");

      aJson.add ("queryDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME.format (aQueryDT));
      aJson.add ("queryDurationMillis", aSW.getMillis ());

      aUnifiedResponse.json (aJson).enableCaching (1 * CGlobal.SECONDS_PER_HOUR);
    }
  }
}
