/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.security;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
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
import javax.xml.crypto.dsig.spec.SignatureMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import org.apache.xml.security.c14n.Canonicalizer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.http.security.TrustManagerTrustAll;
import com.helger.phoss.smp.ESMPRESTType;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.restapi.cache.SMPRestResponseCache;
import com.helger.scope.singleton.AbstractGlobalSingleton;
import com.helger.security.keystore.EKeyStoreLoadError;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;

/**
 * This class holds the private key for signing and the certificate for checking.
 *
 * @author Philip Helger
 */
public final class SMPKeyManager extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPKeyManager.class);

  private static final AtomicBoolean KEY_STORE_VALID = new AtomicBoolean (false);
  private static EKeyStoreLoadError s_eInitError;
  private static String s_sInitError;

  private KeyStore m_aKeyStore;
  private KeyStore.PrivateKeyEntry m_aKeyEntry;

  private static void _setKeyStoreValid (final boolean bValid)
  {
    KEY_STORE_VALID.set (bValid);
  }

  private static void _loadError (@Nullable final EKeyStoreLoadError eInitError, @Nullable final String sInitError)
  {
    s_eInitError = eInitError;
    s_sInitError = sInitError;
  }

  private void _loadKeyStore ()
  {
    // Reset every time
    _setKeyStoreValid (false);
    _loadError (null, null);
    m_aKeyStore = null;
    m_aKeyEntry = null;

    // Load the key store and get the signing key
    final LoadedKeyStore aLoadedKeyStore = KeyStoreHelper.loadKeyStore (SMPServerConfiguration.getKeyStoreType (),
                                                                        SMPServerConfiguration.getKeyStorePath (),
                                                                        SMPServerConfiguration.getKeyStorePassword ());
    if (aLoadedKeyStore.isFailure ())
    {
      _loadError (aLoadedKeyStore.getError (), LoadedKeyStore.getLoadError (aLoadedKeyStore));
      throw new InitializationException (s_sInitError);
    }
    m_aKeyStore = aLoadedKeyStore.getKeyStore ();

    final LoadedKey <KeyStore.PrivateKeyEntry> aLoadedKey = KeyStoreHelper.loadPrivateKey (m_aKeyStore,
                                                                                           SMPServerConfiguration.getKeyStorePath (),
                                                                                           SMPServerConfiguration.getKeyStoreKeyAlias (),
                                                                                           SMPServerConfiguration.getKeyStoreKeyPassword ());
    if (aLoadedKey.isFailure ())
    {
      _loadError (aLoadedKey.getError (), LoadedKey.getLoadError (aLoadedKey));
      throw new InitializationException (s_sInitError);
    }

    m_aKeyEntry = aLoadedKey.getKeyEntry ();
    LOGGER.info ("SMPKeyManager successfully initialized with keystore '" +
                 SMPServerConfiguration.getKeyStorePath () +
                 "' and alias '" +
                 SMPServerConfiguration.getKeyStoreKeyAlias () +
                 "'");
    _setKeyStoreValid (true);
  }

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public SMPKeyManager ()
  {
    _loadKeyStore ();
  }

  @NonNull
  public static SMPKeyManager getInstance ()
  {
    return getGlobalSingleton (SMPKeyManager.class);
  }

  /**
   * @return The configured keystore. May be <code>null</code> if loading failed. In that case check
   *         the result of {@link #getInitializationError()}.
   */
  @Nullable
  public KeyStore getKeyStore ()
  {
    return m_aKeyStore;
  }

  /**
   * @return The configured private key. May be <code>null</code> if loading failed. In that case
   *         check the result of {@link #getInitializationError()}.
   */
  public KeyStore.@Nullable PrivateKeyEntry getPrivateKeyEntry ()
  {
    return m_aKeyEntry;
  }

  @Nullable
  public X509Certificate getPrivateKeyCertificate ()
  {
    if (m_aKeyEntry != null)
    {
      final Certificate aCert = m_aKeyEntry.getCertificate ();
      if (aCert instanceof X509Certificate)
        return (X509Certificate) aCert;
    }
    return null;
  }

  /**
   * Create an SSLContext based on the configured key store and trust store. This is required for
   * communication with the SMI/SML as well as other network dependent components like the Peppol
   * Directory.
   *
   * @return A new {@link SSLContext} and never <code>null</code>.
   * @throws GeneralSecurityException
   *         In case something goes wrong :)
   */
  @NonNull
  public SSLContext createSSLContext () throws GeneralSecurityException
  {
    // Key manager
    final KeyManagerFactory aKeyManagerFactory = KeyManagerFactory.getInstance (KeyManagerFactory.getDefaultAlgorithm ());
    aKeyManagerFactory.init (getKeyStore (), SMPServerConfiguration.getKeyStoreKeyPassword ());

    // Trust manager
    final TrustManager [] aTrustManagers;
    if (SMPTrustManager.isTrustStoreValid ())
    {
      // Explicitly use the configured truststore
      final TrustManagerFactory aTrustManagerFactory = TrustManagerFactory.getInstance (TrustManagerFactory.getDefaultAlgorithm ());
      aTrustManagerFactory.init (SMPTrustManager.getInstance ().getTrustStore ());
      aTrustManagers = aTrustManagerFactory.getTrustManagers ();
    }
    else
    {
      // No trust store defined
      aTrustManagers = new TrustManager [] { new TrustManagerTrustAll () };
      LOGGER.warn ("No truststore is configured, so the build SSL/TLS connection will trust all hosts!");
    }

    // Assign key manager and trust manager to SSL/TLS context
    final SSLContext aSSLCtx = SSLContext.getInstance ("TLS");
    aSSLCtx.init (aKeyManagerFactory.getKeyManagers (), aTrustManagers, null);
    return aSSLCtx;
  }

  /**
   * Determine the XMLDSig signature method that fits a private key.
   * <p>
   * SHA-256 throughout, matching the digest method used for the reference. Only the key algorithm
   * varies, and it has to: a signature method and a key of different families cannot be combined,
   * and the failure surfaces as an <code>InvalidKeyException</code> at signing time.
   * </p>
   *
   * @param aPrivateKey
   *        The private key the response is signed with. May not be <code>null</code>.
   * @return The URI of the signature method. Never <code>null</code>.
   * @throws IllegalStateException
   *         if the key algorithm is not supported for XML signatures.
   * @since 8.6.2
   */
  @NonNull
  public static String getSignatureMethodForKey (@NonNull final PrivateKey aPrivateKey)
  {
    ValueEnforcer.notNull (aPrivateKey, "PrivateKey");

    final String sAlgorithm = aPrivateKey.getAlgorithm ();
    // "EC" is what the JDK reports, "ECDSA" is what BouncyCastle reports for the same key
    if ("EC".equalsIgnoreCase (sAlgorithm) || "ECDSA".equalsIgnoreCase (sAlgorithm))
      return SignatureMethod.ECDSA_SHA256;
    if ("RSA".equalsIgnoreCase (sAlgorithm))
      return SignatureMethod.RSA_SHA256;

    // Ed25519 is deliberately absent: SignatureMethod.ED25519 only exists from Java 21 on, and this
    // project compiles against 17. Add it when the release target moves.
    throw new IllegalStateException ("The key algorithm '" +
                                     sAlgorithm +
                                     "' is not supported for signing SMP responses. Supported are RSA and EC.");
  }

  /**
   * Sign the provided element with the configured certificate using XMLDSig.
   *
   * @param aElementToSign
   *        The XML element to sign. May not be <code>null</code>.
   * @param eRESTType
   *        The REST type currently configured. It decides the canonicalization algorithm. The
   *        signature method follows the key algorithm instead - see
   *        {@link #getSignatureMethodForKey(PrivateKey)}.
   * @throws NoSuchAlgorithmException
   *         An algorithm is not supported by the underlying platform.
   * @throws InvalidAlgorithmParameterException
   *         Parameters for certain algorithms are invalid.
   * @throws MarshalException
   *         Marshalling the signature failed
   * @throws XMLSignatureException
   *         Some XMLDSig specific stuff failed
   */
  public void signXML (@NonNull final Element aElementToSign,
                       @NonNull final ESMPRESTType eRESTType) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, MarshalException, XMLSignatureException
  {
    ValueEnforcer.notNull (aElementToSign, "ElementToSign");
    ValueEnforcer.notNull (eRESTType, "RESTType");

    // Create a DOM XMLSignatureFactory that will be used to
    // generate the enveloped signature.
    final XMLSignatureFactory aSignatureFactory = XMLSignatureFactory.getInstance ("DOM");

    // Create a Reference to the enveloped document (in this case,
    // you are signing the whole document, so a URI of "" signifies
    // that, and also specify the SHA1 digest algorithm and
    // the ENVELOPED Transform)
    // * Peppol SMP Spec 1.3.0 changed from SHA-1 to SHA-256
    final String sDigestAlgo = DigestMethod.SHA256;
    final Reference aReference = aSignatureFactory.newReference ("",
                                                                 aSignatureFactory.newDigestMethod (sDigestAlgo, null),
                                                                 new CommonsArrayList <> (aSignatureFactory.newTransform (Transform.ENVELOPED,
                                                                                                                          (TransformParameterSpec) null)),
                                                                 (String) null,
                                                                 (String) null);

    // Create the SignedInfo.
    // * Before Peppol SMP Spec 1.2.0 this was EXCLUSIVE, since 1.2.0 it is
    // INCLUSIVE as of May 1st, 2022
    // * OASIS BDXR always used INCLUSIVE
    // * CIPA and this server always used INCLUSIVE, but this was changed for
    // 5.0.1 to EXCLUSIVE
    // * Peppol SMP Spec 1.3.0 changed from SHA-1 to SHA-256
    final String sC18N = switch (eRESTType)
    {
      case PEPPOL -> CanonicalizationMethod.INCLUSIVE;
      case OASIS_BDXR_V1 -> CanonicalizationMethod.INCLUSIVE;
      case OASIS_BDXR_V2 -> Canonicalizer.ALGO_ID_C14N11_OMIT_COMMENTS;
      default -> throw new IllegalStateException ("Unsupported REST type");
    };

    // The signature method follows the key and not the REST type. Up to and including 8.6.1 this
    // was hard coded to RSA-SHA256 for all three REST types, which made an SMP with an EC key
    // unusable: signing failed with "InvalidKeyException: No installed provider supports this key"
    // on every ServiceMetadata query, because an RSA Signature instance refuses an EC key whatever
    // provider is installed. Networks whose PKI issues EC keys - such as SilvaConnect on secp256r1 -
    // could not be served at all.
    final String sSignatureMethod = getSignatureMethodForKey (m_aKeyEntry.getPrivateKey ());
    final SignedInfo aSingedInfo = aSignatureFactory.newSignedInfo (aSignatureFactory.newCanonicalizationMethod (sC18N,
                                                                                                                 (C14NMethodParameterSpec) null),
                                                                    aSignatureFactory.newSignatureMethod (sSignatureMethod,
                                                                                                          (SignatureMethodParameterSpec) null),
                                                                    new CommonsArrayList <> (aReference));

    // Create the KeyInfo containing the X509Data.
    final KeyInfoFactory aKeyInfoFactory = aSignatureFactory.getKeyInfoFactory ();
    final X509Certificate aCert = (X509Certificate) m_aKeyEntry.getCertificate ();
    final X509Data aX509Data = aKeyInfoFactory.newX509Data (new CommonsArrayList <> (aCert.getSubjectX500Principal ()
                                                                                          .getName (), aCert));
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
   * @return A shortcut method to determine if the certification configuration is valid or not. This
   *         method can be used, even if {@link #getInstance()} throws an exception.
   */
  public static boolean isKeyStoreValid ()
  {
    return KEY_STORE_VALID.get ();
  }

  /**
   * If the certificate is not valid according to {@link #isKeyStoreValid()} this method can be used
   * to determine the error detail code.
   *
   * @return <code>null</code> if initialization was successful.
   */
  @Nullable
  public static EKeyStoreLoadError getInitializationErrorCode ()
  {
    return s_eInitError;
  }

  /**
   * If the certificate is not valid according to {@link #isKeyStoreValid()} this method can be used
   * to determine the error detail message.
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
      final SMPKeyManager aInstance = getGlobalSingletonIfInstantiated (SMPKeyManager.class);
      if (aInstance != null)
        aInstance._loadKeyStore ();
      else
      {
        // _loadKeyStore () is called in the constructor
        getInstance ();
      }

      // Cached responses were signed with the previous key
      SMPRestResponseCache.invalidateAll ();
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to reload from configuration", ex);
    }
  }
}
