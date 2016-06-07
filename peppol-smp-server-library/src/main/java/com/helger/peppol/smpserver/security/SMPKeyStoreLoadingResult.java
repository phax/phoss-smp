/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.state.ISuccessIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.utils.KeyStoreHelper;

/**
 * This class contains the result of loading the configured keystore as
 * configured in the configuration file.
 *
 * @author Philip Helger
 */
final class SMPKeyStoreLoadingResult implements ISuccessIndicator
{
  private final KeyStore m_aKeyStore;
  private final String m_sErrorMessage;

  private SMPKeyStoreLoadingResult (@Nullable final KeyStore aKeyStore, @Nullable final String sErrorMessage)
  {
    m_aKeyStore = aKeyStore;
    m_sErrorMessage = sErrorMessage;
  }

  public boolean isSuccess ()
  {
    return m_aKeyStore != null;
  }

  /**
   * @return The loaded keystore. Never <code>null</code> in case of success.
   *         Always <code>null</code> in case of failure.
   */
  @Nullable
  public KeyStore getKeyStore ()
  {
    return m_aKeyStore;
  }

  /**
   * @return The error message. Never <code>null</code> in case of failure.
   *         Always <code>null</code> in case of success.
   */
  @Nullable
  public String getErrorMessage ()
  {
    return m_sErrorMessage;
  }

  @Nonnull
  public static SMPKeyStoreLoadingResult createSuccess (@Nonnull final KeyStore aKeyStore)
  {
    ValueEnforcer.notNull (aKeyStore, "KeyStore");
    return new SMPKeyStoreLoadingResult (aKeyStore, null);
  }

  @Nonnull
  public static SMPKeyStoreLoadingResult createError (@Nonnull final String sErrorMessage)
  {
    ValueEnforcer.notNull (sErrorMessage, "ErrorMessage");
    return new SMPKeyStoreLoadingResult (null, sErrorMessage);
  }

  /**
   * Load the keystore from the SMP server configuration that is used to sign
   * certain SMP responses.
   *
   * @return The keystore loading result. Never <code>null</code>.
   */
  @Nonnull
  public static SMPKeyStoreLoadingResult loadConfiguredKeyStore ()
  {
    // Get the parameters for the key store
    final String sKeyStorePath = SMPServerConfiguration.getKeystorePath ();
    if (StringHelper.hasNoText (sKeyStorePath))
      return SMPKeyStoreLoadingResult.createError ("No keystore path is defined in the configuration file.");

    final char [] aKeyStorePassword = SMPServerConfiguration.getKeystoreKeyPassword ();
    if (aKeyStorePassword == null)
      return SMPKeyStoreLoadingResult.createError ("No keystore password is defined in the configuration file.");

    KeyStore aKeyStore = null;
    // Try to load key store
    try
    {
      aKeyStore = KeyStoreHelper.loadKeyStore (sKeyStorePath, aKeyStorePassword);
    }
    catch (final IOException | IllegalArgumentException ex)
    {
      return SMPKeyStoreLoadingResult.createError ("Failed to load keystore from path '" +
                                                   sKeyStorePath +
                                                   "'. Seems like the keystore file does not exist. Technical details: " +
                                                   ex.getMessage ());
    }
    catch (final GeneralSecurityException ex)
    {
      return SMPKeyStoreLoadingResult.createError ("Failed to load keystore from path '" +
                                                   sKeyStorePath +
                                                   "'. Seems like the password is invalid or the keystore has an invalid format. Technical details: " +
                                                   ex.getMessage ());
    }

    // Finally success
    return SMPKeyStoreLoadingResult.createSuccess (aKeyStore);
  }
}
