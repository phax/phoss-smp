package com.helger.peppol.smpserver.servlet;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.system.SystemProperties;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.peppol.smpserver.CSMPServer;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.AppConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.helger.peppol.smpserver.settings.ISMPSettings;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

public class SMPStatusXServletHandler implements IXServletSimpleHandler
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPStatusXServletHandler.class);
  private static final Charset CHARSET = StandardCharsets.UTF_8;

  @Nullable
  private static String _toString (@Nullable final Object o)
  {
    return o == null ? null : o.toString ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public static IJsonObject getDefaultStatusData ()
  {
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();

    final IJsonObject aStatusData = new JsonObject ();
    aStatusData.add ("status.datetime", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
    aStatusData.add ("version.smp", CSMPServer.getVersionNumber ());
    aStatusData.add ("version.java", SystemProperties.getJavaVersion ());
    aStatusData.add ("global.debug", GlobalDebug.isDebugMode ());
    aStatusData.add ("global.production", GlobalDebug.isProductionMode ());
    aStatusData.add ("smp.backend", SMPServerConfiguration.getBackend ());
    aStatusData.add ("smp.mode", AppConfiguration.isTestVersion () ? "test" : "production");
    aStatusData.add ("smp.resttype", SMPServerConfiguration.getRESTType ().getID ());
    aStatusData.add ("smp.identifiertype", SMPServerConfiguration.getIdentifierType ().getID ());
    aStatusData.add ("smp.id", SMPServerConfiguration.getSMLSMPID ());
    aStatusData.add ("smp.writable-rest-api.enabled", !aSettings.isRESTWritableAPIDisabled ());

    // SML information
    aStatusData.add ("smp.sml.enabled", aSettings.isSMLActive ());
    aStatusData.add ("smp.sml.needed", aSettings.isSMLNeeded ());
    aStatusData.add ("smp.sml.url", aSettings.getSMLURL ());
    aStatusData.addIfNotNull ("smp.sml.connection-timeout-ms",
                              _toString (SMPServerConfiguration.getSMLConnectionTimeoutMS ()));
    aStatusData.addIfNotNull ("smp.sml.request-timeout-ms",
                              _toString (SMPServerConfiguration.getSMLRequestTimeoutMS ()));

    // Directory information
    aStatusData.add ("smp.pd.enabled", aSettings.isPEPPOLDirectoryIntegrationEnabled ());
    aStatusData.add ("smp.pd.auto-update", aSettings.isPEPPOLDirectoryIntegrationAutoUpdate ());
    aStatusData.add ("smp.pd.hostname", aSettings.getPEPPOLDirectoryHostName ());

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
    return aStatusData;
  }

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Status information requested");

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
