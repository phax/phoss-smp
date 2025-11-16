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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.CGlobal;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.datetime.helper.PDTFactory;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.json.IJsonObject;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.helper.PDBusinessCardHelper;
import com.helger.peppol.sml.ESMPAPIType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.smpclient.httpclient.SMPHttpClientSettings;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Implementation of the REST API to query a BusinessCard from a remote SMP.
 *
 * @author Philip Helger
 */
public final class APIExecutorQueryGetBusinessCard extends AbstractSMPAPIExecutorQuery
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorQueryGetBusinessCard.class);

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
      throw new SMPPreconditionFailedException ("The remote query API is disabled. getRemoteBusinessCard will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    final IIdentifierFactory aIF = SMPMetaManager.getIdentifierFactory ();
    final ESMPAPIType eAPIType = SMPServerConfiguration.getRESTType ().getAPIType ();

    final IParticipantIdentifier aParticipantID = aIF.parseParticipantIdentifier (sPathServiceGroupID);
    if (aParticipantID == null)
    {
      throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());
    }
    final SMPQueryParams aQueryParams = SMPQueryParams.create (eAPIType, aParticipantID);
    if (aQueryParams == null)
    {
      LOGGER.error (sLogPrefix + "Failed to perform the BusinessCard SMP lookup");
      aUnifiedResponse.createNotFound ();
      return;
    }

    final ZonedDateTime aQueryDT = PDTFactory.getCurrentZonedDateTimeUTC ();
    final StopWatch aSW = StopWatch.createdStarted ();

    LOGGER.info (sLogPrefix +
                 "BusinessCard of '" +
                 aParticipantID.getURIEncoded () +
                 "' is queried using SMP API '" +
                 eAPIType +
                 "' from '" +
                 aQueryParams.getSMPHostURI () +
                 "'");

    final SMPHttpClientSettings aHCS = new SMPHttpClientSettings ();

    final String sBCURL = StringHelper.trimEnd (aQueryParams.getSMPHostURI ().toString (), '/') +
                          "/businesscard/" +
                          aParticipantID.getURIEncoded ();
    LOGGER.info (sLogPrefix + "Querying BC from '" + sBCURL + "'");

    byte [] aData;
    try (final HttpClientManager aHttpClientMgr = HttpClientManager.create (aHCS))
    {
      final HttpGet aGet = new HttpGet (sBCURL);
      aData = aHttpClientMgr.execute (aGet, new ResponseHandlerByteArray ());
    }
    catch (final Exception ex)
    {
      aData = null;
    }

    IJsonObject aJson = null;
    if (aData == null)
    {
      LOGGER.warn (sLogPrefix + "No Business Card is available for that participant.");
    }
    else
    {
      final PDBusinessCard aBC = PDBusinessCardHelper.parseBusinessCard (aData, (Charset) null);
      if (aBC == null)
      {
        LOGGER.error (sLogPrefix + "Failed to parse BC:\n" + new String (aData, StandardCharsets.UTF_8));
      }
      else
      {
        // Business Card found
        aJson = aBC.getAsJson ();
      }
    }

    aSW.stop ();

    if (aJson == null)
    {
      LOGGER.error (sLogPrefix + "Failed to perform the BusinessCard SMP lookup");
      aUnifiedResponse.createNotFound ();
    }
    else
    {
      LOGGER.info (sLogPrefix +
                   "Succesfully finished BusinessCard lookup lookup after " +
                   aSW.getMillis () +
                   " milliseconds");

      aJson.add ("queryDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME.format (aQueryDT));
      aJson.add ("queryDurationMillis", aSW.getMillis ());

      aUnifiedResponse.json (aJson).enableCaching (1 * CGlobal.SECONDS_PER_HOUR);
    }
  }
}
