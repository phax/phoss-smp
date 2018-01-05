/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.domain.serviceinfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.peppol.identifier.generic.process.SimpleProcessIdentifier;
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

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final SMPProcess aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getProcessIdentifier (),
                                                                    sNamespaceURI,
                                                                    ELEMENT_PROCESS_IDENTIFIER));
    for (final ISMPEndpoint aEndpoint : aValue.getAllEndpoints ())
      aElement.appendChild (MicroTypeConverter.convertToMicroElement (aEndpoint, sNamespaceURI, ELEMENT_ENDPOINT));
    if (aValue.hasExtension ())
      aElement.appendElement (sNamespaceURI, ELEMENT_EXTENSION).appendText (aValue.getExtensionAsString ());
    return aElement;
  }

  @Nonnull
  public SMPProcess convertToNative (@Nonnull final IMicroElement aElement)
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
