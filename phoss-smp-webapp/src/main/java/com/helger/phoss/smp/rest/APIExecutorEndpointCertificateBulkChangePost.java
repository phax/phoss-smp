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
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
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
 * REST API executor for bulk changing endpoint AP certificates.
 *
 * @author Philip Helger
 * @since 8.1.4
 */
public final class APIExecutorEndpointCertificateBulkChangePost extends AbstractSMPAPIExecutor
{
  public static final String PARAM_WAIT_FOR_COMPLETION = "waitForCompletion";
  public static final String PARAM_WAIT = "wait";

  private static final String JSON_OLD_CERTIFICATE = "oldCertificate";
  private static final String JSON_OLD_CERTIFICATE_PEM = "oldCertificatePem";
  private static final String JSON_NEW_CERTIFICATE = "newCertificate";
  private static final String JSON_NEW_CERTIFICATE_PEM = "newCertificatePem";

  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorEndpointCertificateBulkChangePost.class);

  private static final class BulkChangeEndpointCertificateJob extends AbstractLongRunningJobRunnable
  {
    private final String m_sOldUnifiedCert;
    private final String m_sNewCert;

    BulkChangeEndpointCertificateJob (@NonNull final String sOldUnifiedCert,
                                      @NonNull final String sNewCert,
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
      try (final WebScoped aWebScoped = new WebScoped ())
      {
        return LongRunningJobResult.createJson (_executeBulkChange (m_sOldUnifiedCert, m_sNewCert).getAsJson ());
      }
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
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. bulk endpoint certificate change will not be executed",
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
    final String sOldCert = _getString (aRequestJson, JSON_OLD_CERTIFICATE, JSON_OLD_CERTIFICATE_PEM);
    final String sNewCert = _getString (aRequestJson, JSON_NEW_CERTIFICATE, JSON_NEW_CERTIFICATE_PEM);

    final String sOldUnifiedCert = SMPCertificateHelper.getNormalizedCert (sOldCert);
    final String sNewUnifiedCert = SMPCertificateHelper.getNormalizedCert (sNewCert);

    if (StringHelper.isEmpty (sOldUnifiedCert))
      throw new SMPBadRequestException ("Field '" + JSON_OLD_CERTIFICATE + "' is required", aDataProvider.getCurrentURI ());

    String sErrorDetails = _getCertificateParsingError (sOldUnifiedCert);
    if (sErrorDetails != null)
      throw new SMPBadRequestException ("The old certificate is invalid: " + sErrorDetails, aDataProvider.getCurrentURI ());

    if (StringHelper.isEmpty (sNewUnifiedCert))
      throw new SMPBadRequestException ("Field '" + JSON_NEW_CERTIFICATE + "' is required", aDataProvider.getCurrentURI ());

    sErrorDetails = _getCertificateParsingError (sNewUnifiedCert);
    if (sErrorDetails != null)
      throw new SMPBadRequestException ("The new certificate is invalid: " + sErrorDetails, aDataProvider.getCurrentURI ());

    if (sNewUnifiedCert.equals (sOldUnifiedCert))
      throw new SMPBadRequestException ("The new certificate is identical to the old certificate",
                                        aDataProvider.getCurrentURI ());

    final boolean bWaitForCompletion = aRequestScope.params ()
                                                    .getAsBoolean (PARAM_WAIT_FOR_COMPLETION,
                                                                   aRequestScope.params ()
                                                                                .getAsBoolean (PARAM_WAIT,
                                                                                               _getBoolean (aRequestJson,
                                                                                                            PARAM_WAIT_FOR_COMPLETION,
                                                                                                            _getBoolean (aRequestJson,
                                                                                                                         PARAM_WAIT,
                                                                                                                         false))));

    if (bWaitForCompletion)
    {
      final BulkChangeEndpointCertificateResult aResult = _executeBulkChange (sOldUnifiedCert, StringHelper.trim (sNewCert));
      aUnifiedResponse.json (aResult.getAsJson ().add (PARAM_WAIT_FOR_COMPLETION, true)).createOk ();
      return;
    }

    final BulkChangeEndpointCertificateJob aJob = new BulkChangeEndpointCertificateJob (sOldUnifiedCert,
                                                                                       StringHelper.trim (sNewCert),
                                                                                       aUser.getID ());
    PhotonWorkerPool.getInstance ().run ("BulkChangeEndpointCertificate", aJob);

    LOGGER.info ("Started bulk endpoint certificate change job '{}'", aJob.getJobID ());

    aUnifiedResponse.json (new JsonObject ().add ("status", "accepted")
                                            .add (PARAM_WAIT_FOR_COMPLETION, false)
                                            .add ("jobId", aJob.getJobID ())
                                            .add ("message",
                                                  "The bulk change of the endpoint certificate is running in the background."))
                    .createAccepted ();
  }

  @Nullable
  private static String _getString (@NonNull final IJsonObject aJson, @NonNull final String... aFieldNames)
  {
    for (final String sFieldName : aFieldNames)
    {
      final Object aValue = aJson.getValue (sFieldName);
      if (aValue instanceof final String sValue)
        return StringHelper.trim (sValue);
    }
    return null;
  }

  private static boolean _getBoolean (@NonNull final IJsonObject aJson,
                                      @NonNull final String sFieldName,
                                      final boolean bDefault)
  {
    final Object aValue = aJson.getValue (sFieldName);
    if (aValue instanceof final Boolean aBoolean)
      return aBoolean.booleanValue ();
    if (aValue instanceof final String sValue)
    {
      final String sTrimmed = sValue.trim ();
      if ("true".equalsIgnoreCase (sTrimmed) || "1".equals (sTrimmed) || "yes".equalsIgnoreCase (sTrimmed))
        return true;
      if ("false".equalsIgnoreCase (sTrimmed) || "0".equals (sTrimmed) || "no".equalsIgnoreCase (sTrimmed))
        return false;
    }
    return bDefault;
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
      return StringHelper.isNotEmpty (ex.getMessage ()) ? ex.getMessage () : ex.getClass ().getName ();
    }
    return aEndpointCert != null ? null : "Invalid input string provided";
  }

  @NonNull
  private static BulkChangeEndpointCertificateResult _executeBulkChange (@NonNull final String sOldUnifiedCert,
                                                                         @NonNull final String sNewCert)
  {
    final StopWatch aSW = StopWatch.createdStarted ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final long nEndpointsChanged = aServiceInfoMgr.updateAllEndpointCertificates (sOldUnifiedCert, sNewCert);
    aSW.stop ();

    LOGGER.info ("Bulk endpoint certificate change completed: {} endpoint(s) changed in {} ms",
                 Long.valueOf (nEndpointsChanged),
                 Long.valueOf (aSW.getMillis ()));

    return new BulkChangeEndpointCertificateResult (nEndpointsChanged, aSW.getMillis ());
  }

  private static final class BulkChangeEndpointCertificateResult
  {
    private final long m_nEndpointsChanged;
    private final long m_nDurationMillis;

    BulkChangeEndpointCertificateResult (final long nEndpointsChanged, final long nDurationMillis)
    {
      m_nEndpointsChanged = nEndpointsChanged;
      m_nDurationMillis = nDurationMillis;
    }

    @NonNull
    IJsonObject getAsJson ()
    {
      return new JsonObject ().add ("status", "completed")
                              .add ("endpointsChanged", m_nEndpointsChanged)
                              .add ("durationMillis", m_nDurationMillis);
    }
  }
}
