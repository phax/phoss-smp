/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.streams.StreamUtils;
import com.helger.commons.xml.EXMLIncorrectCharacterHandling;
import com.helger.commons.xml.serialize.DOMReader;
import com.helger.commons.xml.serialize.EXMLSerializeIndent;
import com.helger.commons.xml.serialize.IXMLWriterSettings;
import com.helger.commons.xml.serialize.XMLWriter;
import com.helger.commons.xml.serialize.XMLWriterSettings;
import com.helger.commons.xml.transform.XMLTransformerFactory;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;

final class SigningContainerResponseWriter implements ContainerResponseWriter
{
  private final ContainerResponseWriter m_aCRW;
  private final PrivateKeyEntry m_aKeyEntry;
  private final X509Certificate m_aCert;
  private NonBlockingByteArrayOutputStream m_aBAOS;
  private ContainerResponse m_aResponse;

  SigningContainerResponseWriter (@Nonnull final ContainerResponseWriter crw,
                                  @Nonnull final PrivateKeyEntry aKeyEntry,
                                  @Nonnull final X509Certificate aCert)
  {
    m_aCRW = crw;
    m_aKeyEntry = aKeyEntry;
    m_aCert = aCert;
  }

  public OutputStream writeStatusAndHeaders (final long nContentLength, final ContainerResponse aResponse) throws IOException
  {
    m_aResponse = aResponse;
    return m_aBAOS = new NonBlockingByteArrayOutputStream ();
  }

  private void _signXML (final Element aElementToSign) throws NoSuchAlgorithmException,
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
    aX509Content.add (m_aCert.getSubjectX500Principal ().getName ());
    aX509Content.add (m_aCert);
    final X509Data aX509Data = aKeyInfoFactory.newX509Data (aX509Content);
    final KeyInfo aKeyInfo = aKeyInfoFactory.newKeyInfo (Collections.singletonList (aX509Data));

    // Create a DOMSignContext and specify the RSA PrivateKey and
    // location of the resulting XMLSignature's parent element.
    final DOMSignContext dsc = new DOMSignContext (m_aKeyEntry.getPrivateKey (), aElementToSign);

    // Create the XMLSignature, but don't sign it yet.
    final XMLSignature signature = aSignatureFactory.newXMLSignature (aSingedInfo, aKeyInfo);

    // Marshal, generate, and sign the enveloped signature.
    signature.sign (dsc);
  }

  public void finish () throws IOException
  {
    final byte [] aContent = m_aBAOS.toByteArray ();
    final OutputStream aOS = m_aCRW.writeStatusAndHeaders (-1, m_aResponse);

    // Do security work here wrapping content and writing out XMLDSIG stuff to
    // out

    // Parse current response to XML
    Document aDoc;
    try
    {
      aDoc = DOMReader.readXMLDOM (aContent);
    }
    catch (final Exception e)
    {
      throw new RuntimeException ("Error in parsing xml", e);
    }

    // Sign the document
    try
    {
      _signXML (aDoc.getDocumentElement ());
    }
    catch (final Exception e)
    {
      throw new RuntimeException ("Error in signing xml", e);
    }

    if (false)
    {
      // And write the result to the main output stream
      // IMPORTANT: no indent and no align!
      final IXMLWriterSettings aSettings = new XMLWriterSettings ().setIncorrectCharacterHandling (EXMLIncorrectCharacterHandling.THROW_EXCEPTION)
                                                                   .setIndent (EXMLSerializeIndent.NONE);
      if (XMLWriter.writeToStream (aDoc, aOS, aSettings).isFailure ())
        throw new RuntimeException ("Failed to serialize node!");
    }
    else
    {
      // Use this because it correctly serializes &#13; which is important for
      // validating the signature!
      try
      {
        XMLTransformerFactory.newTransformer ().transform (new DOMSource (aDoc), new StreamResult (aOS));
      }
      catch (final TransformerException ex)
      {
        throw new IllegalStateException ("Failed to save to XML", ex);
      }
      finally
      {
        StreamUtils.close (aOS);
      }
    }
  }
}
