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
package com.helger.phoss.smp.rest;

import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.annotation.Nonempty;
import com.helger.base.debug.GlobalDebug;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.string.StringHelper;
import com.helger.mime.CMimeType;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.exception.SMPInternalErrorException;
import com.helger.phoss.smp.restapi.BDXR1ServerAPI;
import com.helger.phoss.smp.restapi.BDXR2ServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPServerAPI;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.smpclient.bdxr1.marshal.BDXR1MarshallerSignedServiceMetadataType;
import com.helger.smpclient.bdxr1.marshal.BDXR1NamespaceContext;
import com.helger.smpclient.bdxr2.marshal.BDXR2MarshallerServiceMetadata;
import com.helger.smpclient.peppol.marshal.SMPMarshallerSignedServiceMetadataType;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.IXMLWriterSettings;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.helger.xml.transform.XMLTransformerFactory;
import com.helger.xsds.bdxr.smp1.CBDXRSMP1;

import jakarta.annotation.Nonnull;

public final class APIExecutorServiceMetadataGet extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorServiceMetadataGet.class);

  @Override
  protected void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                            @Nonnull @Nonempty final String sPath,
                            @Nonnull final Map <String, String> aPathVariables,
                            @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                            @Nonnull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final String sPathDocumentTypeID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_DOCUMENT_TYPE_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Create the unsigned response document
    final Document aDoc;
    switch (SMPServerConfiguration.getRESTType ())
    {
      case PEPPOL:
      {
        final var ret = new SMPServerAPI (aDataProvider).getServiceRegistration (sPathServiceGroupID,
                                                                                 sPathDocumentTypeID);

        // Convert to DOM document
        final SMPMarshallerSignedServiceMetadataType aMarshaller = new SMPMarshallerSignedServiceMetadataType ();
        // Disable XSD check, because Signature is added later
        aMarshaller.setUseSchema (false);
        aDoc = aMarshaller.getAsDocument (ret);
        break;
      }
      case OASIS_BDXR_V1:
      {
        final var ret = new BDXR1ServerAPI (aDataProvider).getServiceRegistration (sPathServiceGroupID,
                                                                                   sPathDocumentTypeID);

        // Convert to DOM document
        final BDXR1MarshallerSignedServiceMetadataType aMarshaller = new BDXR1MarshallerSignedServiceMetadataType ();
        // Disable XSD check, because Signature is added later
        aMarshaller.setUseSchema (false);

        if (SMPServerConfiguration.isHREdeliveryExtensionMode ())
        {
          // Special namespace prefix to identify phoss SMP instances
          var aSpecialNSCtx = BDXR1NamespaceContext.getInstance ().getClone ();
          aSpecialNSCtx.removeMapping (CBDXRSMP1.DEFAULT_PREFIX);
          aSpecialNSCtx.addMapping (CBDXRSMP1.DEFAULT_PREFIX + "hr", CBDXRSMP1.NAMESPACE_URI);
          aSpecialNSCtx.addMapping ("hrext", CSMPServer.HR_EXTENSION_NAMESPACE_URI);
          aMarshaller.setNamespaceContext (aSpecialNSCtx);
        }

        aDoc = aMarshaller.getAsDocument (ret);
        break;
      }
      case OASIS_BDXR_V2:
      {
        final var ret = new BDXR2ServerAPI (aDataProvider).getServiceRegistration (sPathServiceGroupID,
                                                                                   sPathDocumentTypeID);

        // Convert to DOM document
        final BDXR2MarshallerServiceMetadata aMarshaller = new BDXR2MarshallerServiceMetadata ();
        // Disable XSD check, because Signature is added later
        aMarshaller.setUseSchema (false);
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
      SMPKeyManager.getInstance ().signXML (aDoc.getDocumentElement (), SMPServerConfiguration.getRESTType ());
      LOGGER.info ("Successfully signed response XML");
    }
    catch (final Exception ex)
    {
      throw new SMPInternalErrorException ("Error in signing the response XML", ex);
    }

    if (GlobalDebug.isDebugMode ())
    {
      LOGGER.info ("Running post-signing XML Schema validation in debug mode");

      // Run the XML Schema validation after the signing
      switch (SMPServerConfiguration.getRESTType ())
      {
        case PEPPOL:
        {
          // Verify DOM document
          final SMPMarshallerSignedServiceMetadataType aMarshaller = new SMPMarshallerSignedServiceMetadataType ();
          if (aMarshaller.read (aDoc) == null)
            LOGGER.error ("Signed response document is not XML Schema compliant");
          break;
        }
        case OASIS_BDXR_V1:
        {
          // Verify DOM document
          final BDXR1MarshallerSignedServiceMetadataType aMarshaller = new BDXR1MarshallerSignedServiceMetadataType ();
          if (aMarshaller.read (aDoc) == null)
            LOGGER.error ("Signed response document is not XML Schema compliant");
          break;
        }
        case OASIS_BDXR_V2:
        {
          // Verify DOM document
          final BDXR2MarshallerServiceMetadata aMarshaller = new BDXR2MarshallerServiceMetadata ();
          if (aMarshaller.read (aDoc) == null)
            LOGGER.error ("Signed response document is not XML Schema compliant");
          break;
        }
        default:
          throw new UnsupportedOperationException ("Unsupported REST type specified!");
      }
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
