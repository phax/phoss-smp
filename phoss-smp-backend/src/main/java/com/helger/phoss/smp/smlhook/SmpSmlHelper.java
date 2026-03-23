package com.helger.phoss.smp.smlhook;

import java.net.URL;
import java.util.Locale;

import org.apache.hc.core5.util.Timeout;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.exception.InitializationException;
import com.helger.base.url.URLHelper;
import com.helger.http.security.HostnameVerifierVerifyAll;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.BDMSLClient;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppol.smlclient.ManageServiceMetadataServiceCaller;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.wsclient.WSClientConfig;

/**
 * Helper class to create the SML clients based on the configuration of the SMP.
 * 
 * @author Philip Helger
 * @since 8.1.4
 */
@Immutable
public final class SmpSmlHelper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SmpSmlHelper.class);

  private SmpSmlHelper ()
  {}

  /**
   * This is the centralized place to customize the SOAP client with transport specific settings.
   * 
   * @param aSMLEndpointURL
   *        The target URL the call is targeted to. May not be <code>null</code>.
   * @param aCaller
   *        The SOAP client. May not be <code>null</code>.
   */
  private static void _customizeSMLCaller (@NonNull final URL aSMLEndpointURL, @NonNull final WSClientConfig aCaller)
  {
    final String sEndpointURL = aSMLEndpointURL.toExternalForm ();
    final String sLowerURL = sEndpointURL.toLowerCase (Locale.US);

    LOGGER.info ("Performing SML query to '" + sEndpointURL + "'");

    // SSL socket factory
    if (sLowerURL.startsWith ("https://"))
    {
      // https connection
      if (!SMPKeyManager.isKeyStoreValid ())
        throw new InitializationException ("Cannot init registration hook to SML, because private key/certificate setup has errors: " +
                                           SMPKeyManager.getInitializationError ());
      try
      {
        aCaller.setSSLSocketFactory (SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ());
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init SSLContext for SML access", ex);
      }
    }
    // else local, http only access - no socket factory

    // Hostname verifier
    if (sLowerURL.contains ("//localhost") || sLowerURL.contains ("//127.0.0.1"))
    {
      // Accept all hostnames
      aCaller.setHostnameVerifier (new HostnameVerifierVerifyAll (false));
    }

    final Timeout aConnectionTimeout = SMPServerConfiguration.getSMLConnectionTimeout ();
    if (aConnectionTimeout != null)
      aCaller.setConnectionTimeoutMS (aConnectionTimeout.toMillisecondsIntBound ());

    final Timeout aRequestTimeout = SMPServerConfiguration.getSMLRequestTimeout ();
    aCaller.setRequestTimeoutMS (aRequestTimeout.toMillisecondsIntBound ());

    // Example how to add headers
    if (false)
      aCaller.httpHeaders ().addHeader ("X-Target-System", "NC");
  }

  @NonNull
  public static ManageParticipantIdentifierServiceCaller createSMLCallerPI (@NonNull final ISMLInfo aSMLInfo)
  {
    final URL aSMLEndpointURL = aSMLInfo.getManageParticipantIdentifierEndpointAddress ();

    // Build WS client
    final ManageParticipantIdentifierServiceCaller ret = new ManageParticipantIdentifierServiceCaller (aSMLEndpointURL);
    // and customize
    _customizeSMLCaller (aSMLEndpointURL, ret);
    return ret;
  }

  @NonNull
  public static ManageServiceMetadataServiceCaller createSMLCallerSMP (@NonNull final ISMLInfo aSMLInfo)
  {
    final URL aSMLEndpointURL = aSMLInfo.getManageServiceMetaDataEndpointAddress ();

    // Build WS client
    final ManageServiceMetadataServiceCaller ret = new ManageServiceMetadataServiceCaller (aSMLEndpointURL);
    // and customize
    _customizeSMLCaller (aSMLEndpointURL, ret);
    return ret;
  }

  @NonNull
  public static BDMSLClient createSMLCallerBDMSL (@NonNull final ISMLInfo aSMLInfo)
  {
    final URL aSMLEndpointURL = URLHelper.getAsURL (aSMLInfo.getManagementServiceURL () + "/bdmslservice");

    // Build WS client
    final BDMSLClient ret = new BDMSLClient (aSMLEndpointURL);
    // and customize
    _customizeSMLCaller (aSMLEndpointURL, ret);
    return ret;
  }
}
