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

import java.security.cert.X509Certificate;
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
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.exception.SMPServiceUnavailableException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.phoss.smp.restapi.cache.SMPRestResponseCache;
import com.helger.phoss.smp.security.SMPCertificateHelper;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.user.IUser;
import com.helger.security.certificate.CertificateDecodeHelper;
import com.helger.text.ReadOnlyMultilingualText;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.scope.mgr.WebScoped;

import jakarta.annotation.Nullable;

/**
 * REST API executor for bulk changing the endpoint certificate. The same logic as in the UI page
 * "Bulk change certificate".
 *
 * @author Philip Helger
 * @since 8.6.3
 */
public final class APIExecutorEndpointCertificateBulkChangePost extends AbstractSMPAPIExecutor
{
  /** Query parameter: if "true" the call returns after the change was performed */
  public static final String PARAM_WAIT = "wait";

  private static final String JSON_OLD_CERTIFICATE = "oldCertificate";
  private static final String JSON_NEW_CERTIFICATE = "newCertificate";

  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorEndpointCertificateBulkChangePost.class);

  private static final class BulkChangeEndpointCertificateJob extends AbstractLongRunningJobRunnable
  {
    private final String m_sOldUnifiedCert;
    private final String m_sNewCert;

    BulkChangeEndpointCertificateJob (@NonNull @Nonempty final String sOldUnifiedCert,
                                      @NonNull @Nonempty final String sNewCert,
                                      @Nullable final String sUserID)
    {
      super ("BulkChangeEndpointCertificate",
             new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Bulk change endpoint certificate"),
             () -> sUserID);
      m_sOldUnifiedCert = sOldUnifiedCert;
      m_sNewCert = sNewCert;
    }

    @NonNull
    public LongRunningJobResult createLongRunningJobResult ()
    {
      final long nChanged = _executeBulkChange (m_sOldUnifiedCert, m_sNewCert);
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
        SMPBulkChangeLocks.ENDPOINT_CERTIFICATE.release ();
      }
    }
  }

  private static long _executeBulkChange (@NonNull final String sOldUnifiedCert, @NonNull final String sNewCert)
  {
    final StopWatch aSW = StopWatch.createdStarted ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final long nEndpointsChanged = aServiceInfoMgr.updateAllEndpointCertificates (sOldUnifiedCert, sNewCert);
    if (nEndpointsChanged > 0)
    {
      // The bulk update does not trigger the callbacks and affects an unknown number of
      // participants
      SMPRestResponseCache.invalidateAll ();
    }
    aSW.stop ();
    LOGGER.info ("Bulk endpoint certificate change completed: " +
                 nEndpointsChanged +
                 " endpoint(s) changed in " +
                 aSW.getMillis () +
                 " ms");
    return nEndpointsChanged;
  }

  @Nullable
  private static String _getCertificateParsingError (@NonNull final String sCert)
  {
    X509Certificate aEndpointCert = null;
    try
    {
      aEndpointCert = new CertificateDecodeHelper ().source (sCert).pemEncoded (true).getDecodedOrThrow ();
    }
    catch (final Exception ex)
    {
      return ex.getMessage ();
    }
    return aEndpointCert != null ? null : "Invalid input string provided";
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
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. The bulk endpoint certificate change will not be executed.",
                                                aDataProvider.getCurrentURI ());
    }

    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());
    final IUser aUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);

    final byte [] aPayloadBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    if (ArrayHelper.isEmpty (aPayloadBytes))
      throw new SMPBadRequestException ("No request body provided", aDataProvider.getCurrentURI ());

    final IJson aPayload = JsonReader.builder ().source (aPayloadBytes).read ();
    if (aPayload == null)
      throw new SMPBadRequestException ("Failed to parse request body as JSON", aDataProvider.getCurrentURI ());
    if (!aPayload.isObject ())
      throw new SMPBadRequestException ("Failed to parse request body as JSON object", aDataProvider.getCurrentURI ());

    final IJsonObject aRequestJson = aPayload.getAsObject ();
    final String sOldCert = StringHelper.trim (aRequestJson.getAsString (JSON_OLD_CERTIFICATE));
    final String sNewCert = StringHelper.trim (aRequestJson.getAsString (JSON_NEW_CERTIFICATE));
    final String sOldUnifiedCert = SMPCertificateHelper.getNormalizedCert (sOldCert);
    final String sNewUnifiedCert = SMPCertificateHelper.getNormalizedCert (sNewCert);

    if (StringHelper.isEmpty (sOldUnifiedCert))
      throw new SMPBadRequestException ("Field '" + JSON_OLD_CERTIFICATE + "' is required",
                                        aDataProvider.getCurrentURI ());
    String sErrorDetails = _getCertificateParsingError (sOldUnifiedCert);
    if (sErrorDetails != null)
      throw new SMPBadRequestException ("The old certificate is invalid: " + sErrorDetails,
                                        aDataProvider.getCurrentURI ());

    if (StringHelper.isEmpty (sNewUnifiedCert))
      throw new SMPBadRequestException ("Field '" + JSON_NEW_CERTIFICATE + "' is required",
                                        aDataProvider.getCurrentURI ());
    sErrorDetails = _getCertificateParsingError (sNewUnifiedCert);
    if (sErrorDetails != null)
      throw new SMPBadRequestException ("The new certificate is invalid: " + sErrorDetails,
                                        aDataProvider.getCurrentURI ());
    if (sNewUnifiedCert.equals (sOldUnifiedCert))
      throw new SMPBadRequestException ("The new certificate is identical to the old certificate",
                                        aDataProvider.getCurrentURI ());

    final boolean bWait = aRequestScope.params ().getAsBoolean (PARAM_WAIT, false);

    // Only a single bulk change may run at a time
    if (!SMPBulkChangeLocks.ENDPOINT_CERTIFICATE.tryAcquire (aUser.getID ()))
    {
      throw new SMPServiceUnavailableException ("Another bulk change of the endpoint certificate is already running. Please try again later",
                                                aDataProvider.getCurrentURI ());
    }

    if (bWait)
    {
      try
      {
        final long nChanged = _executeBulkChange (sOldUnifiedCert, sNewCert);
        aUnifiedResponse.json (new JsonObject ().add ("status", "completed").add ("endpointsChanged", nChanged))
                        .createOk ();
      }
      finally
      {
        SMPBulkChangeLocks.ENDPOINT_CERTIFICATE.release ();
      }
      return;
    }

    final BulkChangeEndpointCertificateJob aJob = new BulkChangeEndpointCertificateJob (sOldUnifiedCert,
                                                                                        sNewCert,
                                                                                        aUser.getID ());
    try
    {
      PhotonWorkerPool.getInstance ().run ("BulkChangeEndpointCertificate", aJob);
    }
    catch (final RuntimeException ex)
    {
      // The job was never started, so it can never release the lock
      SMPBulkChangeLocks.ENDPOINT_CERTIFICATE.release ();
      throw ex;
    }

    LOGGER.info ("Started bulk endpoint certificate change job in the background");
    aUnifiedResponse.json (new JsonObject ().add ("status", "accepted")).createAccepted ();
  }
}
