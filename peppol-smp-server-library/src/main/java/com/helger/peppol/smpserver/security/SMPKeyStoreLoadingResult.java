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
    final String sKeyStorePath = SMPServerConfiguration.getKeyStorePath ();
    if (StringHelper.hasNoText (sKeyStorePath))
      return new SMPKeyStoreLoadingResult (null, "No keystore path is defined in the configuration file.");

    final String sKeyStorePassword = SMPServerConfiguration.getKeyStorePassword ();
    if (sKeyStorePassword == null)
      return new SMPKeyStoreLoadingResult (null, "No keystore password is defined in the configuration file.");

    KeyStore aKeyStore = null;
    // Try to load key store
    try
    {
      aKeyStore = KeyStoreHelper.loadKeyStore (sKeyStorePath, sKeyStorePassword);
    }
    catch (final IOException | IllegalArgumentException ex)
    {
      return new SMPKeyStoreLoadingResult (null,
                                           "Failed to load keystore from path '" +
                                                 sKeyStorePath +
                                                 "'. Seems like the keystore file does not exist. Technical details: " +
                                                 ex.getMessage ());
    }
    catch (final GeneralSecurityException ex)
    {
      return new SMPKeyStoreLoadingResult (null,
                                           "Failed to load keystore from path '" +
                                                 sKeyStorePath +
                                                 "'. Seems like the password is invalid or the keystore has an invalid format. Technical details: " +
                                                 ex.getMessage ());
    }

    // Finally success
    return new SMPKeyStoreLoadingResult (aKeyStore, null);
  }

  /**
   * Load the trust store from the SMP server configuration.
   *
   * @return The trust store loading result. Never <code>null</code>.
   */
  @Nonnull
  public static SMPKeyStoreLoadingResult loadConfiguredTrustStore ()
  {
    // Get the parameters for the trust store
    final String sTrustStorePath = SMPServerConfiguration.getTrustStorePath ();
    if (StringHelper.hasNoText (sTrustStorePath))
      return new SMPKeyStoreLoadingResult (null, "No truststore path is defined in the configuration file.");

    final String sTrustStorePassword = SMPServerConfiguration.getTrustStorePassword ();
    if (sTrustStorePassword == null)
      return new SMPKeyStoreLoadingResult (null, "No truststore password is defined in the configuration file.");

    KeyStore aTrustStore = null;
    // Try to load trust store
    try
    {
      aTrustStore = KeyStoreHelper.loadKeyStore (sTrustStorePath, sTrustStorePassword);
    }
    catch (final IOException | IllegalArgumentException ex)
    {
      return new SMPKeyStoreLoadingResult (null,
                                           "Failed to load truststore from path '" +
                                                 sTrustStorePath +
                                                 "'. Seems like the truststore file does not exist. Technical details: " +
                                                 ex.getMessage ());
    }
    catch (final GeneralSecurityException ex)
    {
      return new SMPKeyStoreLoadingResult (null,
                                           "Failed to load truststore from path '" +
                                                 sTrustStorePath +
                                                 "'. Seems like the password is invalid or the truststore has an invalid format. Technical details: " +
                                                 ex.getMessage ());
    }

    // Finally success
    return new SMPKeyStoreLoadingResult (aTrustStore, null);
  }
}
