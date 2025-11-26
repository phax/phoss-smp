/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.ui.secure;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.lang.clazz.ClassHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.pd.client.PDClientConfiguration;
import com.helger.peppol.ui.CertificateUI;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.security.SMPTrustManager;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.nav.BootstrapTabBox;
import com.helger.photon.bootstrap4.table.BootstrapTable;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;

import jakarta.annotation.Nullable;

/**
 * This page displays information about the certificate configured in the SMP Server configuration
 * file.
 *
 * @author Philip Helger
 */
public final class PageSecureCertificateInformation extends AbstractSMPWebPage
{
  private enum EPredefinedCA
  {
    // PEPPOL PKI G1 2010
    PEPPOL_PILOT_G1 ("Peppol Pilot CA G1 (2010)",
                     "CN=PEPPOL SERVICE METADATA PUBLISHER TEST CA,OU=FOR TEST PURPOSES ONLY,O=NATIONAL IT AND TELECOM AGENCY,C=DK",
                     3,
                     true),
    PEPPOL_PRODUCTION_G1 ("Peppol Production CA G1 (2010)",
                          "CN=PEPPOL SERVICE METADATA PUBLISHER CA, O=NATIONAL IT AND TELECOM AGENCY, C=DK",
                          3,
                          true),
    // PEPPOL PKI G2 2018
    PEPPOL_PILOT_G2 ("Peppol Pilot CA G2 (2018)",
                     "CN=PEPPOL SERVICE METADATA PUBLISHER TEST CA - G2,OU=FOR TEST ONLY,O=OpenPEPPOL AISBL,C=BE",
                     3,
                     false),
    PEPPOL_PRODUCTION_G2 ("Peppol Production CA G2 (2018)",
                          "CN=PEPPOL SERVICE METADATA PUBLISHER CA - G2,O=OpenPEPPOL AISBL,C=BE",
                          3,
                          false),
    // PEPPOL PKI G3 2025
    PEPPOL_PILOT_G3 ("Peppol Pilot CA G3 (2025)",
                     "CN=PEPPOL SERVICE METADATA PUBLISHER TEST CA - G3,OU=FOR TEST ONLY,O=OpenPEPPOL AISBL,C=BE",
                     3,
                     false),
    PEPPOL_PRODUCTION_G3 ("Peppol Production CA G3 (2025)",
                          "CN=PEPPOL SERVICE METADATA PUBLISHER CA - G3,O=OpenPEPPOL AISBL,C=BE",
                          3,
                          false),
    // TOOP Pilot PKI
    @Deprecated (forRemoval = false)
    TOOP_PILOT_SMP("TOOP Pilot CA", "CN=TOOP PILOTS TEST SMP CA,OU=CCTF,O=TOOP,ST=Belgium,C=EU", 3, true),

    // DE4A PKIs
    @Deprecated (forRemoval = false)
    DE4A_TEST("DE4A Test CA",
              "E=CEF-EDELIVERY-SUPPORT@ec.europa.eu,CN=DE4A_TEST_SMP_CA,OU=CEF,O=DE4A,ST=Brussels-Capital,C=BE",
              4,
              true),
    @Deprecated (forRemoval = false)
    DE4A_TELSEC1("DE4A Telesec CA [1]",
                 "CN=TeleSec Business CA 1,OU=T-Systems Trust Center,O=T-Systems International GmbH,C=DE",
                 3,
                 true),
    @Deprecated (forRemoval = false)
    DE4A_TELSEC2("DE4A Telesec CA [2]", "CN=TeleSec Business CA 21,O=Deutsche Telekom Security GmbH,C=DE", 3, true),
    @Deprecated (forRemoval = false)
    DE4A_COMMISSIGN_2("DE4A CommisSign", "CN=CommisSign - 2,O=European Commission", 3, true),

    // DBNA
    DBNA_PRODUCTION ("DBNA Production CA",
                     "CN=Digital Business Networks Alliance Intermediate CA,O=Digital Business Networks Alliance,C=US",
                     3,
                     false),
    DBNA_TEST ("DBNA Test CA",
               "CN=DBNAlliance Demo Intermediate Test,O=Digital Business Network Alliance,STREET=3 River Way Suite 920,PostalCode=77056,L=Houston,ST=Texas,C=US",
               3,
               false),
    DBNA_PILOT ("DBNA Pilot CA",
                "CN=DBNAlliance Demo Intermediate Pilot,O=Digital Business Network Alliance,STREET=3 River Way Suite 920,PostalCode=77056,L=Houston,ST=Texas,C=US",
                3,
                false),

