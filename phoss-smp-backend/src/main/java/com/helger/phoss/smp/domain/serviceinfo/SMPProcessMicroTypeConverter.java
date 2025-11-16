/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.serviceinfo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * This class is internally used to convert {@link SMPProcess} from and to XML.
 *
 * @author Philip Helger
 */
public final class SMPProcessMicroTypeConverter implements IMicroTypeConverter <SMPProcess>
{
  private static final String ELEMENT_PROCESS_IDENTIFIER = "processidentifier";
  private static final String ELEMENT_ENDPOINT = "endpoint";
  private static final String ELEMENT_EXTENSION = "extension";

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final SMPProcess aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.addChild (MicroTypeConverter.convertToMicroElement (aValue.getProcessIdentifier (),
                                                                 sNamespaceURI,
                                                                 ELEMENT_PROCESS_IDENTIFIER));
    for (final ISMPEndpoint aEndpoint : aValue.getAllEndpoints ())
      aElement.addChild (MicroTypeConverter.convertToMicroElement (aEndpoint, sNamespaceURI, ELEMENT_ENDPOINT));
    if (aValue.getExtensions ().extensions ().isNotEmpty ())
      aElement.addElementNS (sNamespaceURI, ELEMENT_EXTENSION)
              .addText (aValue.getExtensions ().getExtensionsAsJsonString ());
    return aElement;
  }

  @NonNull
  public SMPProcess convertToNative (@NonNull final IMicroElement aElement)
  {
    final SimpleProcessIdentifier aProcessIdentifier = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_PROCESS_IDENTIFIER),
                                                                                           SimpleProcessIdentifier.class);
    final ICommonsList <SMPEndpoint> aEndpoints = new CommonsArrayList <> ();
    for (final IMicroElement aEndpoint : aElement.getAllChildElements (ELEMENT_ENDPOINT))
      aEndpoints.add (MicroTypeConverter.convertToNative (aEndpoint, SMPEndpoint.class));
    final String sExtension = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_EXTENSION);

    return new SMPProcess (aProcessIdentifier, aEndpoints, sExtension);
  }
}
