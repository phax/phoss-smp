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
package com.helger.phoss.smp.status;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.commons.timing.StopWatch;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.config.SMPHttpConfiguration;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.servlet.SMPWebAppListener;
import com.helger.phoss.smp.settings.ISMPSettings;

/**
 * The main class to provide the SMP status content.
 *
 * @author Philip Helger
 * @since 5.4.0
 */
@ThreadSafe
public final class SMPStatusProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPStatusProvider.class);
  private static final ICommonsList <ISMPStatusProviderExtensionSPI> LIST = new CommonsArrayList <> ();
  static
  {
    LIST.addAll (ServiceLoaderHelper.getAllSPIImplementations (ISMPStatusProviderExtensionSPI.class));
    LOGGER.info ("Found " +
                 LIST.size () +
                 " implementation(s) of " +
                 ISMPStatusProviderExtensionSPI.class.getSimpleName ());
  }

  private SMPStatusProvider ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public static IJsonObject getDefaultStatusData (final boolean bDisableLongRunningOperations)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Building status data");

    final StopWatch aSW = StopWatch.createdStarted ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();
    final ISMLInfo aSMLInfo = aSettings.getSMLInfo ();

    final IJsonObject aStatusData = new JsonObject ();
    // Since 5.0.7
    aStatusData.add ("build.timestamp", CSMPServer.getBuildTimestamp ());
    // Since 5.3.3
    aStatusData.addIfNotNull ("startup.datetime",
                              PDTWebDateHelper.getAsStringXSD (SMPWebAppListener.getStartupDateTime ()));
    aStatusData.add ("status.datetime", PDTWebDateHelper.getAsStringXSD (aNow));
    aStatusData.add ("status.datetime.utc",
                     PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentOffsetDateTimeUTC ()));
    aStatusData.add ("version.smp", CSMPServer.getVersionNumber ());
    aStatusData.add ("version.java", SystemProperties.getJavaVersion ());
    aStatusData.add ("global.debug", GlobalDebug.isDebugMode ());
    aStatusData.add ("global.production", GlobalDebug.isProductionMode ());
    // Since 5.7.0
    aStatusData.add ("smp.application", CSMP.APPLICATION_TITLE);
    aStatusData.add ("smp.backend", SMPServerConfiguration.getBackend ());
    aStatusData.add ("smp.mode", SMPWebAppConfiguration.isTestVersion () ? "test" : "production");
    aStatusData.add ("smp.resttype", SMPServerConfiguration.getRESTType ().getID ());
    aStatusData.add ("smp.identifiertype", SMPServerConfiguration.getIdentifierType ().getID ());
    aStatusData.add ("smp.id", SMPServerConfiguration.getSMLSMPID ());
    aStatusData.add ("smp.writable-rest-api.enabled", !aSettings.isRESTWritableAPIDisabled ());
    // New in 5.1.0
    aStatusData.add ("smp.publicurl", SMPServerConfiguration.getPublicServerURL ());
    // New in 5.1.0
    aStatusData.add ("smp.forceroot", SMPServerConfiguration.isForceRoot ());
    // New in 5.2.0
    aStatusData.add ("smp.rest.log-exceptions", SMPServerConfiguration.isRESTLogExceptions ());
    // New in 5.2.1
    aStatusData.add ("smp.rest.payload-on-error", SMPServerConfiguration.isRESTPayloadOnError ());

    // SML information
    aStatusData.add ("smp.sml.enabled", aSettings.isSMLEnabled ());
    aStatusData.add ("smp.sml.needed", aSettings.isSMLRequired ());
    if (aSMLInfo != null)
    {
      aStatusData.add ("smp.sml.url", aSMLInfo.getManagementServiceURL ());
      aStatusData.add ("smp.sml.dnszone", aSMLInfo.getDNSZone ());
    }
    final Timeout aCT = SMPServerConfiguration.getSMLConnectionTimeout ();
    if (aCT != null)
      aStatusData.add ("smp.sml.connection-timeout-ms", aCT.toMilliseconds ());
    aStatusData.add ("smp.sml.request-timeout-ms", SMPServerConfiguration.getSMLRequestTimeout ().toMilliseconds ());

    // Directory information
    aStatusData.add ("smp.pd.enabled", aSettings.isDirectoryIntegrationEnabled ());
    // New in 5.1.0
    aStatusData.add ("smp.pd.needed", aSettings.isDirectoryIntegrationRequired ());
    aStatusData.add ("smp.pd.auto-update", aSettings.isDirectoryIntegrationAutoUpdate ());
    aStatusData.add ("smp.pd.hostname", aSettings.getDirectoryHostName ());

    // Certificate information
    final boolean bCertConfigOk = SMPKeyManager.isKeyStoreValid ();
    aStatusData.add ("smp.certificate.configuration-valid", bCertConfigOk);
    if (bCertConfigOk)
    {
      final SMPKeyManager aKeyMgr = SMPKeyManager.getInstance ();
      final PrivateKeyEntry aKeyEntry = aKeyMgr.getPrivateKeyEntry ();
      if (aKeyEntry != null)
      {
        final Certificate [] aChain = aKeyEntry.getCertificateChain ();
        if (aChain.length > 0 && aChain[0] instanceof X509Certificate)
        {
          final X509Certificate aX509Cert = (X509Certificate) aChain[0];
          aStatusData.add ("smp.certificate.issuer", aX509Cert.getIssuerX500Principal ().getName ());
          aStatusData.add ("smp.certificate.subject", aX509Cert.getSubjectX500Principal ().getName ());

          final OffsetDateTime aNotAfter = PDTFactory.createOffsetDateTime (aX509Cert.getNotAfter ());
          final boolean bIsExpired = aNow.isAfter (aNotAfter);
          aStatusData.add ("smp.certificate.expired", bIsExpired);
          if (SMPServerConfiguration.isStatusShowCertificateDates ())
          {
            // Since v5.7.0
            final OffsetDateTime aNotBefore = PDTFactory.createOffsetDateTime (aX509Cert.getNotBefore ());
            aStatusData.add ("smp.certificate.notbefore", PDTWebDateHelper.getAsStringXSD (aNotBefore));
            aStatusData.add ("smp.certificate.notafter", PDTWebDateHelper.getAsStringXSD (aNotAfter));
          }
        }
      }
    }
    // Proxy configuration (since 5.2.0)
    aStatusData.add ("proxy.http.configured", SMPHttpConfiguration.getAsHttpProxySettings () != null);
    aStatusData.add ("proxy.https.configured", SMPHttpConfiguration.getAsHttpsProxySettings () != null);
    aStatusData.add ("proxy.username.configured", StringHelper.isNotEmpty (SMPHttpConfiguration.getProxyUsername ()));

    // CSP configuration (since 5.2.6)
    aStatusData.add ("csp.enabled", SMPWebAppConfiguration.isCSPEnabled ());
    aStatusData.add ("csp.reporting.only", SMPWebAppConfiguration.isCSPReportingOnly ());
    aStatusData.add ("csp.reporting.enabled", SMPWebAppConfiguration.isCSPReportingEnabled ());

    // Add SPI data as well
    for (final ISMPStatusProviderExtensionSPI aImpl : LIST)
    {
      final ICommonsOrderedMap <String, ?> aMap = aImpl.getAdditionalStatusData (bDisableLongRunningOperations);
      aStatusData.addAll (aMap);
    }
    final long nMillis = aSW.stopAndGetMillis ();
    if (nMillis > 100)
    {
      LOGGER.info ("Finished building status data after " +
                   nMillis +
                   " milliseconds which is considered to be too long");
    }
    else
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Finished building status data");
    }
    return aStatusData;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static IJsonObject getStatusDisabledData ()
  {
    final IJsonObject aStatusData = new JsonObject ();
    aStatusData.add ("status.enabled", false);
    return aStatusData;
  }
}
