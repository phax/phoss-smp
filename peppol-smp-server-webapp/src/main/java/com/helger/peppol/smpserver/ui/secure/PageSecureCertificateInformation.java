/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.string.StringHelper;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.client.PDClientConfiguration;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.AppConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.helger.peppol.smpserver.security.SMPTrustManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPage;
import com.helger.peppol.smpserver.ui.AppCommonUI;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.nav.BootstrapTabBox;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;

/**
 * This page displays information about the certificate configured in the SMP
 * Server configuration file.
 *
 * @author Philip Helger
 */
public final class PageSecureCertificateInformation extends AbstractSMPWebPage
{
  private enum EPredefinedCert
  {
    PEPPOL_PILOT_V2 ("PEPPOL pilot v2",
                     "CN=PEPPOL SERVICE METADATA PUBLISHER TEST CA,OU=FOR TEST PURPOSES ONLY,O=NATIONAL IT AND TELECOM AGENCY,C=DK",
                     3),
    PEPPOL_PRODUCTION_V2 ("PEPPOL production v2",
                          "CN=PEPPOL SERVICE METADATA PUBLISHER CA, O=NATIONAL IT AND TELECOM AGENCY, C=DK",
                          3),
    PEPPOL_PILOT_V3 ("PEPPOL pilot v3",
                     "CN=PEPPOL SERVICE METADATA PUBLISHER TEST CA - G2,OU=FOR TEST ONLY,O=OpenPEPPOL AISBL,C=BE",
                     2),
    PEPPOL_PRODUCTION_V3 ("PEPPOL production v3",
                          "CN=PEPPOL SERVICE METADATA PUBLISHER CA - G2,O=OpenPEPPOL AISBL,C=BE",
                          2);

    private final String m_sName;
    private final String m_sIssuer;
    private final int m_nCerts;

    private EPredefinedCert (@Nonnull @Nonempty final String sName,
                             @Nonnull @Nonempty final String sIssuer,
                             @Nonnegative final int nCerts)
    {
      m_sName = sName;
      m_sIssuer = sIssuer;
      m_nCerts = nCerts;
    }

    @Nonnull
    @Nonempty
    public String getName ()
    {
      return m_sName;
    }

    @Nonnegative
    public int getCertificateTreeLength ()
    {
      return m_nCerts;
    }

    @Nullable
    public static EPredefinedCert getFromIssuerOrNull (@Nullable final String sIssuer)
    {
      if (StringHelper.hasText (sIssuer))
        for (final EPredefinedCert e : values ())
          if (e.m_sIssuer.equals (sIssuer))
            return e;
      return null;
    }
  }

  private static final String ACTION_RELOAD_KEYSTORE = "reloadkeystore";
  private static final String ACTION_RELOAD_TRUSTSTORE = "reloadtruststore";

