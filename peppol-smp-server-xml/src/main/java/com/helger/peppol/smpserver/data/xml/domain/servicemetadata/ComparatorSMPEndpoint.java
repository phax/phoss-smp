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
package com.helger.peppol.smpserver.data.xml.domain.servicemetadata;

import com.helger.commons.compare.AbstractComparator;

public class ComparatorSMPEndpoint extends AbstractComparator <ISMPEndpoint>
{
  @Override
  protected int mainCompare (final ISMPEndpoint aElement1, final ISMPEndpoint aElement2)
  {
    int ret = aElement1.getTransportProfile ().compareTo (aElement2.getTransportProfile ());
    if (ret == 0)
      ret = aElement1.getEndpointReference ().compareTo (aElement2.getEndpointReference ());
    return ret;
  }
}
