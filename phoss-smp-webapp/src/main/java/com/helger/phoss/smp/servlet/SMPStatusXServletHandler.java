/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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
package com.helger.phoss.smp.servlet;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

/**
 * Create SMP status as JSON object. See
 * https://github.com/phax/phoss-smp/wiki/Status-API for details.
 *
 * @author Philip Helger
 * @since 5.0.6
 */
public class SMPStatusXServletHandler implements IXServletSimpleHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPStatusXServletHandler.class);
  private static final Charset CHARSET = StandardCharsets.UTF_8;

  @Nonnull
  @ReturnsMutableCopy
  public static IJsonObject getDefaultStatusData ()
  {
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();
    final ISMLInfo aSMLInfo = aSettings.getSMLInfo ();

    final IJsonObject aStatusData = new JsonObject ();
    aStatusData.add ("status.datetime", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
    aStatusData.add ("version.smp", CSMPServer.getVersionNumber ());
    // Since 5.0.7
    aStatusData.add ("build.timestamp", CSMPServer.getBuildTimestamp ());
    aStatusData.add ("version.java", SystemProperties.getJavaVersion ());
    aStatusData.add ("global.debug", GlobalDebug.isDebugMode ());
    aStatusData.add ("global.production", GlobalDebug.isProductionMode ());
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
    aStatusData.addIfNotNull ("smp.sml.connection-timeout-ms", SMPServerConfiguration.getSMLConnectionTimeoutMS ());
    aStatusData.add ("smp.sml.request-timeout-ms", SMPServerConfiguration.getSMLRequestTimeoutMS ());

    // Directory information
    aStatusData.add ("smp.pd.enabled", aSettings.isDirectoryIntegrationEnabled ());
    // New in 5.1.0
    aStatusData.add ("smp.pd.needed", aSettings.isDirectoryIntegrationRequired ());
    aStatusData.add ("smp.pd.auto-update", aSettings.isDirectoryIntegrationAutoUpdate ());
    aStatusData.add ("smp.pd.hostname", aSettings.getDirectoryHostName ());

    // Certificate information
    final boolean bCertConfigOk = SMPKeyManager.isCertificateValid ();
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

          final LocalDateTime aNotAfter = PDTFactory.createLocalDateTime (aX509Cert.getNotAfter ());
          final boolean bIsExpired = aNow.isAfter (aNotAfter);
          aStatusData.add ("smp.certificate.expired", bIsExpired);
        }
      }
    }

    // Proxy configuration (since 5.2.0)
    aStatusData.add ("proxy.http.configured", SMPServerConfiguration.getAsHttpProxySettings () != null);
    aStatusData.add ("proxy.https.configured", SMPServerConfiguration.getAsHttpsProxySettings () != null);
    aStatusData.add ("proxy.username.configured", StringHelper.hasText (SMPServerConfiguration.getProxyUsername ()));

    return aStatusData;
  }

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Status information requested");

    // Build data to provide
    IJsonObject aStatusData;
    if (SMPServerConfiguration.isStatusEnabled ())
      aStatusData = getDefaultStatusData ();
    else
    {
      // Status is disabled in the configuration
      aStatusData = new JsonObject ();
      aStatusData.add ("status.enabled", false);
    }

    // Put JSON on response
    aUnifiedResponse.disableCaching ();
    aUnifiedResponse.setMimeType (new MimeType (CMimeType.APPLICATION_JSON).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                          CHARSET.name ()));
    aUnifiedResponse.setContentAndCharset (aStatusData.getAsJsonString (), CHARSET);
  }
}
