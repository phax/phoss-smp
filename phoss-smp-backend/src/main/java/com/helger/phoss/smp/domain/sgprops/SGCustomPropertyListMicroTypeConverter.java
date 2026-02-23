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
package com.helger.phoss.smp.domain.sgprops;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;

/**
 * This class is internally used to convert {@link SGCustomPropertyList} from and to XML.
 *
 * @author Philip Helger
 */
public final class SGCustomPropertyListMicroTypeConverter implements IMicroTypeConverter <SGCustomPropertyList>
{
  private static final String ELEMENT_CUSTOM_PROPERTY = "customproperty";

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final SGCustomPropertyList aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    for (final SGCustomProperty aItem : aValue)
      aElement.addChild (MicroTypeConverter.convertToMicroElement (aItem, sNamespaceURI, ELEMENT_CUSTOM_PROPERTY));
    return aElement;
  }

  @NonNull
  public SGCustomPropertyList convertToNative (@NonNull final IMicroElement aElement)
  {
    final SGCustomPropertyList ret = new SGCustomPropertyList ();
    for (final IMicroElement eChild : aElement.getAllChildElements (ELEMENT_CUSTOM_PROPERTY))
    {
      final SGCustomProperty aCustomProperty = MicroTypeConverter.convertToNative (eChild, SGCustomProperty.class);
      if (aCustomProperty != null && ret.add (aCustomProperty).isUnchanged ())
        throw new IllegalStateException ("Another Custom Property with the name '" +
                                         aCustomProperty.getName () +
                                         "' is already contained in the list.");
    }
    return ret;
  }
}
