/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.security;

import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
import com.helger.scope.singleton.AbstractGlobalSingleton;
import com.helger.security.keystore.EKeyStoreLoadError;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKeyStore;

/**
 * This class holds the global trust store.
 *
 * @author Philip Helger
 */
public final class SMPTrustManager extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPTrustManager.class);

  private static final AtomicBoolean s_aCertificateValid = new AtomicBoolean (false);
  private static EKeyStoreLoadError s_eInitError;
  private static String s_sInitError;

  private KeyStore m_aTrustStore;

  private void _loadCertificates () throws InitializationException
  {
    // Reset every time
    s_aCertificateValid.set (false);
    s_sInitError = null;
    m_aTrustStore = null;

    // Load the trust store
    final LoadedKeyStore aTrustStoreLoading = KeyStoreHelper.loadKeyStore (SMPServerConfiguration.getTrustStoreType (),
                                                                           SMPServerConfiguration.getTrustStorePath (),
                                                                           SMPServerConfiguration.getTrustStorePassword ());
    if (aTrustStoreLoading.isFailure ())
    {
      s_eInitError = aTrustStoreLoading.getError ();
      s_sInitError = PeppolKeyStoreHelper.getLoadError (aTrustStoreLoading);
      throw new InitializationException (s_sInitError);
    }
    m_aTrustStore = aTrustStoreLoading.getKeyStore ();

    LOGGER.info ("SMPTrustManager successfully initialized with truststore '" +
                    SMPServerConfiguration.getTrustStorePath () +
                    "'");
    s_aCertificateValid.set (true);
  }

  @Deprecated
  @UsedViaReflection
  public SMPTrustManager () throws InitializationException
  {
    _loadCertificates ();
  }

  @Nonnull
  public static SMPTrustManager getInstance ()
  {
    return getGlobalSingleton (SMPTrustManager.class);
  }

  /**
   * @return The global trust store to be used. This trust store is never
   *         reloaded and must be present.
   */
  @Nullable
  public KeyStore getTrustStore ()
  {
    return m_aTrustStore;
  }

  /**
   * @return A shortcut method to determine if the certification configuration
   *         is valid or not. This method can be used, even if
   *         {@link #getInstance()} throws an exception.
   */
  public static boolean isCertificateValid ()
  {
    return s_aCertificateValid.get ();
  }

  /**
   * If the certificate is not valid according to {@link #isCertificateValid()}
   * this method can be used to determine the error detail code.
   *
   * @return <code>null</code> if initialization was successful.
   */
  @Nullable
  public static EKeyStoreLoadError getInitializationErrorCode ()
  {
    return s_eInitError;
  }

  /**
   * If the certificate is not valid according to {@link #isCertificateValid()}
   * this method can be used to determine the error detail message.
   *
   * @return <code>null</code> if initialization was successful.
   */
  @Nullable
  public static String getInitializationError ()
  {
    return s_sInitError;
  }

  public static void reloadFromConfiguration ()
  {
    try
    {
      final SMPTrustManager aInstance = getGlobalSingletonIfInstantiated (SMPTrustManager.class);
      if (aInstance != null)
        aInstance._loadCertificates ();
      else
        getInstance ();
    }
    catch (final Exception ex)
    {}
  }
}
