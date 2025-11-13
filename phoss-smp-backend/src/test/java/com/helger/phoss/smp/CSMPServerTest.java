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
package com.helger.phoss.smp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.cache.regex.RegExHelper;

/**
 * Test class for class {@link CSMPServer}.
 * 
 * @author Philip Helger
 */
public final class CSMPServerTest
{
  @Test
  public void testSMPIDPattern () throws Exception
  {
    for (final String s : new String [] { "a", "aaaa", "1234", "a123", "-", "a-b", "-1" })
      assertTrue (RegExHelper.stringMatchesPattern (CSMPServer.PATTERN_SMP_ID, s));
    for (final String s : new String [] { "", "_", "_aaaa", ".1", "", "-aaaaa.", "ä", "1ä" })
      assertFalse (RegExHelper.stringMatchesPattern (CSMPServer.PATTERN_SMP_ID, s));
  }

  @Test
  public void testHROIBPattern () throws Exception
  {
    for (final String s : new String [] { "00000000000", "12345678901" })
      assertTrue (RegExHelper.stringMatchesPattern (CSMPServer.PATTERN_HR_OIB, s));
    for (final String s : new String [] { "",
                                          "1234567890",
                                          "123456789012",
                                          "1234567890a",
                                          "a2345678901",
                                          "12345-78901" })
      assertFalse (RegExHelper.stringMatchesPattern (CSMPServer.PATTERN_HR_OIB, s));
  }
}