  public PageSecureCertificateInformation (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Certificate information");
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ZonedDateTime aNowZDT = PDTFactory.getCurrentZonedDateTime ();
    final LocalDateTime aNowLDT = aNowZDT.toLocalDateTime ();

    if (aWPEC.hasAction (ACTION_RELOAD_KEYSTORE))
    {
      SMPKeyManager.reloadFromConfiguration ();
      aWPEC.postRedirectGetInternal (new BootstrapInfoBox ().addChild ("The keystore was updated from the configuration at " +
                                                                       DateTimeFormatter.ISO_DATE_TIME.format (aNowZDT) +
                                                                       ". The changes are reflected below."));
    }
    else
      if (aWPEC.hasAction (ACTION_RELOAD_TRUSTSTORE))
      {
        SMPTrustManager.reloadFromConfiguration ();
        aWPEC.postRedirectGetInternal (new BootstrapInfoBox ().addChild ("The truststore was updated from the configuration at " +
                                                                         DateTimeFormatter.ISO_DATE_TIME.format (aNowZDT) +
                                                                         ". The changes are reflected below."));
      }

    {
      final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
      aToolbar.addChild (new BootstrapButton ().addChild ("Reload keystore")
                                               .setIcon (EDefaultIcon.REFRESH)
                                               .setOnClick (aWPEC.getSelfHref ()
                                                                 .add (CPageParam.PARAM_ACTION,
                                                                       ACTION_RELOAD_KEYSTORE)));
      aToolbar.addChild (new BootstrapButton ().addChild ("Reload truststore")
                                               .setIcon (EDefaultIcon.REFRESH)
                                               .setOnClick (aWPEC.getSelfHref ()
                                                                 .add (CPageParam.PARAM_ACTION,
                                                                       ACTION_RELOAD_TRUSTSTORE)));
      aNodeList.addChild (aToolbar);
    }

    final BootstrapTabBox aTabBox = aNodeList.addAndReturnChild (new BootstrapTabBox ());

    // key store
    {
      final HCNodeList aTab = new HCNodeList ();
      if (!SMPKeyManager.isCertificateValid ())
      {
        aTab.addChild (new BootstrapErrorBox ().addChild (SMPKeyManager.getInitializationError ()));
      }
      else
      {
        // Successfully loaded private key
        final SMPKeyManager aKeyMgr = SMPKeyManager.getInstance ();
        final PrivateKeyEntry aKeyEntry = aKeyMgr.getPrivateKeyEntry ();
        if (aKeyEntry != null)
        {
          final Certificate [] aChain = aKeyEntry.getCertificateChain ();

          // Key store path and password are fine
          aTab.addChild (new BootstrapSuccessBox ().addChild (new HCDiv ().addChild ("Keystore is located at '" +
                                                                                     SMPServerConfiguration.getKeyStorePath () +
                                                                                     "' and was successfully loaded."))
                                                   .addChild (new HCDiv ().addChild ("The private key with the alias '" +
                                                                                     SMPServerConfiguration.getKeyStoreKeyAlias () +
                                                                                     "' was successfully loaded.")));

          if (aChain.length > 0 && aChain[0] instanceof X509Certificate)
          {
            final X509Certificate aHead = (X509Certificate) aChain[0];
            final String sIssuer = aHead.getIssuerX500Principal ().getName ();
            final EPredefinedCert eCert = EPredefinedCert.getFromIssuerOrNull (sIssuer);
            if (eCert != null)
            {
              aTab.addChild (new BootstrapInfoBox ().addChild ("You are currently using a " +
                                                               eCert.getName () +
                                                               " certificate!"));
              if (aChain.length != eCert.getCertificateTreeLength ())
                aTab.addChild (new BootstrapErrorBox ().addChild ("The private key should be a chain of " +
                                                                  eCert.getCertificateTreeLength () +
                                                                  " certificates but it has " +
                                                                  aChain.length +
                                                                  " certificates. Please ensure that the respective root certificates are contained!"));
            }
            // else: we don't care
          }

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
              aUL.addItem ("The certificate is not an X.509 certificate! It is internally a " +
                           ClassHelper.getClassName (aCert));
          }
          aTab.addChild (aUL);
        }
      }
      aTabBox.addTab ("keystore", "Keystore", aTab);
    }

    // Trust store
    {
      final HCNodeList aTab = new HCNodeList ();
      if (!SMPTrustManager.isCertificateValid ())
      {
        aTab.addChild (new BootstrapWarnBox ().addChild (SMPTrustManager.getInitializationError ()));
      }
      else
      {
        // Successfully loaded private key
        final SMPTrustManager aTrustMgr = SMPTrustManager.getInstance ();
        final KeyStore aTrustStore = aTrustMgr.getTrustStore ();

        // Key store path and password are fine
        aTab.addChild (new BootstrapSuccessBox ().addChild (new HCDiv ().addChild ("Truststore is located at '" +
                                                                                   SMPServerConfiguration.getTrustStorePath () +
                                                                                   "' and was successfully loaded.")));

        final HCOL aUL = new HCOL ();
        try
        {
          for (final String sAlias : CollectionHelper.newList (aTrustStore.aliases ()))
          {
            final Certificate aCert = aTrustStore.getCertificate (sAlias);
            if (aCert instanceof X509Certificate)
            {
              final X509Certificate aX509Cert = (X509Certificate) aCert;
              final BootstrapTable aCertDetails = AppCommonUI.createCertificateDetailsTable (aX509Cert,
                                                                                             aNowLDT,
                                                                                             aDisplayLocale);
              aUL.addItem (aCertDetails.getAsResponsiveTable ());
            }
            else
              aUL.addItem ("The certificate is not an X.509 certificate! It is internally a " +
                           ClassHelper.getClassName (aCert));
          }
        }
        catch (final GeneralSecurityException ex)
        {
          aUL.addItem (new BootstrapErrorBox ().addChild ("Error iterating trust store. Technical details: " +
                                                          ex.getMessage ()));
        }
        aTab.addChild (aUL);
      }
      aTabBox.addTab ("truststore", "Truststore", aTab);
    }

