/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.ui;

import javax.xml.namespace.QName;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCHR;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.json.IJsonArray;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppol.sml.ESMPAPIType;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.extension.ISMPHasExtension;
import com.helger.photon.uictrls.prism.EPrismLanguage;
import com.helger.photon.uictrls.prism.HCPrismJS;
import com.helger.smpclient.bdxr1.marshal.AbstractBDXR1Marshaller;
import com.helger.smpclient.bdxr2.marshal.AbstractBDXR2Marshaller;
import com.helger.smpclient.extension.SMPExtension;
import com.helger.smpclient.extension.SMPExtensionList;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.EXMLSerializeXMLDeclaration;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.helger.xsds.bdxr.smp1.CBDXRSMP1;
import com.helger.xsds.bdxr.smp2.CBDXRSMP2;

import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBElement;

@Immutable
public final class SMPExtensionUI
{
  private static final ESMPAPIType API_TYPE = SMPServerConfiguration.getRESTType ().getAPIType ();
  public static final boolean ONLY_ONE_EXTENSION_ALLOWED = API_TYPE == ESMPAPIType.PEPPOL;

  private SMPExtensionUI ()
  {}

  @SuppressWarnings ("unused")
  private static final class PeppolExtensionMarshaller extends
                                                       AbstractBDXR1Marshaller <com.helger.xsds.peppol.smp1.ExtensionType>
  {
    private static final QName EXTENSION_QNAME = new QName ("http://busdox.org/serviceMetadata/publishing/1.0/",
                                                            "Extension");

    public PeppolExtensionMarshaller ()
    {
      super (com.helger.xsds.peppol.smp1.ExtensionType.class,
             ext -> new JAXBElement <> (EXTENSION_QNAME, com.helger.xsds.peppol.smp1.ExtensionType.class, null, ext));
    }
  }

  private static final class BDXR1ExtensionMarshaller extends
                                                      AbstractBDXR1Marshaller <com.helger.xsds.bdxr.smp1.ExtensionType>
  {
    private static final QName EXTENSION_QNAME = new QName (CBDXRSMP1.NAMESPACE_URI, "Extension");

    public BDXR1ExtensionMarshaller ()
    {
      super (com.helger.xsds.bdxr.smp1.ExtensionType.class,
             ext -> new JAXBElement <> (EXTENSION_QNAME, com.helger.xsds.bdxr.smp1.ExtensionType.class, null, ext));
    }
  }

  @SuppressWarnings ("unused")
  private static final class BDXR2ExtensionMarshaller extends
                                                      AbstractBDXR2Marshaller <com.helger.xsds.bdxr.smp2.ec.SMPExtensionType>
  {
    public BDXR2ExtensionMarshaller ()
    {
      super (com.helger.xsds.bdxr.smp2.ec.SMPExtensionType.class,
             CBDXRSMP2.getAllXSDIncludes (),
             new com.helger.xsds.bdxr.smp2.ec.ObjectFactory ()::createSMPExtension);
    }
  }

  @SuppressWarnings ("unused")
  private static final class BDXR2ExtensionsMarshaller extends
                                                       AbstractBDXR2Marshaller <com.helger.xsds.bdxr.smp2.ec.SMPExtensionsType>
  {
    public BDXR2ExtensionsMarshaller ()
    {
      super (com.helger.xsds.bdxr.smp2.ec.SMPExtensionsType.class,
             CBDXRSMP2.getAllXSDIncludes (),
             new com.helger.xsds.bdxr.smp2.ec.ObjectFactory ()::createSMPExtensions);
    }
  }

  @Nullable
  public static String getSerializedExtensionsForEdit (@NonNull final SMPExtensionList aExtensions)
  {
    if (ONLY_ONE_EXTENSION_ALLOWED)
      return aExtensions.getFirstExtensionXMLString ();

    if (false)
      if (API_TYPE == ESMPAPIType.OASIS_BDXR_V1)
      {
        String s = "";
        for (final SMPExtension aExt : aExtensions.extensions ())
          s += new BDXR1ExtensionMarshaller ().setFormattedOutput (true)
                                              .setUseSchema (false)
                                              .withXMLWriterSettings (xws -> xws.setSerializeXMLDeclaration (EXMLSerializeXMLDeclaration.IGNORE))
                                              .getAsString (aExt.getAsBDXRExtension ());
        return s;
      }

    final IJsonArray aJson = aExtensions.getExtensionsAsJson ();
    if (aJson != null)
      return aJson.getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);

    return null;
  }

  @NonNull
  public static IHCNode getSerializedExtensions (@NonNull final SMPExtensionList aExtensions)
  {
    final HCCode ret = new HCCode ();
    if (ONLY_ONE_EXTENSION_ALLOWED)
      ret.addChildren (HCExtHelper.nl2divList (aExtensions.getFirstExtensionXMLString ()));
    else
    {
      final IJsonArray aJson = aExtensions.getExtensionsAsJson ();
      if (aJson != null)
        ret.addChildren (HCExtHelper.nl2divList (aJson.getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED)));
    }
    return ret;
  }

  @Nullable
  public static IHCNode getExtensionDisplay (@NonNull final ISMPHasExtension aHasExtension)
  {
    final ICommonsList <SMPExtension> aExtensions = aHasExtension.getExtensions ().extensions ();
    if (aExtensions.isEmpty ())
      return null;

    final HCNodeList aNL = new HCNodeList ();
    for (final SMPExtension aExtension : aExtensions)
    {
      if (aNL.hasChildren ())
      {
        // add a separator line
        aNL.addChild (new HCHR ());
      }

      if (StringHelper.isNotEmpty (aExtension.getExtensionID ()))
        aNL.addChild (new HCDiv ().addChild ("Extension ID: ")
                                  .addChild (new HCCode ().addChild (aExtension.getExtensionID ())));

      // Use only the XML element of the first extension
      final String sXML = XMLWriter.getNodeAsString (aExtension.getAny (),
                                                     new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN));
      aNL.addChild (new HCPrismJS (EPrismLanguage.MARKUP).addChild (sXML));
    }
    return aNL;
  }
}