    // HR eDelivery
    HR_EDELIVERY_DEMO ("HR eDelivery Demo CA", "CN=Fina Demo CA 2020,O=Financijska agencija,C=HR", 1, false),
    HR_EDELIVERY_PRODUCTION ("HR eDelivery Production CA", "CN=Fina RDC 2020,O=Financijska agencija,C=HR", 1, false),;

    private final String m_sDisplayName;
    private final String m_sIssuer;
    private final int m_nCerts;
    private final boolean m_bDeprecated;

    /**
     * @param sDisplayName
     *        Display name
     * @param sIssuer
     *        Required issuer
     * @param nCerts
     *        Required depth of PKI
     */
    EPredefinedCA (@NonNull @Nonempty final String sDisplayName,
                   @NonNull @Nonempty final String sIssuer,
                   @Nonnegative final int nCerts,
                   final boolean bDeprecated)
    {
      m_sDisplayName = sDisplayName;
      m_sIssuer = sIssuer;
      m_nCerts = nCerts;
      m_bDeprecated = bDeprecated;
    }

    @NonNull
    @Nonempty
    public String getDisplayName ()
    {
      return m_sDisplayName;
    }

    @Nonnegative
    public int getCertificateTreeLength ()
    {
      return m_nCerts;
    }

    public boolean isDeprecated ()
    {
      return m_bDeprecated;
    }

    @Nullable
    public static EPredefinedCA getFromIssuerOrNull (@Nullable final String sIssuer)
    {
      if (StringHelper.isNotEmpty (sIssuer))
        for (final EPredefinedCA e : values ())
          if (e.m_sIssuer.equals (sIssuer))
            return e;
      return null;
    }
  }

  private static final String ACTION_RELOAD_KEYSTORE = "reloadkeystore";
  private static final String ACTION_RELOAD_TRUSTSTORE = "reloadtruststore";
  private static final String ACTION_RELOAD_DIRECTORY_CONFIGURATION = "reloadpdconfig";

