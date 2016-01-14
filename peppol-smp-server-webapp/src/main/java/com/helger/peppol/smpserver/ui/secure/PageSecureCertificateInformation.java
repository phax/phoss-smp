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

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.annotation.Nonnull;

import org.joda.time.LocalDateTime;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.ClassHelper;
import com.helger.datetime.PDTFactory;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPage;
import com.helger.peppol.smpserver.ui.AppCommonUI;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
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

    final KeyLoadingResult aKeyLoadingResult = KeyLoadingResult.loadConfiguredKey ();
    if (aKeyLoadingResult.isFailure ())
    {
      aNodeList.addChild (new BootstrapErrorBox ().addChild (aKeyLoadingResult.getErrorMessage ()));
    }
    else
    {
      // Successfully loaded private key
      final PrivateKeyEntry aKeyEntry = aKeyLoadingResult.getKeyEntry ();
      final Certificate [] aChain = aKeyEntry.getCertificateChain ();

      // Key store path and password are fine
      aNodeList.addChild (new BootstrapSuccessBox ().addChild (new HCDiv ().addChild ("Keystore is located at '" + SMPServerConfiguration.getKeystorePath () + "' and was successfully loaded."))
                                                    .addChild (new HCDiv ().addChild ("The private key with the alias '" +
                                                                                      SMPServerConfiguration.getKeystoreKeyAlias () +
                                                                                      "' was successfully loaded.")));

      if (aChain.length != 3)
        aNodeList.addChild (new BootstrapWarnBox ().addChild ("The private key should be a chain of 3 certificates but it has " +
                                                              aChain.length +
                                                              " certificates. Please ensure that the respective root certificates are contained!"));

      final LocalDateTime aNowLDT = PDTFactory.getCurrentLocalDateTime ();
      final HCOL aUL = new HCOL ();
      for (final Certificate aCert : aChain)
      {
        if (aCert instanceof X509Certificate)
        {
          final X509Certificate aX509Cert = (X509Certificate) aCert;
          final BootstrapTable aCertDetails = AppCommonUI.createCertificateDetailsTable (aX509Cert, aNowLDT, aDisplayLocale);
          aUL.addItem (aCertDetails.getAsResponsiveTable ());
        }
        else
          aUL.addItem ("The certificate is not an X.509 certificate! It is internally a " + ClassHelper.getClassName (aCert));
      }
      aNodeList.addChild (aUL);
    }
  }
}
