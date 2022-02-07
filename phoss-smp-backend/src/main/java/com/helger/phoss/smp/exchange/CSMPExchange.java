/*
 * Copyright (C) 2015-2022 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.exchange;

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
