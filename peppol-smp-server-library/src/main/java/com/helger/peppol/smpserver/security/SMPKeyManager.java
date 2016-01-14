/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.utils.KeyStoreHelper;

/**
 * This class holds the private key for signing and the certificate for
 * checking.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class SMPKeyManager extends AbstractGlobalSingleton
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPKeyManager.class);

  private static final AtomicBoolean s_aCertificateValid = new AtomicBoolean (false);

  private final KeyStore.PrivateKeyEntry m_aKeyEntry;

  @Deprecated
  @UsedViaReflection
  public SMPKeyManager ()
  {
    // Load the KeyStore and get the signing key and certificate.
    try
    {
      final String sKeyStoreClassPath = SMPServerConfiguration.getKeystorePath ();
      final String sKeyStorePassword = SMPServerConfiguration.getKeystorePassword ();
      final String sKeyStoreKeyAlias = SMPServerConfiguration.getKeystoreKeyAlias ();
      final char [] aKeyStoreKeyPassword = SMPServerConfiguration.getKeystoreKeyPassword ();

      if (StringHelper.hasNoText (sKeyStoreClassPath))
        throw new InitializationException ("No SMP keystore path provided!");

      final KeyStore aKeyStore = KeyStoreHelper.loadKeyStore (sKeyStoreClassPath, sKeyStorePassword);
      final KeyStore.Entry aEntry = aKeyStore.getEntry (sKeyStoreKeyAlias,
                                                        new KeyStore.PasswordProtection (aKeyStoreKeyPassword));
      if (aEntry == null)
      {
        // Alias not found
        throw new InitializationException ("Failed to find key store alias '" +
                                           sKeyStoreKeyAlias +
                                           "' in keystore '" +
                                           sKeyStoreClassPath +
                                           "'. Does the alias exist? Is the key password correct?");
      }
      if (!(aEntry instanceof KeyStore.PrivateKeyEntry))
      {
        // Not a private key
        throw new InitializationException ("The keystore alias '" +
                                           sKeyStoreKeyAlias +
                                           "' was found in keystore '" +
                                           sKeyStoreClassPath +
                                           "' but it is not a private key! The internal type is " +
                                           aEntry.getClass ().getName ());
      }
      m_aKeyEntry = (KeyStore.PrivateKeyEntry) aEntry;
      s_aLogger.info ("SMPKeyManager initialized with keystore '" +
                      sKeyStoreClassPath +
                      "' and alias '" +
                      sKeyStoreKeyAlias +
                      "'");
    }
    catch (final IOException ex)
    {
      throw new InitializationException ("Error in constructor of SMPKeyManager", ex);
    }
    catch (final GeneralSecurityException ex)
    {
      throw new InitializationException ("Error in constructor of SMPKeyManager", ex);
    }
  }

  @Nonnull
  public static SMPKeyManager getInstance ()
  {
    return getGlobalSingleton (SMPKeyManager.class);
  }

  @Nonnull
  private PrivateKey _getPrivateKey ()
  {
    return m_aKeyEntry.getPrivateKey ();
  }

  @Nonnull
  private X509Certificate _getCertificate ()
  {
    return (X509Certificate) m_aKeyEntry.getCertificate ();
  }

  public void signXML (@Nonnull final Element aElementToSign) throws NoSuchAlgorithmException,
                                                              InvalidAlgorithmParameterException,
                                                              MarshalException,
                                                              XMLSignatureException
  {
    // Create a DOM XMLSignatureFactory that will be used to
    // generate the enveloped signature.
    final XMLSignatureFactory aSignatureFactory = XMLSignatureFactory.getInstance ("DOM");

    // Create a Reference to the enveloped document (in this case,
    // you are signing the whole document, so a URI of "" signifies
    // that, and also specify the SHA1 digest algorithm and
    // the ENVELOPED Transform)
    final Reference aReference = aSignatureFactory.newReference ("",
                                                                 aSignatureFactory.newDigestMethod (DigestMethod.SHA1,
                                                                                                    null),
                                                                 Collections.singletonList (aSignatureFactory.newTransform (Transform.ENVELOPED,
                                                                                                                            (TransformParameterSpec) null)),
                                                                 null,
                                                                 null);

    // Create the SignedInfo.
    final SignedInfo aSingedInfo = aSignatureFactory.newSignedInfo (aSignatureFactory.newCanonicalizationMethod (CanonicalizationMethod.INCLUSIVE,
                                                                                                                 (C14NMethodParameterSpec) null),
                                                                    aSignatureFactory.newSignatureMethod (SignatureMethod.RSA_SHA1,
                                                                                                          null),
                                                                    Collections.singletonList (aReference));

    // Create the KeyInfo containing the X509Data.
    final KeyInfoFactory aKeyInfoFactory = aSignatureFactory.getKeyInfoFactory ();
    final List <Object> aX509Content = new ArrayList <Object> ();
    aX509Content.add (_getCertificate ().getSubjectX500Principal ().getName ());
    aX509Content.add (_getCertificate ());
    final X509Data aX509Data = aKeyInfoFactory.newX509Data (aX509Content);
    final KeyInfo aKeyInfo = aKeyInfoFactory.newKeyInfo (Collections.singletonList (aX509Data));

    // Create a DOMSignContext and specify the RSA PrivateKey and
    // location of the resulting XMLSignature's parent element.
    final DOMSignContext aSignContext = new DOMSignContext (_getPrivateKey (), aElementToSign);

    // Create the XMLSignature, but don't sign it yet.
    final XMLSignature aSignature = aSignatureFactory.newXMLSignature (aSingedInfo, aKeyInfo);

    // Marshal, generate, and sign the enveloped signature.
    aSignature.sign (aSignContext);
  }

  public static void markCertificateValid ()
  {
    s_aCertificateValid.set (true);
  }

  public static boolean isCertificateValid ()
  {
    return s_aCertificateValid.get ();
  }
}
