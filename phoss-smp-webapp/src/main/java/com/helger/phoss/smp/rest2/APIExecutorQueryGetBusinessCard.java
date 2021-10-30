/*
 * Copyright (C) 2014-2021 Philip Helger and contributors
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
package com.helger.phoss.smp.rest2;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.CGlobal;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.http.CHttp;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.timing.StopWatch;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.json.IJsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.helper.PDBusinessCardHelper;
import com.helger.peppol.sml.ESMPAPIType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Implementation of the REST API to query a BusinessCard from a remote SMP.
 *
 * @author Philip Helger
 */
public final class APIExecutorQueryGetBusinessCard extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorQueryGetBusinessCard.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    // Is the remote query API disabled?
    if (SMPServerConfiguration.isRestRemoteQueryAPIDisabled ())
    {
      LOGGER.warn ("The remote query API is disabled. getRemoteBusinessCard will not be executed.");
      aUnifiedResponse.setStatus (CHttp.HTTP_NOT_FOUND);
      return;
    }

    final IIdentifierFactory aIF = SMPMetaManager.getIdentifierFactory ();
    final ESMPAPIType eAPIType = SMPServerConfiguration.getRESTType ().getAPIType ();

    final String sParticipantID = aPathVariables.get (Rest2Filter.PARAM_SERVICE_GROUP_ID);
    final IParticipantIdentifier aParticipantID = aIF.parseParticipantIdentifier (sParticipantID);
    if (aParticipantID == null)
      throw SMPBadRequestException.failedToParseSG (sParticipantID, null);

    final SMPQueryParams aQueryParams = SMPQueryParams.create (eAPIType, aParticipantID);

    final ZonedDateTime aQueryDT = PDTFactory.getCurrentZonedDateTimeUTC ();
    final StopWatch aSW = StopWatch.createdStarted ();

    final String sLogPrefix = "[QueryAPI] ";
    LOGGER.info (sLogPrefix +
                 "BusinessCard of '" +
                 aParticipantID.getURIEncoded () +
                 "' is queried using SMP API '" +
                 eAPIType +
                 "' from '" +
                 aQueryParams.getSMPHostURI () +
                 "'");

    IJsonObject aJson = null;

    final String sBCURL = aQueryParams.getSMPHostURI ().toString () + "/businesscard/" + aParticipantID.getURIEncoded ();
    LOGGER.info (sLogPrefix + "Querying BC from '" + sBCURL + "'");
    byte [] aData;
    try (HttpClientManager aHttpClientMgr = new HttpClientManager ())
    {
      final HttpGet aGet = new HttpGet (sBCURL);
      aData = aHttpClientMgr.execute (aGet, new ResponseHandlerByteArray ());
    }
    catch (final Exception ex)
    {
      aData = null;
    }

    if (aData == null)
      LOGGER.warn (sLogPrefix + "No Business Card is available for that participant.");
    else
    {
      final PDBusinessCard aBC = PDBusinessCardHelper.parseBusinessCard (aData, null);
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
      aUnifiedResponse.setStatus (CHttp.HTTP_NOT_FOUND);
    }
    else
    {
      LOGGER.info (sLogPrefix + "Succesfully finished BusinessCard lookup lookup after " + aSW.getMillis () + " milliseconds");

      aJson.add ("queryDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME.format (aQueryDT));
      aJson.add ("queryDurationMillis", aSW.getMillis ());

      final String sRet = new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aJson);
      aUnifiedResponse.setContentAndCharset (sRet, StandardCharsets.UTF_8)
                      .setMimeType (CMimeType.APPLICATION_JSON)
                      .enableCaching (3 * CGlobal.SECONDS_PER_HOUR);
    }
  }
}
