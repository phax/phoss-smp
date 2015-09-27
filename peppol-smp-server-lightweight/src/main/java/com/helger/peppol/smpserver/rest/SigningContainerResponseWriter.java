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
package com.helger.peppol.smpserver.rest;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.xml.serialize.read.DOMReader;
import com.helger.commons.xml.serialize.write.EXMLIncorrectCharacterHandling;
import com.helger.commons.xml.serialize.write.EXMLSerializeIndent;
import com.helger.commons.xml.serialize.write.IXMLWriterSettings;
import com.helger.commons.xml.serialize.write.XMLWriter;
import com.helger.commons.xml.serialize.write.XMLWriterSettings;
import com.helger.commons.xml.transform.XMLTransformerFactory;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;

final class SigningContainerResponseWriter implements ContainerResponseWriter
{
  private final ContainerResponseWriter m_aCRW;
  private NonBlockingByteArrayOutputStream m_aBAOS;
  private ContainerResponse m_aResponse;

  SigningContainerResponseWriter (@Nonnull final ContainerResponseWriter aCRW)
  {
    m_aCRW = aCRW;
  }

  @Nonnull
  public OutputStream writeStatusAndHeaders (final long nContentLength,
                                             final ContainerResponse aResponse) throws IOException
  {
    m_aResponse = aResponse;
    m_aBAOS = new NonBlockingByteArrayOutputStream ();
    return m_aBAOS;
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
      SMPKeyManager.getInstance ().signXML (aDoc.getDocumentElement ());
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
        StreamHelper.close (aOS);
      }
    }
  }
}
