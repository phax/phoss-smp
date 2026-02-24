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
 * Test class for class {@link ESGPredefinedCustomProperty}.
 *
 * @author Philip Helger
 */
public final class ESGPredefinedCustomPropertyTest
{
  @Test
  public void testBasic ()
  {
    for (final var e : ESGPredefinedCustomProperty.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getName ()));
      assertTrue (SGCustomProperty.isValidName (e.getName ()));
      assertSame (e, ESGPredefinedCustomProperty.getFromNameOrNull (e.getName ()));
    }

    assertTrue (ESGPredefinedCustomProperty.isPredefined ("hr.oib"));
    assertFalse (ESGPredefinedCustomProperty.isPredefined ("unknown.property"));
    assertFalse (ESGPredefinedCustomProperty.isPredefined (null));
  }

  @Test
  public void testHROIB ()
  {
    assertFalse (ESGPredefinedCustomProperty.HR_OIB.isValueValid (null));
    assertFalse (ESGPredefinedCustomProperty.HR_OIB.isValueValid (""));
    assertFalse (ESGPredefinedCustomProperty.HR_OIB.isValueValid ("1234567890"));
    assertFalse (ESGPredefinedCustomProperty.HR_OIB.isValueValid ("1234567890a"));

    assertTrue (ESGPredefinedCustomProperty.HR_OIB.isValueValid ("12345678901"));
  }
}
