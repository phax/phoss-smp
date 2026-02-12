/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.config;

import org.apache.hc.core5.util.Timeout;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.config.IConfig;
import com.helger.mime.EMimeContentType;
import com.helger.peppolid.factory.ESMPIdentifierType;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.ESMPRESTType;
import com.helger.security.keystore.EKeyStoreType;

/**
 * This class provides easy access to certain configuration properties using
 * {@link SMPConfigProvider}.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class SMPServerConfiguration
{
  public static final String KEY_SMP_BACKEND = "smp.backend";

  public static final String KEY_SMP_KEYSTORE_TYPE = "smp.keystore.type";
  public static final String KEY_SMP_KEYSTORE_PATH = "smp.keystore.path";
  public static final String KEY_SMP_KEYSTORE_PASSWORD = "smp.keystore.password";
  public static final String KEY_SMP_KEYSTORE_KEY_ALIAS = "smp.keystore.key.alias";
  public static final String KEY_SMP_KEYSTORE_KEY_PASSWORD = "smp.keystore.key.password";

  public static final String KEY_SMP_TRUSTSTORE_TYPE = "smp.truststore.type";
  public static final String KEY_SMP_TRUSTSTORE_PATH = "smp.truststore.path";
  public static final String KEY_SMP_TRUSTSTORE_PASSWORD = "smp.truststore.password";

  public static final String KEY_SMP_FORCE_ROOT = "smp.forceroot";
  public static final String KEY_SMP_PUBLIC_URL = "smp.publicurl";
  public static final String KEY_SMP_PUBLIC_URL_MODE = "smp.publicurl.mode";
  public static final String KEY_SMP_IDENTIFIER_TYPE = "smp.identifiertype";

  public static final String KEY_SMP_REST_TYPE = "smp.rest.type";
  public static final String KEY_SMP_REST_LOG_EXCEPTIONS = "smp.rest.log.exceptions";
  public static final String KEY_SMP_REST_PAYLOAD_ON_ERROR = "smp.rest.payload.on.error";
  public static final String KEY_SMP_REST_REMOTE_QUERY_API_DISABLED = "smp.rest.remote.queryapi.disabled";

  public static final String KEY_SMP_STATUS_ENABLED = "smp.status.enabled";
  public static final String KEY_SMP_STATUS_SHOW_CERTIFICATE_DATES = "smp.status.show.certificate.dates";

  public static final String KEY_SMP_BDXR2_CERTIFICATE_MIME_CODE = "smp.bdxr2.certificate.mimecode";
  public static final String KEY_SMP_BDXR2_CERTIFICATE_TYPE_CODE = "smp.bdxr2.certificate.typecode";

  public static final String KEY_SMP_TIMEZONE = "smp.timezone";

  public static final String KEY_SML_SMPID = "sml.smpid";
  public static final String KEY_SML_SMP_IP = "sml.smp.ip";
  public static final String KEY_SML_SMP_HOSTNAME = "sml.smp.hostname";
  public static final String KEY_SML_CONNECTION_TIMEOUT_MS = "sml.connection.timeout.ms";
  public static final String KEY_SML_REQUEST_TIMEOUT_MS = "sml.request.timeout.ms";

  @Deprecated (forRemoval = true, since = "8.0.6")
  public static final String KEY_HREDELIVERY_SG_EXTENSION_ON_SI = "hredelivery.sg.extension.on.si";
  public static final String KEY_SMP_HREDELIVERY_EXTENSION = "smp.hredelivery.extension";
  public static final String KEY_SMP_HREDELIVERY_ACCESSPOINTOIB = "smp.hredelivery.accesspointoib";

  public static final boolean DEFAULT_SMP_FORCEROOT = false;
  public static final ESMPIdentifierType DEFAULT_SMP_IDENTIFIER_TYPE = ESMPIdentifierType.PEPPOL;
  public static final ESMPRESTType DEFAULT_SMP_REST_TYPE = ESMPRESTType.PEPPOL;
  public static final boolean DEFAULT_SMP_REST_LOG_EXCEPTIONS = false;
  public static final boolean DEFAULT_SMP_REST_PAYLOAD_ON_ERROR = true;
  public static final boolean DEFAULT_SMP_REST_REMOTE_QUERY_API_DISABLED = true;

  public static final boolean DEFAULT_SMP_STATUS_ENABLED = true;
  public static final boolean DEFAULT_SMP_STATUS_SHOW_CERTIFICATE_DATES = false;

  public static final String DEFAULT_SMP_BDXR2_CERTIFICATE_MIME_CODE = EMimeContentType.APPLICATION.buildMimeType ("base64")
                                                                                                   .getAsString ();
  public static final String DEFAULT_SMP_BDXR2_CERTIFICATE_TYPE_CODE = "bdxr-as4-signing-encryption";

  public static final Timeout DEFAULT_SML_REQUEST_TIMEOUT = Timeout.ofSeconds (30);

  @Deprecated (forRemoval = true, since = "8.0.6")
  public static final boolean DEFAULT_HREDELIVERY_SG_EXTENSION_ON_SI = false;
  public static final boolean DEFAULT_SMP_HREDELIVERY_EXTENSION = false;

  private SMPServerConfiguration ()
  {}

  /**
   * @return The configuration file. Never <code>null</code>.
   */
  @NonNull
  private static IConfig _getConfig ()
  {
    return SMPConfigProvider.getConfig ();
  }

  /**
   * @return The backend to be used. Depends on the different possible implementations. Should not
   *         be <code>null</code>. Property <code>smp.backend</code>.
   * @see com.helger.phoss.smp.backend.SMPBackendRegistry
   */
  @Nullable
  public static String getBackend ()
  {
    return _getConfig ().getAsString (KEY_SMP_BACKEND);
  }

  /**
   * @return The type to the keystore. This is usually JKS. Property <code>smp.keystore.type</code>.
   * @since 5.0.4
   */
  @NonNull
  public static EKeyStoreType getKeyStoreType ()
  {
    final String sType = _getConfig ().getAsString (KEY_SMP_KEYSTORE_TYPE);
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType, EKeyStoreType.JKS);
  }

  /**
   * @return The path to the keystore. May be a classpath or an absolute file path. Property
   *         <code>smp.keystore.path</code>.
   */
  @Nullable
  public static String getKeyStorePath ()
  {
    return _getConfig ().getAsString (KEY_SMP_KEYSTORE_PATH);
  }

  /**
   * @return The password required to open the keystore. Property
   *         <code>smp.keystore.password</code>.
   */
  public static char @Nullable [] getKeyStorePassword ()
  {
    return _getConfig ().getAsCharArray (KEY_SMP_KEYSTORE_PASSWORD);
  }

  /**
   * @return The alias of the SMP key in the keystore. Property <code>smp.keystore.key.alias</code>.
   */
  @Nullable
  public static String getKeyStoreKeyAlias ()
  {
    return _getConfig ().getAsString (KEY_SMP_KEYSTORE_KEY_ALIAS);
  }

  /**
   * @return The password used to access the private key. May be different than the password to the
   *         overall keystore. Property <code>smp.keystore.key.password</code>.
   */
  public static char @Nullable [] getKeyStoreKeyPassword ()
  {
    return _getConfig ().getAsCharArray (KEY_SMP_KEYSTORE_KEY_PASSWORD);
  }

  /**
   * @return The type to the truststore. This is usually JKS. Property
   *         <code>smp.truststore.type</code>.
   */
  @NonNull
  public static EKeyStoreType getTrustStoreType ()
  {
    final String sType = _getConfig ().getAsString (KEY_SMP_TRUSTSTORE_TYPE);
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType, EKeyStoreType.JKS);
  }

  /**
   * @return The path to the truststore. May be a classpath or an absolute file path. Property
   *         <code>smp.truststore.path</code>.
   */
  @Nullable
  public static String getTrustStorePath ()
  {
    return _getConfig ().getAsString (KEY_SMP_TRUSTSTORE_PATH);
  }

  /**
   * @return The password required to open the truststore. Property
   *         <code>smp.truststore.password</code>.
   */
  public static char @Nullable [] getTrustStorePassword ()
  {
    return _getConfig ().getAsCharArray (KEY_SMP_TRUSTSTORE_PASSWORD);
  }

  /**
   * @return <code>true</code> if all paths should be forced to the ROOT ("/") context,
   *         <code>false</code> if the context should remain as it is. Property
   *         <code>smp.forceroot</code>.
   */
  public static boolean isForceRoot ()
  {
    return _getConfig ().getAsBoolean (KEY_SMP_FORCE_ROOT, DEFAULT_SMP_FORCEROOT);
  }

  /**
   * @return The server URL that should be used to create absolute URLs inside the application. This
   *         may be helpful when running on a proxied Tomcat behind a web server. Property
   *         <code>smp.publicurl</code>.
   */
  @Nullable
  public static String getPublicServerURL ()
  {
    return _getConfig ().getAsString (KEY_SMP_PUBLIC_URL);
  }

  /**
   * @return The public server URL mode to use. This was introduced for issue #131. May be
   *         <code>null</code>.
   * @since 5.2.4
   */
  @Nullable
  public static String getPublicServerURLMode ()
  {
    return _getConfig ().getAsString (KEY_SMP_PUBLIC_URL_MODE);
  }

  /**
   * @return The identifier types to be used. Never <code>null</code>. Defaults to
   *         {@link ESMPIdentifierType#PEPPOL}. Property <code>smp.identifiertype</code>.
   */
  @NonNull
  public static ESMPIdentifierType getIdentifierType ()
  {
    final String sType = _getConfig ().getAsString (KEY_SMP_IDENTIFIER_TYPE);
    return ESMPIdentifierType.getFromIDOrDefault (sType, DEFAULT_SMP_IDENTIFIER_TYPE);
  }

  /**
   * @return The REST type to be used. Never <code>null</code>. Defaults to
   *         {@link ESMPRESTType#PEPPOL}. Property <code>smp.rest.type</code>.
   */
  @NonNull
  public static ESMPRESTType getRESTType ()
  {
    final String sType = _getConfig ().getAsString (KEY_SMP_REST_TYPE);
    return ESMPRESTType.getFromIDOrDefault (sType, DEFAULT_SMP_REST_TYPE);
  }

  /**
   * @return <code>true</code> if the exceptions in the REST API should be logged,
   *         <code>false</code> if not. By default it is disabled.
   * @since 5.1.0
   * @deprecated Use {@link #isRestLogExceptions()} instead
   */
  @Deprecated (forRemoval = true, since = "8.0.2")
  public static boolean isRESTLogExceptions ()
  {
    return isRestLogExceptions ();
  }

  /**
   * @return <code>true</code> if the exceptions in the REST API should be logged,
   *         <code>false</code> if not. By default it is disabled.
   * @since 8.0.2
   */
  public static boolean isRestLogExceptions ()
  {
    return _getConfig ().getAsBoolean (KEY_SMP_REST_LOG_EXCEPTIONS, DEFAULT_SMP_REST_LOG_EXCEPTIONS);
  }

  /**
   * @return <code>true</code> if in case of an exception in the REST API´, payload text should be
   *         provided as test, <code>false</code> if not. By default it is enabled. For security
   *         reasons it should be disabled.
   * @since 5.2.1
   * @deprecated Use {@link #isRestPayloadOnError()} instead
   */
  @Deprecated (forRemoval = true, since = "8.0.2")
  public static boolean isRESTPayloadOnError ()
  {
    return isRestPayloadOnError ();
  }

  /**
   * @return <code>true</code> if in case of an exception in the REST API´, payload text should be
   *         provided as test, <code>false</code> if not. By default it is enabled. For security
   *         reasons it should be disabled.
   * @since 8.0.2
   */
  public static boolean isRestPayloadOnError ()
  {
    return _getConfig ().getAsBoolean (KEY_SMP_REST_PAYLOAD_ON_ERROR, DEFAULT_SMP_REST_PAYLOAD_ON_ERROR);
  }

  /**
   * @return <code>true</code> if the remote query API (get endpoints and business cards from other
   *         SMP server) is disabled. By default it is disabled.
   * @since 5.3.0-RC2
   */
  public static boolean isRestRemoteQueryAPIDisabled ()
  {
    return _getConfig ().getAsBoolean (KEY_SMP_REST_REMOTE_QUERY_API_DISABLED,
                                       DEFAULT_SMP_REST_REMOTE_QUERY_API_DISABLED);
  }

  /**
   * @return <code>true</code> if the status servlet at <code>/smp-status/</code> is enabled,
   *         <code>false</code> if it is disabled. By default it is enabled.
   * @since 5.0.6
   */
  public static boolean isStatusEnabled ()
  {
    return _getConfig ().getAsBoolean (KEY_SMP_STATUS_ENABLED, DEFAULT_SMP_STATUS_ENABLED);
  }

  /**
   * @return <code>true</code> if the certificate not before and not after dates should be listed in
   *         the status or not. Defaults to <code>false</code>.
   * @since 5.7.0
   */
  public static boolean isStatusShowCertificateDates ()
  {
    return _getConfig ().getAsBoolean (KEY_SMP_STATUS_SHOW_CERTIFICATE_DATES,
                                       DEFAULT_SMP_STATUS_SHOW_CERTIFICATE_DATES);
  }

  /**
   * @return The MIME code to be used for BDXR2 certificates. Defaults to
   *         {@link #DEFAULT_SMP_BDXR2_CERTIFICATE_MIME_CODE}.
   * @since 5.7.0
   */
  @NonNull
  @Nonempty
  public static String getBDXR2CertificateMimeCode ()
  {
    return _getConfig ().getAsString (KEY_SMP_BDXR2_CERTIFICATE_MIME_CODE, DEFAULT_SMP_BDXR2_CERTIFICATE_MIME_CODE);
  }

  /**
   * @return The type code to be used for BDXR2 certificates. Defaults to
   *         {@link #DEFAULT_SMP_BDXR2_CERTIFICATE_TYPE_CODE}.
   * @since 5.7.0
   */
  @NonNull
  @Nonempty
  public static String getBDXR2CertificateTypeCode ()
  {
    return _getConfig ().getAsString (KEY_SMP_BDXR2_CERTIFICATE_TYPE_CODE, DEFAULT_SMP_BDXR2_CERTIFICATE_TYPE_CODE);
  }

  @NonNull
  @Nonempty
  public static String getTimeZoneOrDefault ()
  {
    return _getConfig ().getAsString (KEY_SMP_TIMEZONE, CSMPServer.DEFAULT_TIMEZONE);
  }

  /**
   * @return The SMP-ID to be used in the SML. Only relevant when SML connection is active. Property
   *         <code>sml.smpid</code>.
   */
  @Nullable
  public static String getSMLSMPID ()
  {
    return _getConfig ().getAsString (KEY_SML_SMPID);
  }

  /**
   * @return The default hostname to be used for the SML registration including an "http://" prefix
   *         as in <code>http://smp.example.org</code>. May be <code>null</code> in which case the
   *         name must be manually provided.
   * @since 5.0.3
   */
  @Nullable
  public static String getSMLSMPHostname ()
  {
    return _getConfig ().getAsString (KEY_SML_SMP_HOSTNAME);
  }

  /**
   * @return The connection timeout in milliseconds used for connecting to the SML server. May be
   *         <code>null</code> in which case the system default timeout should be used.
   * @since 6.0.0
   */
  @Nullable
  public static Timeout getSMLConnectionTimeout ()
  {
    final long ret = _getConfig ().getAsLong (KEY_SML_CONNECTION_TIMEOUT_MS, -1L);
    if (ret >= 0)
      return Timeout.ofMilliseconds (ret);
    return null;
  }

  /**
   * @return The request timeout used for connecting to the SML server. The default is defined in
   *         {@link #DEFAULT_SML_REQUEST_TIMEOUT} since 5.1.1.
   * @since 6.0.0
   */
  @NonNull
  public static Timeout getSMLRequestTimeout ()
  {
    final long ret = _getConfig ().getAsLong (KEY_SML_REQUEST_TIMEOUT_MS, -1L);
    if (ret >= 0)
      return Timeout.ofMilliseconds (ret);
    return DEFAULT_SML_REQUEST_TIMEOUT;
  }

  /**
   * @return <code>true</code> if the ServiceGroup extension should also be emitted in the
   *         ServiceInformation extension. The default is <code>false</code>.
   * @since 8.0.1
   */
  @Deprecated (forRemoval = true, since = "8.0.6")
  public static boolean isHRIncludeSGExtOnSI ()
  {
    return _getConfig ().getAsBoolean (KEY_HREDELIVERY_SG_EXTENSION_ON_SI, DEFAULT_HREDELIVERY_SG_EXTENSION_ON_SI);
  }

  /**
   * @return <code>true</code> if the extension mode for HR eDelivery is enabled, <code>false</code>
   *         if not.
   * @since 8.0.6
   */
  public static boolean isHREdeliveryExtensionMode ()
  {
    return _getConfig ().getAsBoolean (KEY_SMP_HREDELIVERY_EXTENSION, DEFAULT_SMP_HREDELIVERY_EXTENSION);
  }

  /**
   * @return The HR OIB number of the Access Point. This must be an 11-digit number.
   * @since 8.0.6
   */
  @Nullable
  public static String getHREdeliveryAccessPointOIB ()
  {
    return _getConfig ().getAsString (KEY_SMP_HREDELIVERY_ACCESSPOINTOIB);
  }
}
