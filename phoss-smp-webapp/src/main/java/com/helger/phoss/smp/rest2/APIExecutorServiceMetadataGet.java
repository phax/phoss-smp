/**
 * Copyright (C) 2014-2021 Philip Helger and contributors
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
package com.helger.phoss.smp.rest2;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.mime.CMimeType;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.restapi.BDXR1ServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPServerAPI;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.bdxr1.marshal.BDXR1MarshallerSignedServiceMetadataType;
import com.helger.smpclient.peppol.marshal.SMPMarshallerSignedServiceMetadataType;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.IXMLWriterSettings;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.helger.xml.transform.XMLTransformerFactory;

public final class APIExecutorServiceMetadataGet extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorServiceMetadataGet.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sServiceGroupID = aPathVariables.get (Rest2Filter.PARAM_SERVICE_GROUP_ID);
    final String sDocumentTypeID = aPathVariables.get (Rest2Filter.PARAM_DOCUMENT_TYPE_ID);
    final ISMPServerAPIDataProvider aDataProvider = new Rest2DataProvider (aRequestScope, sServiceGroupID);

    // Create the unsigned response document
    final Document aDoc;
    switch (SMPServerConfiguration.getRESTType ())
    {
      case PEPPOL:
      {
        final com.helger.xsds.peppol.smp1.SignedServiceMetadataType ret = new SMPServerAPI (aDataProvider).getServiceRegistration (sServiceGroupID,
                                                                                                                                   sDocumentTypeID);

        // Convert to DOM document
        // Disable XSD check, because Signature is added later
        final SMPMarshallerSignedServiceMetadataType aMarshaller = new SMPMarshallerSignedServiceMetadataType (false);
        aDoc = aMarshaller.getAsDocument (ret);
        break;
      }
      case OASIS_BDXR_V1:
      {
        final com.helger.xsds.bdxr.smp1.SignedServiceMetadataType ret = new BDXR1ServerAPI (aDataProvider).getServiceRegistration (sServiceGroupID,
                                                                                                                                   sDocumentTypeID);

        // Convert to DOM document
        // Disable XSD check, because Signature is added later
        final BDXR1MarshallerSignedServiceMetadataType aMarshaller = new BDXR1MarshallerSignedServiceMetadataType (false);
        aDoc = aMarshaller.getAsDocument (ret);
        break;
      }
      default:
        throw new UnsupportedOperationException ("Unsupported REST type specified!");
    }
    if (aDoc == null)
      throw new IllegalStateException ("Failed to serialize unsigned node!");

    // Sign the document
    try
    {
      SMPKeyManager.getInstance ().signXML (aDoc.getDocumentElement (), SMPServerConfiguration.getRESTType ().isBDXR ());
      LOGGER.info ("Successfully signed response XML");
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Error in signing the response XML", ex);
    }

    // Serialize the signed document
    try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
    {
      if (false)
      {
        // IMPORTANT: no indent and no align!
        final IXMLWriterSettings aSettings = XMLWriterSettings.createForCanonicalization ();

        // Write the result to a byte array
        if (XMLWriter.writeToStream (aDoc, aBAOS, aSettings).isFailure ())
          throw new IllegalStateException ("Failed to serialize signed node!");
      }
      else
      {
        // Use this because it correctly serializes &#13; which is important
        // for validating the signature!
        try
        {
          final Transformer aTransformer = XMLTransformerFactory.newTransformer ();
          aTransformer.transform (new DOMSource (aDoc), new StreamResult (aBAOS));
        }
        catch (final TransformerException ex)
        {
          throw new IllegalStateException ("Failed to serialized signed node", ex);
        }
      }

      aUnifiedResponse.setContent (aBAOS.toByteArray ())
                      .setMimeType (CMimeType.TEXT_XML)
                      .setCharset (XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ);
    }
  }
}
