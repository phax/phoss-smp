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
package com.helger.phoss.smp.domain.sgprops;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.json.IJsonArray;
import com.helger.unittest.support.TestHelper;
import com.helger.xml.mock.XMLTestHelper;

/**
 * Test class for class {@link SGCustomPropertyList}.
 * 
 * @author Philip Helger
 */
public final class SGCustomPropertyListTest
{
  @Test
  public void testBasic ()
  {
    final SGCustomPropertyList aList = new SGCustomPropertyList ();
    assertTrue (aList.isEmpty ());
    assertEquals (0, aList.size ());

    assertTrue (aList.add (SGCustomProperty.createPrivate ("key1", "value1")).isChanged ());
    assertFalse (aList.isEmpty ());
    assertEquals (1, aList.size ());

    assertTrue (aList.add (SGCustomProperty.createPublic ("key2", "value2")).isChanged ());
    assertFalse (aList.isEmpty ());
    assertEquals (2, aList.size ());

    assertFalse (aList.add (SGCustomProperty.createPublic ("key2", "value2a")).isChanged ());
    assertFalse (aList.isEmpty ());
    assertEquals (2, aList.size ());

    XMLTestHelper.testMicroTypeConversion (aList);

    final IJsonArray aJson = aList.getAsJson ();
    assertNotNull (aJson);
    final SGCustomPropertyList aList2 = SGCustomPropertyList.fromJson (aJson);
    assertNotNull (aList2);
    TestHelper.testDefaultImplementationWithEqualContentObject (aList, aList2);
  }
}
