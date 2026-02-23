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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link ESGCustomPropertyType}.
 * 
 * @author Philip Helger
 */
public final class ESGCustomPropertyTypeTest
{
  @Test
  public void testBasic ()
  {
    for (final var e : ESGCustomPropertyType.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getID ()));
      assertTrue (e.getID ().length () <= ESGCustomPropertyType.ID_MAX_LEN);
      assertSame (e, ESGCustomPropertyType.getFromIDOrNull (e.getID ()));
    }

    assertTrue (ESGCustomPropertyType.PRIVATE.isPrivate ());
    assertFalse (ESGCustomPropertyType.PRIVATE.isPublic ());
    assertFalse (ESGCustomPropertyType.PUBLIC.isPrivate ());
    assertTrue (ESGCustomPropertyType.PUBLIC.isPublic ());
  }
}
