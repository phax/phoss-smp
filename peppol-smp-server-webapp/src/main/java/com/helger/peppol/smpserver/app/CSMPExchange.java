/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.app;

import javax.annotation.concurrent.Immutable;

/**
 * Constants for import/export of SMP data.
 *
 * @author Philip Helger
 */
@Immutable
public final class CSMPExchange
{
  public static final String ELEMENT_SMP_DATA = "smp-data";
  public static final String VERSION_10 = "1.0";
  public static final String ATTR_VERSION = "version";
  public static final String ELEMENT_SERVICEGROUP = "servicegroup";
  public static final String ELEMENT_SERVICEINFO = "serviceinfo";
  public static final String ELEMENT_REDIRECT = "redirect";
  public static final String ELEMENT_BUSINESSCARD = "businesscard";

  private CSMPExchange ()
  {}
}
