/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.annotation.Nonnull;

import org.joda.time.LocalDateTime;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.string.StringHelper;
import com.helger.datetime.PDTFactory;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPage;
import com.helger.peppol.smpserver.ui.AppCommonUI;
import com.helger.peppol.utils.KeyStoreHelper;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.uicore.page.WebPageExecutionContext;

/**
 * This page displays information about the certificate configured in the SMP
 * Server configuration file.
 *
 * @author Philip Helger
 */
public final class PageSecureCertificateInformation extends AbstractSMPWebPage
{
  public PageSecureCertificateInformation (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Certificate information");
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    // Get the parameters for the key store
    KeyStore aKeyStore = null;
    final String sKeyStorePath = SMPServerConfiguration.getKeystorePath ();
    final char [] aKeyStorePassword = SMPServerConfiguration.getKeystoreKeyPassword ();
    if (StringHelper.hasNoText (sKeyStorePath) || aKeyStorePassword == null)
    {
      aNodeList.addChild (new BootstrapErrorBox ().addChild ("No keystore path and/or password are defined in the properties file."));
    }
    else
    {
      // Try to load key store
      try
      {
        aKeyStore = KeyStoreHelper.loadKeyStore (sKeyStorePath, aKeyStorePassword);
      }
      catch (final IOException | IllegalArgumentException ex)
      {
        aNodeList.addChild (new BootstrapErrorBox ().addChild ("Failed to load keystore from path '" +
                                                               sKeyStorePath +
                                                               "'. Seems like the keystore file does not exist."));
      }
      catch (final GeneralSecurityException ex)
      {
        aNodeList.addChild (new BootstrapErrorBox ().addChild ("Failed to load keystore from path '" +
                                                               sKeyStorePath +
                                                               "'. Seems like the password is invalid or the keystore has an invalid format."));
      }
    }

    if (aKeyStore != null)
    {
      // Key store path and password are fine
      aNodeList.addChild (new BootstrapSuccessBox ().addChild ("Keystore is located at '" +
                                                               sKeyStorePath +
                                                               "' and was successfully loaded."));

      KeyStore.PrivateKeyEntry aKeyEntry = null;
      final String sKeyStoreKeyAlias = SMPServerConfiguration.getKeystoreKeyAlias ();
      final char [] aKeyStoreKeyPassword = SMPServerConfiguration.getKeystoreKeyPassword ();
      if (StringHelper.hasNoText (sKeyStoreKeyAlias) || aKeyStoreKeyPassword == null)
      {
        aNodeList.addChild (new BootstrapErrorBox ().addChild ("No keystore key alias and/or password are defined in the properties file."));
      }
      else
      {
        // Try to load the key.
        try
        {
          final KeyStore.Entry aEntry = aKeyStore.getEntry (sKeyStoreKeyAlias,
                                                            new KeyStore.PasswordProtection (aKeyStoreKeyPassword));
          if (!(aEntry instanceof KeyStore.PrivateKeyEntry))
          {
            // Not a private key
            aNodeList.addChild (new BootstrapErrorBox ().addChild ("The keystore key alias '" +
                                                                   sKeyStoreKeyAlias +
                                                                   "' was found in keystore '" +
                                                                   sKeyStorePath +
                                                                   "' but it is not a private key! The internal type is " +
                                                                   ClassHelper.getClassName (aEntry)));
          }
          else
          {
            aKeyEntry = (KeyStore.PrivateKeyEntry) aEntry;
          }
        }
        catch (final UnrecoverableKeyException ex)
        {
          aNodeList.addChild (new BootstrapErrorBox ().addChild ("Failed to load key with alias '" +
                                                                 sKeyStoreKeyAlias +
                                                                 "' from keystore at '" +
                                                                 sKeyStorePath +
                                                                 "'. Seems like the password for the key is invalid."));
        }
        catch (final GeneralSecurityException ex)
        {
          aNodeList.addChild (new BootstrapErrorBox ().addChild ("Failed to load key with alias '" +
                                                                 sKeyStoreKeyAlias +
                                                                 "' from keystore at '" +
                                                                 sKeyStorePath +
                                                                 "'. Technical details: " +
                                                                 ex.getMessage ()));
        }
      }

      if (aKeyEntry != null)
      {
        // Key store alias is fine
        final Certificate [] aChain = aKeyEntry.getCertificateChain ();
        aNodeList.addChild (new BootstrapSuccessBox ().addChild ("The private key with the alias '" +
                                                                 sKeyStoreKeyAlias +
                                                                 "' was successfully loaded. It contains a total of " +
                                                                 aChain.length +
                                                                 " certificates."));

        final LocalDateTime aNowLDT = PDTFactory.getCurrentLocalDateTime ();
        final HCOL aUL = new HCOL ();
        for (final Certificate aCert : aChain)
        {
          if (aCert instanceof X509Certificate)
          {
            final X509Certificate aX509Cert = (X509Certificate) aCert;
            final BootstrapTable aCertDetails = AppCommonUI.createCertificateDetailsTable (aX509Cert,
                                                                                           aNowLDT,
                                                                                           aDisplayLocale);
            aUL.addItem (aCertDetails.getAsResponsiveTable ());
          }
          else
            aUL.addItem ("The certificate is not an X.509 certificate!");
        }
        aNodeList.addChild (aUL);
      }
    }
  }
}
