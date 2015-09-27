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
package com.helger.peppol.smpserver.domain.serviceinfo;

import com.helger.commons.compare.AbstractComparator;
import com.helger.peppol.identifier.IdentifierHelper;

public class ComparatorSMPProcess extends AbstractComparator <ISMPProcess>
{
  @Override
  protected int mainCompare (final ISMPProcess aElement1, final ISMPProcess aElement2)
  {
    return IdentifierHelper.compareProcessIdentifiers (aElement1.getProcessIdentifier (),
                                                       aElement2.getProcessIdentifier ());
  }
}
