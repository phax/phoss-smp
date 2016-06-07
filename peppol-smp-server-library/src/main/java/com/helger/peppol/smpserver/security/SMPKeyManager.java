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
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
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
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
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

  private KeyStore m_aTrustStore;
  private String m_sInitError;
  private KeyStore m_aKeyStore;
  private KeyStore.PrivateKeyEntry m_aKeyEntry;

  private final void _reload () throws InitializationException
  {
    // Reset every time
    m_sInitError = null;
    m_aKeyStore = null;
    m_aKeyEntry = null;

    // Load the KeyStore and get the signing key and certificate.
    final SMPKeyStoreLoadingResult aKeyStoreLoading = SMPKeyStoreLoadingResult.loadConfiguredKeyStore ();
    if (aKeyStoreLoading.isFailure ())
    {
      m_sInitError = aKeyStoreLoading.getErrorMessage ();
      throw new InitializationException (m_sInitError);
    }
    m_aKeyStore = aKeyStoreLoading.getKeyStore ();

    final SMPKeyLoadingResult aKeyLoading = SMPKeyLoadingResult.loadConfiguredKey (m_aKeyStore);
    if (aKeyLoading.isFailure ())
    {
      m_sInitError = aKeyLoading.getErrorMessage ();
      throw new InitializationException (m_sInitError);
    }

    m_aKeyEntry = aKeyLoading.getKeyEntry ();
    s_aLogger.info ("SMPKeyManager successfully initialized with keystore '" +
                    SMPServerConfiguration.getKeystorePath () +
                    "' and alias '" +
                    SMPServerConfiguration.getKeystoreKeyAlias () +
                    "'");
  }

  private final void _ensureLoaded () throws InitializationException
  {
    if (!isCertificateValid ())
    {
      _reload ();
      internalMarkCertificateValid ();
    }
  }

  @Deprecated
  @UsedViaReflection
  public SMPKeyManager ()
  {
    // Load trust store
    try
    {
      m_aTrustStore = KeyStoreHelper.loadKeyStore (KeyStoreHelper.TRUSTSTORE_COMPLETE_CLASSPATH,
                                                   KeyStoreHelper.TRUSTSTORE_PASSWORD);
    }
    catch (GeneralSecurityException | IOException ex)
    {
      throw new InitializationException ("Failed to load PEPPOL truststore", ex);
    }
    _ensureLoaded ();
  }

  @Nonnull
  public static SMPKeyManager getInstance ()
  {
    return getGlobalSingleton (SMPKeyManager.class);
  }

  public boolean hasInitializationError ()
  {
    return getInitializationError () != null;
  }

  @Nullable
  public String getInitializationError ()
  {
    _ensureLoaded ();
    return m_sInitError;
  }

  /**
   * @return The global trust store to be used. This trust store is never
   *         reloaded and must be present.
   */
  @Nonnull
  public KeyStore getTrustStore ()
  {
    return m_aTrustStore;
  }

  /**
   * @return The configured keystore. May be <code>null</code> if loading
   *         failed. In that case check the result of
   *         {@link #getInitializationError()}.
   */
  @Nullable
  public KeyStore getKeyStore ()
  {
    _ensureLoaded ();
    return m_aKeyStore;
  }

  /**
   * @return The configured private key. May be <code>null</code> if loading
   *         failed. In that case check the result of
   *         {@link #getInitializationError()}.
   */
  @Nullable
  public KeyStore.PrivateKeyEntry getPrivateKeyEntry ()
  {
    _ensureLoaded ();
    return m_aKeyEntry;
  }

  @Nonnull
  public SSLContext createSSLContext () throws GeneralSecurityException
  {
    // Key manager
    final KeyManagerFactory aKeyManagerFactory = KeyManagerFactory.getInstance (KeyManagerFactory.getDefaultAlgorithm ());
    aKeyManagerFactory.init (getKeyStore (), SMPServerConfiguration.getKeystoreKeyPassword ());

    // Trust manager
    final TrustManagerFactory aTrustManagerFactory = TrustManagerFactory.getInstance (TrustManagerFactory.getDefaultAlgorithm ());
    aTrustManagerFactory.init (getTrustStore ());

    // Assign key manager and empty trust manager to SSL/TLS context
    final SSLContext aSSLCtx = SSLContext.getInstance ("TLS");
    aSSLCtx.init (aKeyManagerFactory.getKeyManagers (),
                  aTrustManagerFactory.getTrustManagers (),
                  RandomHelper.getSecureRandom ());
    return aSSLCtx;
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
                                                                 new CommonsArrayList <> (aSignatureFactory.newTransform (Transform.ENVELOPED,
                                                                                                                          (TransformParameterSpec) null)),
                                                                 null,
                                                                 null);

    // Create the SignedInfo.
    final SignedInfo aSingedInfo = aSignatureFactory.newSignedInfo (aSignatureFactory.newCanonicalizationMethod (CanonicalizationMethod.INCLUSIVE,
                                                                                                                 (C14NMethodParameterSpec) null),
                                                                    aSignatureFactory.newSignatureMethod (SignatureMethod.RSA_SHA1,
                                                                                                          null),
                                                                    new CommonsArrayList <> (aReference));

    // Create the KeyInfo containing the X509Data.
    final KeyInfoFactory aKeyInfoFactory = aSignatureFactory.getKeyInfoFactory ();
    final ICommonsList <Object> aX509Content = new CommonsArrayList <> ();
    final X509Certificate aCert = (X509Certificate) m_aKeyEntry.getCertificate ();
    aX509Content.add (aCert.getSubjectX500Principal ().getName ());
    aX509Content.add (aCert);
    final X509Data aX509Data = aKeyInfoFactory.newX509Data (aX509Content);
    final KeyInfo aKeyInfo = aKeyInfoFactory.newKeyInfo (new CommonsArrayList <> (aX509Data));

    // Create a DOMSignContext and specify the RSA PrivateKey and
    // location of the resulting XMLSignature's parent element.
    final DOMSignContext aSignContext = new DOMSignContext (m_aKeyEntry.getPrivateKey (), aElementToSign);

    // Create the XMLSignature, but don't sign it yet.
    final XMLSignature aSignature = aSignatureFactory.newXMLSignature (aSingedInfo, aKeyInfo);

    // Marshal, generate, and sign the enveloped signature.
    aSignature.sign (aSignContext);
  }

  /**
   * This method is only to be called on startup to indicate that the
   * certificate configuration from the config file is valid.
   */
  static void internalMarkCertificateValid ()
  {
    s_aCertificateValid.set (true);
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
}
