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
package com.helger.peppol.smpserver.ui.secure;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.UnrecoverableKeyException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.state.ISuccessIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.utils.KeyStoreHelper;

/**
 * This class contains the result of loading the configured private key as
 * configured in the configuration file. See {@link #loadConfiguredKey()} on the
 * simplest way to load the private key entry configured in the SMP
 * configuration file.
 *
 * @author Philip Helger
 */
public class KeyLoadingResult implements ISuccessIndicator
{
  private final PrivateKeyEntry m_aKeyEntry;
  private final String m_sErrorMessage;

  private KeyLoadingResult (@Nullable final PrivateKeyEntry aKeyEntry, @Nullable final String sErrorMessage)
  {
    m_aKeyEntry = aKeyEntry;
    m_sErrorMessage = sErrorMessage;
  }

  public boolean isSuccess ()
  {
    return m_aKeyEntry != null;
  }

  public boolean isFailure ()
  {
    return m_aKeyEntry == null;
  }

  /**
   * @return The loaded key entry. Never <code>null</code> in case of success.
   *         Always <code>null</code> in case of failure.
   * @see #isSuccess()
   * @see #isFailure()
   */
  @Nullable
  public PrivateKeyEntry getKeyEntry ()
  {
    return m_aKeyEntry;
  }

  /**
   * @return The error message. Never <code>null</code> in case of failure.
   *         Always <code>null</code> in case of success.
   * @see #isSuccess()
   * @see #isFailure()
   */
  @Nullable
  public String getErrorMessage ()
  {
    return m_sErrorMessage;
  }

  @Nonnull
  public static KeyLoadingResult createSuccess (@Nonnull final PrivateKeyEntry aKeyEntry)
  {
    ValueEnforcer.notNull (aKeyEntry, "KeyEntry");
    return new KeyLoadingResult (aKeyEntry, null);
  }

  @Nonnull
  public static KeyLoadingResult createError (@Nonnull final String sErrorMessage)
  {
    ValueEnforcer.notNull (sErrorMessage, "ErrorMessage");
    return new KeyLoadingResult (null, sErrorMessage);
  }

  /**
   * Load the private key entry from the SMP server configuration that is used
   * to sign certain SMP responses.
   *
   * @return The key loading result. Never <code>null</code>.
   */
  @Nonnull
  public static KeyLoadingResult loadConfiguredKey ()
  {
    // Get the parameters for the key store
    final String sKeyStorePath = SMPServerConfiguration.getKeystorePath ();
    if (StringHelper.hasNoText (sKeyStorePath))
      return KeyLoadingResult.createError ("No keystore path is defined in the configuration file.");

    final char [] aKeyStorePassword = SMPServerConfiguration.getKeystoreKeyPassword ();
    if (aKeyStorePassword == null)
      return KeyLoadingResult.createError ("No keystore password is defined in the configuration file.");

    KeyStore aKeyStore = null;
    // Try to load key store
    try
    {
      aKeyStore = KeyStoreHelper.loadKeyStore (sKeyStorePath, aKeyStorePassword);
    }
    catch (final IOException | IllegalArgumentException ex)
    {
      return KeyLoadingResult.createError ("Failed to load keystore from path '" + sKeyStorePath + "'. Seems like the keystore file does not exist. Technical details: " + ex.getMessage ());
    }
    catch (final GeneralSecurityException ex)
    {
      return KeyLoadingResult.createError ("Failed to load keystore from path '" +
                                           sKeyStorePath +
                                           "'. Seems like the password is invalid or the keystore has an invalid format. Technical details: " +
                                           ex.getMessage ());
    }

    final String sKeyStoreKeyAlias = SMPServerConfiguration.getKeystoreKeyAlias ();
    if (StringHelper.hasNoText (sKeyStoreKeyAlias))
      return KeyLoadingResult.createError ("No keystore key alias is defined in the configuration file.");

    final char [] aKeyStoreKeyPassword = SMPServerConfiguration.getKeystoreKeyPassword ();
    if (aKeyStoreKeyPassword == null)
      return KeyLoadingResult.createError ("No keystore key password is defined in the configuration file.");

    // Try to load the key.
    KeyStore.PrivateKeyEntry aKeyEntry = null;
    try
    {
      final KeyStore.Entry aEntry = aKeyStore.getEntry (sKeyStoreKeyAlias, new KeyStore.PasswordProtection (aKeyStoreKeyPassword));
      if (aEntry == null)
      {
        // No such entry
        return KeyLoadingResult.createError ("The keystore key alias '" + sKeyStoreKeyAlias + "' was not found in keystore '" + sKeyStorePath + "'.");
      }
      if (!(aEntry instanceof KeyStore.PrivateKeyEntry))
      {
        // Not a private key
        return KeyLoadingResult.createError ("The keystore key alias '" +
                                             sKeyStoreKeyAlias +
                                             "' was found in keystore '" +
                                             sKeyStorePath +
                                             "' but it is not a private key! The internal type is " +
                                             ClassHelper.getClassName (aEntry));
      }

      aKeyEntry = (KeyStore.PrivateKeyEntry) aEntry;
    }
    catch (final UnrecoverableKeyException ex)
    {
      return KeyLoadingResult.createError ("Failed to load key with alias '" +
                                           sKeyStoreKeyAlias +
                                           "' from keystore at '" +
                                           sKeyStorePath +
                                           "'. Seems like the password for the key is invalid. Technical details: " +
                                           ex.getMessage ());
    }
    catch (final GeneralSecurityException ex)
    {
      return KeyLoadingResult.createError ("Failed to load key with alias '" + sKeyStoreKeyAlias + "' from keystore at '" + sKeyStorePath + "'. Technical details: " + ex.getMessage ());
    }

    // Finally success
    return KeyLoadingResult.createSuccess (aKeyEntry);
  }
}
