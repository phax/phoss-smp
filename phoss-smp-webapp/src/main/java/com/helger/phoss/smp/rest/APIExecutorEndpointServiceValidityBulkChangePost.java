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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.array.ArrayHelper;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.json.IJson;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.exception.SMPServiceUnavailableException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.phoss.smp.restapi.cache.SMPRestResponseCache;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.user.IUser;
import com.helger.text.ReadOnlyMultilingualText;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.scope.mgr.WebScoped;

import jakarta.annotation.Nullable;

/**
 * REST API executor for bulk changing the service activation date and/or the service expiration
 * date of all endpoints of a document type. The same logic as in the UI page "Bulk change service
 * validity".
 *
 * @author Philip Helger
 * @since 8.6.3
 */
public final class APIExecutorEndpointServiceValidityBulkChangePost extends AbstractSMPAPIExecutor
{
  /** Query parameter: if "true" the call returns after the change was performed */
  public static final String PARAM_WAIT = "wait";

  private static final String JSON_DOCUMENT_TYPE_ID = "documentTypeID";
  private static final String JSON_SERVICE_GROUP_ID = "serviceGroupID";
  private static final String JSON_SERVICE_ACTIVATION_DATE = "serviceActivationDate";
  private static final String JSON_SERVICE_EXPIRATION_DATE = "serviceExpirationDate";

  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorEndpointServiceValidityBulkChangePost.class);

  private static final class BulkChangeEndpointServiceValidityJob extends AbstractLongRunningJobRunnable
  {
    private final IDocumentTypeIdentifier m_aDocTypeID;
    private final IParticipantIdentifier m_aServiceGroupPID;
    private final boolean m_bChangeActivation;
    private final XMLOffsetDateTime m_aNewActivation;
    private final boolean m_bChangeExpiration;
    private final XMLOffsetDateTime m_aNewExpiration;

    BulkChangeEndpointServiceValidityJob (@NonNull final IDocumentTypeIdentifier aDocTypeID,
                                          @Nullable final IParticipantIdentifier aServiceGroupPID,
                                          final boolean bChangeActivation,
                                          @Nullable final XMLOffsetDateTime aNewActivation,
                                          final boolean bChangeExpiration,
                                          @Nullable final XMLOffsetDateTime aNewExpiration,
                                          @Nullable final String sUserID)
    {
      super ("BulkChangeEndpointServiceValidity",
             new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Bulk change endpoint service validity"),
             () -> sUserID);
      m_aDocTypeID = aDocTypeID;
      m_aServiceGroupPID = aServiceGroupPID;
      m_bChangeActivation = bChangeActivation;
      m_aNewActivation = aNewActivation;
      m_bChangeExpiration = bChangeExpiration;
      m_aNewExpiration = aNewExpiration;
    }

    @NonNull
    public LongRunningJobResult createLongRunningJobResult ()
    {
      final long nChanged = _executeBulkChange (m_aDocTypeID,
                                                m_aServiceGroupPID,
                                                m_bChangeActivation,
                                                m_aNewActivation,
                                                m_bChangeExpiration,
                                                m_aNewExpiration);
      return LongRunningJobResult.createText ("Changed " + nChanged + " endpoint(s)");
    }

    @Override
    public void run ()
    {
      try (final WebScoped w = new WebScoped ())
      {
        super.run ();
      }
      finally
      {
        // Always release, even if the job failed
        SMPBulkChangeLocks.ENDPOINT_SERVICE_VALIDITY.release ();
      }
    }
  }

  private static long _executeBulkChange (@NonNull final IDocumentTypeIdentifier aDocTypeID,
                                          @Nullable final IParticipantIdentifier aServiceGroupPID,
                                          final boolean bChangeActivation,
                                          @Nullable final XMLOffsetDateTime aNewActivation,
                                          final boolean bChangeExpiration,
                                          @Nullable final XMLOffsetDateTime aNewExpiration)
  {
    final StopWatch aSW = StopWatch.createdStarted ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final long nEndpointsChanged = aServiceInfoMgr.updateAllEndpointServiceValidity (aDocTypeID,
                                                                                     aServiceGroupPID,
                                                                                     bChangeActivation,
                                                                                     aNewActivation,
                                                                                     bChangeExpiration,
                                                                                     aNewExpiration);
    if (nEndpointsChanged > 0)
    {
      // The bulk update does not trigger the callbacks and affects an unknown number of participants
      SMPRestResponseCache.invalidateAll ();
    }
    aSW.stop ();
    LOGGER.info ("Bulk endpoint service validity change completed: " +
                 nEndpointsChanged +
                 " endpoint(s) changed in " +
                 aSW.getMillis () +
                 " ms");
    return nEndpointsChanged;
  }

  @Nullable
  private static LocalDate _parseDate (@NonNull final IJsonObject aRequestJson,
                                       @NonNull final String sFieldName,
                                       @NonNull final ISMPServerAPIDataProvider aDataProvider) throws SMPBadRequestException
  {
    final String sValue = StringHelper.trim (aRequestJson.getAsString (sFieldName));
    if (StringHelper.isEmpty (sValue))
      return null;
    try
    {
      return LocalDate.parse (sValue);
    }
    catch (final DateTimeParseException ex)
    {
      throw new SMPBadRequestException ("Field '" +
                                        sFieldName +
                                        "' must be a date in the format yyyy-MM-dd but is '" +
                                        sValue +
                                        "'",
                                        aDataProvider.getCurrentURI ());
    }
  }

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. The bulk endpoint service validity change will not be executed.",
                                                aDataProvider.getCurrentURI ());
    }

    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());
    final IUser aUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);

    final byte [] aPayloadBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    if (ArrayHelper.isEmpty (aPayloadBytes))
      throw new SMPBadRequestException ("No request body provided", aDataProvider.getCurrentURI ());

    final IJson aPayload = JsonReader.readFromString (new String (aPayloadBytes, StandardCharsets.UTF_8));
    if (aPayload == null || !aPayload.isObject ())
      throw new SMPBadRequestException ("Failed to parse request body as JSON object", aDataProvider.getCurrentURI ());

    final IJsonObject aRequestJson = aPayload.getAsObject ();

    // Document type is the selection criterion
    final String sDocTypeID = StringHelper.trim (aRequestJson.getAsString (JSON_DOCUMENT_TYPE_ID));
    if (StringHelper.isEmpty (sDocTypeID))
      throw new SMPBadRequestException ("Field '" + JSON_DOCUMENT_TYPE_ID + "' is required",
                                        aDataProvider.getCurrentURI ());
    final IDocumentTypeIdentifier aDocTypeID = SMPMetaManager.getIdentifierFactory ()
                                                             .parseDocumentTypeIdentifier (sDocTypeID);
    if (aDocTypeID == null)
      throw new SMPBadRequestException ("The provided document type ID '" + sDocTypeID + "' is invalid",
                                        aDataProvider.getCurrentURI ());

    // Optional restriction to a single service group
    final String sServiceGroupID = StringHelper.trim (aRequestJson.getAsString (JSON_SERVICE_GROUP_ID));
    IParticipantIdentifier aServiceGroupPID = null;
    if (StringHelper.isNotEmpty (sServiceGroupID))
    {
      aServiceGroupPID = SMPMetaManager.getIdentifierFactory ().parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupPID == null)
        throw new SMPBadRequestException ("The provided service group ID '" + sServiceGroupID + "' is invalid",
                                          aDataProvider.getCurrentURI ());
      if (SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aServiceGroupPID) == null)
        throw new SMPNotFoundException ("The service group '" + sServiceGroupID + "' does not exist",
                                        aDataProvider.getCurrentURI ());
    }

    // A missing field means "keep unchanged", a null or empty value means "remove the date"
    final boolean bChangeActivation = aRequestJson.containsKey (JSON_SERVICE_ACTIVATION_DATE);
    final boolean bChangeExpiration = aRequestJson.containsKey (JSON_SERVICE_EXPIRATION_DATE);
    if (!bChangeActivation && !bChangeExpiration)
      throw new SMPBadRequestException ("At least one of the fields '" +
                                        JSON_SERVICE_ACTIVATION_DATE +
                                        "' and '" +
                                        JSON_SERVICE_EXPIRATION_DATE +
                                        "' is required",
                                        aDataProvider.getCurrentURI ());

    final LocalDate aNewActivationDate = bChangeActivation ? _parseDate (aRequestJson,
                                                                         JSON_SERVICE_ACTIVATION_DATE,
                                                                         aDataProvider) : null;
    final LocalDate aNewExpirationDate = bChangeExpiration ? _parseDate (aRequestJson,
                                                                         JSON_SERVICE_EXPIRATION_DATE,
                                                                         aDataProvider) : null;
    if (aNewActivationDate != null && aNewExpirationDate != null && aNewActivationDate.isAfter (aNewExpirationDate))
      throw new SMPBadRequestException ("'" +
                                        JSON_SERVICE_ACTIVATION_DATE +
                                        "' must not be after '" +
                                        JSON_SERVICE_EXPIRATION_DATE +
                                        "'",
                                        aDataProvider.getCurrentURI ());

    final XMLOffsetDateTime aNewActivation = PDTFactory.createXMLOffsetDateTime (aNewActivationDate);
    final XMLOffsetDateTime aNewExpiration = PDTFactory.createXMLOffsetDateTime (aNewExpirationDate);

    final boolean bWait = aRequestScope.params ().getAsBoolean (PARAM_WAIT, false);

    // Only a single bulk change may run at a time
    if (!SMPBulkChangeLocks.ENDPOINT_SERVICE_VALIDITY.tryAcquire (aUser.getID ()))
    {
      throw new SMPServiceUnavailableException ("Another bulk change of the endpoint service validity is already running. Please try again later",
                                                aDataProvider.getCurrentURI ());
    }

    if (bWait)
    {
      try
      {
        final long nChanged = _executeBulkChange (aDocTypeID,
                                                  aServiceGroupPID,
                                                  bChangeActivation,
                                                  aNewActivation,
                                                  bChangeExpiration,
                                                  aNewExpiration);
        aUnifiedResponse.json (new JsonObject ().add ("status", "completed").add ("endpointsChanged", nChanged))
                        .createOk ();
      }
      finally
      {
        SMPBulkChangeLocks.ENDPOINT_SERVICE_VALIDITY.release ();
      }
      return;
    }

    final BulkChangeEndpointServiceValidityJob aJob = new BulkChangeEndpointServiceValidityJob (aDocTypeID,
                                                                                               aServiceGroupPID,
                                                                                               bChangeActivation,
                                                                                               aNewActivation,
                                                                                               bChangeExpiration,
                                                                                               aNewExpiration,
                                                                                               aUser.getID ());
    try
    {
      PhotonWorkerPool.getInstance ().run ("BulkChangeEndpointServiceValidity", aJob);
    }
    catch (final RuntimeException ex)
    {
      // The job was never started, so it can never release the lock
      SMPBulkChangeLocks.ENDPOINT_SERVICE_VALIDITY.release ();
      throw ex;
    }

    LOGGER.info ("Started bulk endpoint service validity change job in the background");
    aUnifiedResponse.json (new JsonObject ().add ("status", "accepted")).createAccepted ();
  }
}
