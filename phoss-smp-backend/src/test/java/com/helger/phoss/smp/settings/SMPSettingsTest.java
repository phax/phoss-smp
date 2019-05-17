/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.settings;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for class {@link SMPSettings}.
 *
 * @author Philip Helger
 */
public class SMPSettingsTest
{
  @Test
  public void testBasic ()
  {
    final SMPSettings aSettings = new SMPSettings ();
    assertTrue (aSettings.isPEPPOLDirectoryIntegrationRequired ());
  }
}
