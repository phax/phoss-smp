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
import com.helger.base.array.ArrayHelper;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.json.IJson;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonReader;
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
 * REST API executor for bulk changing the endpoint technical contact URL. The same logic as in the UI page
 * "Bulk change technical contact URL".
 *
 * @author Philip Helger
 * @since 8.7.0
 */
public final class APIExecutorEndpointTechnicalContactURLBulkChangePost extends AbstractSMPAPIExecutor
{
  /** Query parameter: if "true" the call returns after the change was performed */
  public static final String PARAM_WAIT = "wait";

  private static final String JSON_SERVICE_GROUP_ID = "serviceGroupID";
  private static final String JSON_OLD_URL = "oldURL";
  private static final String JSON_NEW_URL = "newURL";

  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorEndpointTechnicalContactURLBulkChangePost.class);

  private static final class BulkChangeEndpointTechnicalContactURLJob extends AbstractLongRunningJobRunnable
  {
    private final IParticipantIdentifier m_aServiceGroupPID;
    private final String m_sOldURL;
    private final String m_sNewURL;

    BulkChangeEndpointTechnicalContactURLJob (@Nullable final IParticipantIdentifier aServiceGroupPID,
                              @NonNull @Nonempty final String sOldURL,
                              @NonNull @Nonempty final String sNewURL,
                              @Nullable final String sUserID)
    {
      super ("BulkChangeEndpointTechnicalContactURL",
             new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Bulk change endpoint technical contact URL"),
             () -> sUserID);
      m_aServiceGroupPID = aServiceGroupPID;
      m_sOldURL = sOldURL;
      m_sNewURL = sNewURL;
    }

    @NonNull
    public LongRunningJobResult createLongRunningJobResult ()
    {
      final long nChanged = _executeBulkChange (m_aServiceGroupPID, m_sOldURL, m_sNewURL);
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
        SMPBulkChangeLocks.ENDPOINT_TECHNICAL_CONTACT_URL.release ();
      }
    }
  }

  private static long _executeBulkChange (@Nullable final IParticipantIdentifier aServiceGroupPID,
                                          @NonNull final String sOldURL,
                                          @NonNull final String sNewURL)
  {
    final StopWatch aSW = StopWatch.createdStarted ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final long nEndpointsChanged = aServiceInfoMgr.updateAllEndpointTechnicalContactUrls (aServiceGroupPID, sOldURL, sNewURL);
    if (nEndpointsChanged > 0)
    {
      // The bulk update does not trigger the callbacks and affects an unknown number of
      // participants
      SMPRestResponseCache.invalidateAll ();
    }
    aSW.stop ();
    LOGGER.info ("Bulk endpoint technical contact URL change completed: " +
                 nEndpointsChanged +
                 " endpoint(s) changed in " +
                 aSW.getMillis () +
                 " ms");
    return nEndpointsChanged;
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
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. The bulk endpoint technical contact URL change will not be executed.",
                                                aDataProvider.getCurrentURI ());
    }

    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());
    final IUser aUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);

    // Read payload
    final byte [] aPayloadBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    if (ArrayHelper.isEmpty (aPayloadBytes))
      throw new SMPBadRequestException ("No request body provided", aDataProvider.getCurrentURI ());

    // Read payload as JSON
    final IJson aPayload = JsonReader.builder ().source (aPayloadBytes).read ();
    if (aPayload == null)
      throw new SMPBadRequestException ("Failed to parse request body as JSON", aDataProvider.getCurrentURI ());
    if (!aPayload.isObject ())
      throw new SMPBadRequestException ("Failed to parse request body as JSON object", aDataProvider.getCurrentURI ());

    final IJsonObject aRequestJson = aPayload.getAsObject ();
    final String sServiceGroupID = StringHelper.trim (aRequestJson.getAsString (JSON_SERVICE_GROUP_ID));
    final String sOldURL = StringHelper.trim (aRequestJson.getAsString (JSON_OLD_URL));
    final String sNewURL = StringHelper.trim (aRequestJson.getAsString (JSON_NEW_URL));

    if (StringHelper.isEmpty (sOldURL))
      throw new SMPBadRequestException ("Field '" + JSON_OLD_URL + "' is required", aDataProvider.getCurrentURI ());
    if (StringHelper.isEmpty (sNewURL))
      throw new SMPBadRequestException ("Field '" + JSON_NEW_URL + "' is required", aDataProvider.getCurrentURI ());
    if (sNewURL.equals (sOldURL))
      throw new SMPBadRequestException ("The new technical contact URL is identical to the old technical contact URL", aDataProvider.getCurrentURI ());

    // Optional restriction to a single service group
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

    final boolean bWait = aRequestScope.params ().getAsBoolean (PARAM_WAIT, false);

    // Only a single bulk change may run at a time
    if (!SMPBulkChangeLocks.ENDPOINT_TECHNICAL_CONTACT_URL.tryAcquire (aUser.getID ()))
    {
      throw new SMPServiceUnavailableException ("Another bulk change of the endpoint technical contact URL is already running. Please try again later",
                                                aDataProvider.getCurrentURI ());
    }

    if (bWait)
    {
      try
      {
        final long nChanged = _executeBulkChange (aServiceGroupPID, sOldURL, sNewURL);
        aUnifiedResponse.json (new JsonObject ().add ("status", "completed").add ("endpointsChanged", nChanged))
                        .createOk ();
      }
      finally
      {
        SMPBulkChangeLocks.ENDPOINT_TECHNICAL_CONTACT_URL.release ();
      }
      return;
    }

    final BulkChangeEndpointTechnicalContactURLJob aJob = new BulkChangeEndpointTechnicalContactURLJob (aServiceGroupPID,
                                                                        sOldURL,
                                                                        sNewURL,
                                                                        aUser.getID ());
    try
    {
      PhotonWorkerPool.getInstance ().run ("BulkChangeEndpointTechnicalContactURL", aJob);
    }
    catch (final RuntimeException ex)
    {
      // The job was never started, so it can never release the lock
      SMPBulkChangeLocks.ENDPOINT_TECHNICAL_CONTACT_URL.release ();
      throw ex;
    }

    LOGGER.info ("Started bulk endpoint technical contact URL change job in the background");
    aUnifiedResponse.json (new JsonObject ().add ("status", "accepted")).createAccepted ();
  }
}