    // PEPPOL Directory client certificate
    if (SMPMetaManager.getSettings ().isPEPPOLDirectoryIntegrationEnabled ())
    {
      final HCNodeList aTab = new HCNodeList ();

      final EKeyStoreType eKeyStoreType = PDClientConfiguration.getKeyStoreType ();
      final String sKeyStorePath = PDClientConfiguration.getKeyStorePath ();

      final LoadedKeyStore aKeyStoreLR = KeyStoreHelper.loadKeyStore (eKeyStoreType,
                                                                      sKeyStorePath,
                                                                      PDClientConfiguration.getKeyStorePassword ());
      if (aKeyStoreLR.isFailure ())
      {
        aTab.addChild (new BootstrapErrorBox ().addChild (PeppolKeyStoreHelper.getLoadError (aKeyStoreLR)));
      }
      else
      {
        final KeyStore aKeyStore = aKeyStoreLR.getKeyStore ();
        final String sKeyStoreAlias = PDClientConfiguration.getKeyStoreKeyAlias ();
        final LoadedKey <KeyStore.PrivateKeyEntry> aKeyLoading = KeyStoreHelper.loadPrivateKey (aKeyStore,
                                                                                                sKeyStorePath,
                                                                                                sKeyStoreAlias,
                                                                                                PDClientConfiguration.getKeyStoreKeyPassword ());
        if (aKeyLoading.isFailure ())
        {
          aTab.addChild (new BootstrapSuccessBox ().addChild (new HCDiv ().addChild ("Keystore is located at '" +
                                                                                     sKeyStorePath +
                                                                                     "' and was successfully loaded.")));
          aTab.addChild (new BootstrapErrorBox ().addChild (PeppolKeyStoreHelper.getLoadError (aKeyLoading)));
        }
        else
        {
          // Successfully loaded private key
          final PrivateKeyEntry aKeyEntry = aKeyLoading.getKeyEntry ();
          final Certificate [] aChain = aKeyEntry.getCertificateChain ();

          // Key store path and password are fine
          aTab.addChild (new BootstrapSuccessBox ().addChild (new HCDiv ().addChild ("Keystore is located at '" +
                                                                                     sKeyStorePath +
                                                                                     "' and was successfully loaded."))
                                                   .addChild (new HCDiv ().addChild ("The private key with the alias '" +
                                                                                     sKeyStoreAlias +
                                                                                     "' was successfully loaded.")));

          if (aChain.length > 0 && aChain[0] instanceof X509Certificate)
          {
            final X509Certificate aHead = (X509Certificate) aChain[0];
            final String sIssuer = aHead.getIssuerX500Principal ().getName ();
            final EPredefinedCert eCert = EPredefinedCert.getFromIssuerOrNull (sIssuer);
            if (eCert != null)
            {
              aTab.addChild (new BootstrapInfoBox ().addChild ("You are currently using a " +
                                                               eCert.getName () +
                                                               " certificate!"));
              if (aChain.length != eCert.getCertificateTreeLength ())
                aTab.addChild (new BootstrapErrorBox ().addChild ("The private key should be a chain of " +
                                                                  eCert.getCertificateTreeLength () +
                                                                  " certificates but it has " +
                                                                  aChain.length +
                                                                  " certificates. Please ensure that the respective root certificates are contained!"));
            }
            // else: we don't care
          }

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
              aUL.addItem ("The certificate is not an X.509 certificate! It is internally a " +
                           ClassHelper.getClassName (aCert));
          }
          aTab.addChild (aUL);
        }
      }
      aTabBox.addTab ("pdkeystore", AppConfiguration.getDirectoryName () + " Keystore", aTab);
    }
  }
}
