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
package com.helger.peppol.smpserver.data.xml.domain.redirect;

import javax.annotation.Nonnull;

import com.helger.commons.compare.AbstractComparator;
import com.helger.peppol.identifier.IdentifierHelper;

public class ComparatorSMPRedirect extends AbstractComparator <ISMPRedirect>
{
  @Override
  protected int mainCompare (@Nonnull final ISMPRedirect aElement1, @Nonnull final ISMPRedirect aElement2)
  {
    int ret = aElement1.getServiceGroupID ().compareTo (aElement2.getServiceGroupID ());
    if (ret == 0)
      ret = IdentifierHelper.compareDocumentTypeIdentifiers (aElement1.getDocumentTypeIdentifier (),
                                                             aElement2.getDocumentTypeIdentifier ());
    return ret;
  }
}
