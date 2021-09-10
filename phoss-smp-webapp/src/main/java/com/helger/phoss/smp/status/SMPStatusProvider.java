package com.helger.phoss.smp.status;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.debug.GlobalDebug;
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
  private SMPStatusProvider ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public static IJsonObject getDefaultStatusData ()
  {
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();
    final ISMLInfo aSMLInfo = aSettings.getSMLInfo ();

    final IJsonObject aStatusData = new JsonObject ();
    // Since 5.0.7
    aStatusData.add ("build.timestamp", CSMPServer.getBuildTimestamp ());
    // Since 5.3.3
    aStatusData.addIfNotNull ("startup.datetime", PDTWebDateHelper.getAsStringXSD (SMPWebAppListener.getStartupDateTime ()));
    aStatusData.add ("status.datetime", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentOffsetDateTimeUTC ()));
    aStatusData.add ("version.smp", CSMPServer.getVersionNumber ());
    aStatusData.add ("version.java", SystemProperties.getJavaVersion ());
    aStatusData.add ("global.debug", GlobalDebug.isDebugMode ());
    aStatusData.add ("global.production", GlobalDebug.isProductionMode ());
    aStatusData.add ("smp.backend", SMPServerConfiguration.getBackend ());
    if ("sql".equalsIgnoreCase (SMPServerConfiguration.getBackend ()))
    {
      // Since 5.3.0-RC5
      aStatusData.add ("smp.sql.target-database", SMPServerConfiguration.getConfigFile ().getAsString ("target-database"));
    }
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

    // CSP configuration (since 5.2.6)
    aStatusData.add ("csp.enabled", SMPWebAppConfiguration.isCSPEnabled ());
    aStatusData.add ("csp.reporting.only", SMPWebAppConfiguration.isCSPReportingOnly ());
    aStatusData.add ("csp.reporting.enabled", SMPWebAppConfiguration.isCSPReportingEnabled ());

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
