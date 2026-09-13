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
package com.helger.phoss.smp.domain.accesspoint;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * This class is internally used to convert {@link SMPAccessPoint} from and to XML.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public final class SMPAccessPointMicroTypeConverter implements IMicroTypeConverter <SMPAccessPoint>
{
  public static final MicroQName ATTR_ID = new MicroQName ("id");
  public static final MicroQName ATTR_NAME = new MicroQName ("name");
  public static final MicroQName ATTR_ENDPOINT_REFERENCE = new MicroQName ("endpointref");
  public static final String ELEMENT_CERTIFICATE = "certificate";

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final SMPAccessPoint aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_ID, aValue.getID ());
    aElement.setAttribute (ATTR_NAME, aValue.getName ());
    if (aValue.hasEndpointReference ())
      aElement.setAttribute (ATTR_ENDPOINT_REFERENCE, aValue.getEndpointReference ());
    if (aValue.hasCertificate ())
      aElement.addElementNS (sNamespaceURI, ELEMENT_CERTIFICATE).addText (aValue.getCertificate ());
    return aElement;
  }

  @NonNull
  public SMPAccessPoint convertToNative (@NonNull final IMicroElement aElement)
  {
    String sID = aElement.getAttributeValue (ATTR_ID);
    if (StringHelper.isEmpty (sID))
      sID = SMPAccessPointHelper.createUniqueAccessPointID ();
    String sName = aElement.getAttributeValue (ATTR_NAME);
    if (StringHelper.isEmpty (sName))
      sName = sID;
    final String sEndpointReference = aElement.getAttributeValue (ATTR_ENDPOINT_REFERENCE);
    final String sCertificate = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_CERTIFICATE);
    return new SMPAccessPoint (sID, sName, sEndpointReference, sCertificate);
  }
}
