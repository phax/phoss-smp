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
package com.helger.phoss.smp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link ESMPRESTType}.
 * 
 * @author Philip Helger
 */
public class ESMPRESTTypeTest
{
  @Test
  public void testBasic ()
  {
    for (final var e : ESMPRESTType.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getID ()));
      assertTrue (StringHelper.isNotEmpty (e.getDisplayName ()));
      assertSame (e, ESMPRESTType.getFromIDOrNull (e.getID ()));
      assertSame (e, ESMPRESTType.getFromIDOrDefault (null, e));

      final String sPrefix = e.getQueryPathPrefix ();
      assertNotNull (sPrefix);
      if (sPrefix.length () > 0)
      {
        assertFalse (sPrefix.startsWith ("/"));
        assertTrue (sPrefix.endsWith ("/"));
      }
    }
  }
}
