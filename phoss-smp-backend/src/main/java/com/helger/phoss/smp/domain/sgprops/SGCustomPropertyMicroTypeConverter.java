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
package com.helger.phoss.smp.domain.sgprops;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

/**
 * This class is internally used to convert {@link SGCustomProperty} from and to XML.
 *
 * @author Philip Helger
 */
public final class SGCustomPropertyMicroTypeConverter implements IMicroTypeConverter <SGCustomProperty>
{
  private static final MicroQName ATTR_TYPE = new MicroQName ("type");
  private static final MicroQName ATTR_NAME = new MicroQName ("name");
  private static final MicroQName ATTR_VALUE = new MicroQName ("value");

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final SGCustomProperty aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_TYPE, aValue.getType ().getID ());
    aElement.setAttribute (ATTR_NAME, aValue.getName ());
    aElement.setAttribute (ATTR_VALUE, aValue.getValue ());
    return aElement;
  }

  @NonNull
  public SGCustomProperty convertToNative (@NonNull final IMicroElement aElement)
  {
    final String sType = aElement.getAttributeValue (ATTR_TYPE);
    final ESGCustomPropertyType eType = ESGCustomPropertyType.getFromIDOrNull (sType);
    if (eType == null)
      throw new IllegalStateException ("Failed to resolve SG custom property type '" + sType + "'");

    final String sName = aElement.getAttributeValue (ATTR_NAME);
    final String sValue = aElement.getAttributeValue (ATTR_VALUE);
    return new SGCustomProperty (eType, sName, sValue);
  }
}