  public PageSecureCertificateInformation (@NonNull @Nonempty final String sID)
  {
    super (sID, "Certificate Information");
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ZonedDateTime aNowZDT = PDTFactory.getCurrentZonedDateTime ();
    final OffsetDateTime aNowDT = aNowZDT.toOffsetDateTime ();
    final String sDirectoryName = SMPWebAppConfiguration.getDirectoryName ();

    if (aWPEC.hasAction (ACTION_RELOAD_KEYSTORE))
    {
      SMPKeyManager.reloadFromConfiguration ();
      aWPEC.postRedirectGetInternal (info ("The keystore was updated from the configuration at " +
                                           DateTimeFormatter.ISO_DATE_TIME.format (aNowZDT) +
                                           ". The changes are reflected below."));
    }
    else
      if (aWPEC.hasAction (ACTION_RELOAD_TRUSTSTORE))
      {
        SMPTrustManager.reloadFromConfiguration ();
        aWPEC.postRedirectGetInternal (info ("The truststore was updated from the configuration at " +
                                             DateTimeFormatter.ISO_DATE_TIME.format (aNowZDT) +
                                             ". The changes are reflected below."));
      }
      else
        if (aWPEC.hasAction (ACTION_RELOAD_DIRECTORY_CONFIGURATION))
        {
          PDClientConfiguration.reloadConfiguration ();
          aWPEC.postRedirectGetInternal (info ("The " +
                                               sDirectoryName +
                                               " configuration was reloaded at " +
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
      if (SMPMetaManager.getSettings ().isDirectoryIntegrationEnabled ())
      {
        aToolbar.addChild (new BootstrapButton ().addChild ("Reload " + sDirectoryName + " configuration")
                                                 .setIcon (EDefaultIcon.REFRESH)
                                                 .setOnClick (aWPEC.getSelfHref ()
                                                                   .add (CPageParam.PARAM_ACTION,
                                                                         ACTION_RELOAD_DIRECTORY_CONFIGURATION)));
      }
      aNodeList.addChild (aToolbar);
    }

    final BootstrapTabBox aTabBox = aNodeList.addAndReturnChild (new BootstrapTabBox ());

    // Inline function to add a visual indicator if a certificate problem was
    // found
    final Function <IHCNode, IHCNode> addErrorHint = x -> x instanceof HCSpan ? x : new HCSpan ().addChild (x)
                                                                                                 .addChild (" ")
                                                                                                 .addChild (badgeDanger ("!!!"));

    final Function <IHCNode, IHCNode> addSuccessHint = x -> x instanceof HCSpan ? x : new HCSpan ().addChild (x)
                                                                                                   .addChild (" ")
                                                                                                   .addChild (badgeSuccess ("OK"));

    // SMP Key store
    {
      IHCNode aTabLabel = new HCTextNode ("Keystore");

      final HCNodeList aTab = new HCNodeList ();
      if (!SMPKeyManager.isKeyStoreValid ())
      {
        aTab.addChild (error (SMPKeyManager.getInitializationError ()));
        aTabLabel = addErrorHint.apply (aTabLabel);
      }
      else
      {
        // Successfully loaded private key
        final SMPKeyManager aKeyMgr = SMPKeyManager.getInstance ();

        final KeyStore aKeyStore = aKeyMgr.getKeyStore ();
        if (aKeyStore != null)
        {
          try
          {
            int nKeyEntries = 0;
            for (final String sAlias : new CommonsArrayList <> (aKeyStore.aliases ()))
            {
              if (aKeyStore.isKeyEntry (sAlias))
                nKeyEntries++;
            }
            if (nKeyEntries == 0)
            {
              aTab.addChild (error ("Found no private key entry in the configured key store."));
              aTabLabel = addErrorHint.apply (aTabLabel);
            }
            else
              if (nKeyEntries > 1)
              {
                aTab.addChild (warn ("The configured key store contains " +
                                     nKeyEntries +
                                     " key entries. It is highly recommended to have only the SMP key in the key store to avoid issues with the SML communication."));
                // No error hint here
              }
          }
          catch (final GeneralSecurityException ex)
          {
            aTab.addChild (error ("Error iterating key store.").addChild (SMPCommonUI.getTechnicalDetailsUI (ex)));
            aTabLabel = addErrorHint.apply (aTabLabel);
          }
        }

        final PrivateKeyEntry aKeyEntry = aKeyMgr.getPrivateKeyEntry ();
        if (aKeyEntry != null)
        {
          final Certificate [] aChain = aKeyEntry.getCertificateChain ();

          // Key store path and password are fine
          aTab.addChild (success (div ("Keystore is located at '" +
                                       SMPServerConfiguration.getKeyStorePath () +
                                       "' and was successfully loaded.")).addChild (div ("The private key with the alias '" +
                                                                                         SMPServerConfiguration.getKeyStoreKeyAlias () +
                                                                                         "' was successfully loaded.")));

          if (aChain.length > 0 && aChain[0] instanceof final X509Certificate aHead)
          {
            final String sIssuer = aHead.getIssuerX500Principal ().getName ();
            final EPredefinedCA eCert = EPredefinedCA.getFromIssuerOrNull (sIssuer);
            if (eCert != null)
            {
              if (eCert.isDeprecated ())
              {
                aTab.addChild (warn ("You are currently using a ").addChild (strong ("deprecated"))
                                                                  .addChild (" " +
                                                                             eCert.getDisplayName () +
                                                                             " certificate!"));
                aTabLabel = addErrorHint.apply (aTabLabel);
              }
              else
                aTab.addChild (info ("You are currently using a " + eCert.getDisplayName () + " certificate!"));
              if (aChain.length != eCert.getCertificateTreeLength ())
              {
                aTab.addChild (error ("The private key should be a chain of " +
                                      eCert.getCertificateTreeLength () +
                                      " certificates but it has " +
                                      aChain.length +
                                      " certificates. Please ensure that the respective root certificates are contained correctly!"));
                aTabLabel = addErrorHint.apply (aTabLabel);
              }
            }
            // else: we don't care
          }

          final String sAlias = SMPServerConfiguration.getKeyStoreKeyAlias ();
          final HCOL aOL = new HCOL ();
          for (final Certificate aCert : aChain)
          {
            if (aCert instanceof final X509Certificate aX509Cert)
            {
              final BootstrapTable aCertDetails = CertificateUI.createCertificateDetailsTable (sAlias,
                                                                                               aX509Cert,
                                                                                               aNowDT,
                                                                                               aDisplayLocale);
              aOL.addItem (aCertDetails);
            }
            else
            {
              aOL.addItem ("A chain entry is not an X.509 certificate! It is internally a " +
                           ClassHelper.getClassName (aCert));
              aTabLabel = addErrorHint.apply (aTabLabel);
            }
          }
          aTab.addChild (aOL);
        }
      }
      aTabBox.addTab ("keystore", addSuccessHint.apply (aTabLabel), aTab);
    }

    // SMP Trust store
    {
      IHCNode aTabLabel = new HCTextNode ("Truststore");

      final HCNodeList aTab = new HCNodeList ();
      if (!SMPTrustManager.isTrustStoreValid ())
      {
        aTab.addChild (warn (SMPTrustManager.getInitializationError ()));
        aTabLabel = addErrorHint.apply (aTabLabel);
      }
      else
      {
        // Successfully loaded trust store
        final SMPTrustManager aTrustMgr = SMPTrustManager.getInstance ();
        final KeyStore aTrustStore = aTrustMgr.getTrustStore ();

        // Trust store path and password are fine
        aTab.addChild (success (div ("Truststore is located at '" +
                                     SMPServerConfiguration.getTrustStorePath () +
                                     "' and was successfully loaded.")));

        final HCOL aOL = new HCOL ();
        try
        {
          for (final String sAlias : new CommonsArrayList <> (aTrustStore.aliases ()))
          {
            final Certificate aCert = aTrustStore.getCertificate (sAlias);
            if (aCert instanceof final X509Certificate aX509Cert)
            {
              final BootstrapTable aCertDetails = CertificateUI.createCertificateDetailsTable (sAlias,
                                                                                               aX509Cert,
                                                                                               aNowDT,
                                                                                               aDisplayLocale);
              aOL.addItem (aCertDetails);
            }
            else
            {
              aOL.addItem ("The entry with alias '" +
                           sAlias +
                           "' is not an X.509 certificate! It is internally a " +
                           ClassHelper.getClassName (aCert));
              aTabLabel = addErrorHint.apply (aTabLabel);
            }
          }
        }
        catch (final GeneralSecurityException ex)
        {
          aOL.addItem (error ("Error iterating trust store.").addChild (SMPCommonUI.getTechnicalDetailsUI (ex)));
          aTabLabel = addErrorHint.apply (aTabLabel);
        }
        aTab.addChild (aOL);
      }
      aTabBox.addTab ("truststore", addSuccessHint.apply (aTabLabel), aTab);
    }

    // Peppol Directory client certificate
    if (SMPMetaManager.getSettings ().isDirectoryIntegrationEnabled ())
    {
      // Directory client keystore
      {
        IHCNode aTabLabel = new HCTextNode (sDirectoryName + " Keystore");

        final HCNodeList aTab = new HCNodeList ();

        final LoadedKeyStore aKeyStoreLR = PDClientConfiguration.loadKeyStore ();
        if (aKeyStoreLR.isFailure ())
        {
          aTab.addChild (error (LoadedKeyStore.getLoadError (aKeyStoreLR)));
          aTabLabel = addErrorHint.apply (aTabLabel);
        }
        else
        {
          final String sKeyStorePath = PDClientConfiguration.getKeyStorePath ();
          final LoadedKey <KeyStore.PrivateKeyEntry> aKeyLoading = PDClientConfiguration.loadPrivateKey (aKeyStoreLR.getKeyStore ());
          if (aKeyLoading.isFailure ())
          {
            aTab.addChild (success (div ("Keystore is located at '" +
                                         sKeyStorePath +
                                         "' and was successfully loaded.")));
            aTab.addChild (error (LoadedKey.getLoadError (aKeyLoading)));
            aTabLabel = addErrorHint.apply (aTabLabel);
          }
          else
          {
            // Successfully loaded private key
            final String sAlias = PDClientConfiguration.getKeyStoreKeyAlias ();
            final PrivateKeyEntry aKeyEntry = aKeyLoading.getKeyEntry ();
            final Certificate [] aChain = aKeyEntry.getCertificateChain ();

            // Key store path and password are fine
            aTab.addChild (success (div ("Keystore is located at '" + sKeyStorePath + "' and was successfully loaded."))
                                                                                                                        .addChild (div ("The private key with the alias '" +
                                                                                                                                        sAlias +
                                                                                                                                        "' was successfully loaded.")));

            if (aChain.length > 0 && aChain[0] instanceof final X509Certificate aHead)
            {
              final String sIssuer = aHead.getIssuerX500Principal ().getName ();
              final EPredefinedCA eCert = EPredefinedCA.getFromIssuerOrNull (sIssuer);
              if (eCert != null)
              {
                if (eCert.isDeprecated ())
                {
                  aTab.addChild (warn ("You are currently using a ").addChild (strong ("deprecated"))
                                                                    .addChild (" " +
                                                                               eCert.getDisplayName () +
                                                                               " certificate!"));
                  aTabLabel = addErrorHint.apply (aTabLabel);
                }
                else
                  aTab.addChild (info ("You are currently using a " + eCert.getDisplayName () + " certificate!"));
                if (aChain.length != eCert.getCertificateTreeLength ())
                {
                  aTab.addChild (error ("The private key should be a chain of " +
                                        eCert.getCertificateTreeLength () +
                                        " certificates but it has " +
                                        aChain.length +
                                        " certificates. Please ensure that the respective root certificates are contained!"));
                  aTabLabel = addErrorHint.apply (aTabLabel);
                }
              }
              // else: we don't care
            }

            final HCOL aUL = new HCOL ();
            for (final Certificate aCert : aChain)
            {
              if (aCert instanceof final X509Certificate aX509Cert)
              {
                final BootstrapTable aCertDetails = CertificateUI.createCertificateDetailsTable (sAlias,
                                                                                                 aX509Cert,
                                                                                                 aNowDT,
                                                                                                 aDisplayLocale);
                aUL.addItem (aCertDetails);
              }
              else
              {
                aUL.addItem ("A chain entry is not an X.509 certificate! It is internally a " +
                             ClassHelper.getClassName (aCert));
                aTabLabel = addErrorHint.apply (aTabLabel);
              }
            }
            aTab.addChild (aUL);
          }
        }
        aTabBox.addTab ("pdkeystore", addSuccessHint.apply (aTabLabel), aTab);
      }

      // Directory client truststore
      {
        IHCNode aTabLabel = new HCTextNode (sDirectoryName + " Truststore");

        final HCNodeList aTab = new HCNodeList ();

        final LoadedKeyStore aTrustStoreLR = PDClientConfiguration.loadTrustStore ();
        if (aTrustStoreLR.isFailure ())
        {
          aTab.addChild (error (LoadedKeyStore.getLoadError (aTrustStoreLR)));
          aTabLabel = addErrorHint.apply (aTabLabel);
        }
        else
        {
          // Successfully loaded trust store
          final String sTrustStorePath = PDClientConfiguration.getTrustStorePath ();
          final KeyStore aTrustStore = aTrustStoreLR.getKeyStore ();

          // Trust store path and password are fine
          aTab.addChild (success (div ("Truststore is located at '" +
                                       sTrustStorePath +
                                       "' and was successfully loaded.")));

          final HCOL aOL = new HCOL ();
          try
          {
            for (final String sAlias : new CommonsArrayList <> (aTrustStore.aliases ()))
            {
              final Certificate aCert = aTrustStore.getCertificate (sAlias);
              if (aCert instanceof final X509Certificate aX509Cert)
              {
                final BootstrapTable aCertDetails = CertificateUI.createCertificateDetailsTable (sAlias,
                                                                                                 aX509Cert,
                                                                                                 aNowDT,
                                                                                                 aDisplayLocale);
                aOL.addItem (aCertDetails);
              }
              else
              {
                aOL.addItem ("The entry with alias '" +
                             sAlias +
                             "' is not an X.509 certificate! It is internally a " +
                             ClassHelper.getClassName (aCert));
                aTabLabel = addErrorHint.apply (aTabLabel);
              }
            }
          }
          catch (final GeneralSecurityException ex)
          {
            aOL.addItem (error ("Error iterating trust store.").addChild (SMPCommonUI.getTechnicalDetailsUI (ex)));
            aTabLabel = addErrorHint.apply (aTabLabel);
          }
          aTab.addChild (aOL);
        }
        aTabBox.addTab ("pdtruststore", addSuccessHint.apply (aTabLabel), aTab);
      }
    }
  }
}
