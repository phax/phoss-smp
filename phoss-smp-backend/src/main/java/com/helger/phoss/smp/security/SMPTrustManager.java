/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.security;

import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.exception.InitializationException;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.scope.singleton.AbstractGlobalSingleton;
import com.helger.security.keystore.EKeyStoreLoadError;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKeyStore;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This class holds the global trust store.
 *
 * @author Philip Helger
 */
public final class SMPTrustManager extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPTrustManager.class);

  private static final AtomicBoolean TRUST_STORE_VALID = new AtomicBoolean (false);
  private static EKeyStoreLoadError s_eInitError;
  private static String s_sInitError;

  private KeyStore m_aTrustStore;

  private static void _setTrustStoreValid (final boolean bValid)
  {
    TRUST_STORE_VALID.set (bValid);
  }

  private static void _loadError (@Nullable final EKeyStoreLoadError eInitError, @Nullable final String sInitError)
  {
    s_eInitError = eInitError;
    s_sInitError = sInitError;
  }

  private void _loadTrustStore ()
  {
    // Reset every time
    _setTrustStoreValid (false);
    _loadError (null, null);
    m_aTrustStore = null;

    // Load the trust store
    final LoadedKeyStore aTrustStoreLoading = KeyStoreHelper.loadKeyStore (SMPServerConfiguration.getTrustStoreType (),
                                                                           SMPServerConfiguration.getTrustStorePath (),
                                                                           SMPServerConfiguration.getTrustStorePassword ());
    if (aTrustStoreLoading.isFailure ())
    {
      _loadError (aTrustStoreLoading.getError (), LoadedKeyStore.getLoadError (aTrustStoreLoading));
      throw new InitializationException (s_sInitError);
    }
    m_aTrustStore = aTrustStoreLoading.getKeyStore ();

    LOGGER.info ("SMPTrustManager successfully initialized with truststore '" +
                 SMPServerConfiguration.getTrustStorePath () +
                 "'");
    _setTrustStoreValid (true);
  }

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public SMPTrustManager ()
  {
    _loadTrustStore ();
  }

  @Nonnull
  public static SMPTrustManager getInstance ()
  {
    return getGlobalSingleton (SMPTrustManager.class);
  }

  /**
   * @return The global trust store to be used. This trust store is never reloaded and must be
   *         present.
   */
  @Nullable
  public KeyStore getTrustStore ()
  {
    return m_aTrustStore;
  }

  /**
   * @return A shortcut method to determine if the trust store configuration is valid or not. This
   *         method can be used, even if {@link #getInstance()} throws an exception.
   */
  public static boolean isTrustStoreValid ()
  {
    return TRUST_STORE_VALID.get ();
  }

  /**
   * If the certificate is not valid according to {@link #isTrustStoreValid()} this method can be
   * used to determine the error detail code.
   *
   * @return <code>null</code> if initialization was successful.
   */
  @Nullable
  public static EKeyStoreLoadError getInitializationErrorCode ()
  {
    return s_eInitError;
  }

  /**
   * If the certificate is not valid according to {@link #isTrustStoreValid()} this method can be
   * used to determine the error detail message.
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
        aInstance._loadTrustStore ();
      else
      {
        // _loadTrustStore () is called in the constructor
        getInstance ();
      }
    }
    catch (final Exception ex)
    {
      // Ignore
    }
  }
}
