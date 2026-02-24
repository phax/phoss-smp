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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.string.StringHelper;
import com.helger.json.IJsonObject;
import com.helger.unittest.support.TestHelper;
import com.helger.xml.mock.XMLTestHelper;

/**
 * Test class for class {@link SGCustomProperty}.
 * 
 * @author Philip Helger
 */
public final class SGCustomPropertyTest
{
  private void _testJsonConversion (@NonNull final SGCustomProperty aCP)
  {
    final IJsonObject aJson = aCP.getAsJson ();
    assertNotNull (aJson);
    final SGCustomProperty aCP2 = SGCustomProperty.fromJson (aJson);
    assertNotNull (aCP2);
    TestHelper.testDefaultImplementationWithEqualContentObject (aCP, aCP2);
    assertEquals (aJson, aCP2.getAsJson ());
  }

  @Test
  public void testBasic ()
  {
    SGCustomProperty aCP = SGCustomProperty.createPrivate ("name", "value");
    assertSame (ESGCustomPropertyType.PRIVATE, aCP.getType ());
    assertTrue (aCP.isPrivate ());
    assertEquals ("name", aCP.getName ());
    assertEquals ("value", aCP.getValue ());
    assertTrue (aCP.hasValue ());
    XMLTestHelper.testMicroTypeConversion (aCP);
    _testJsonConversion (aCP);

    aCP = SGCustomProperty.createPublic ("name2", "");
    assertSame (ESGCustomPropertyType.PUBLIC, aCP.getType ());
    assertTrue (aCP.isPublic ());
    assertEquals ("name2", aCP.getName ());
    assertEquals ("", aCP.getValue ());
    assertFalse (aCP.hasValue ());
    XMLTestHelper.testMicroTypeConversion (aCP);
    _testJsonConversion (aCP);

    try
    {
      // No type
      new SGCustomProperty (null, "name", "value");
      fail ();
    }
    catch (final RuntimeException ex)
    {
      // Expected
    }

    try
    {
      // No name
      new SGCustomProperty (ESGCustomPropertyType.PRIVATE, null, "value");
      fail ();
    }
    catch (final RuntimeException ex)
    {
      // Expected
    }

    try
    {
      // No value
      new SGCustomProperty (ESGCustomPropertyType.PRIVATE, "name", null);
      fail ();
    }
    catch (final RuntimeException ex)
    {
      // Expected
    }
  }

  @Test
  public void testIsValidName ()
  {
    assertFalse (SGCustomProperty.isValidName (null));
    assertFalse (SGCustomProperty.isValidName (""));
    assertTrue (SGCustomProperty.isValidName ("a"));
    assertTrue (SGCustomProperty.isValidName ("abc"));
    assertTrue (SGCustomProperty.isValidName (StringHelper.getRepeated ('a', SGCustomProperty.NAME_MAX_LEN)));
    assertFalse (SGCustomProperty.isValidName (StringHelper.getRepeated ('a', SGCustomProperty.NAME_MAX_LEN + 1)));
    assertTrue (SGCustomProperty.isValidName ("0"));
    assertTrue (SGCustomProperty.isValidName ("."));
    assertTrue (SGCustomProperty.isValidName ("-"));
    assertTrue (SGCustomProperty.isValidName ("_"));
    assertTrue (SGCustomProperty.isValidName ("_.-0123456789abcdefghijkjlmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    assertFalse (SGCustomProperty.isValidName ("$"));
  }

  @Test
  public void testIsValidValue ()
  {
    assertFalse (SGCustomProperty.isValidValue (null));
    assertTrue (SGCustomProperty.isValidValue (""));
    assertTrue (SGCustomProperty.isValidValue ("a"));
    assertTrue (SGCustomProperty.isValidValue ("abc"));
    assertTrue (SGCustomProperty.isValidValue (StringHelper.getRepeated ('a', SGCustomProperty.VALUE_MAX_LEN)));
    assertFalse (SGCustomProperty.isValidValue (StringHelper.getRepeated ('a', SGCustomProperty.VALUE_MAX_LEN + 1)));
    assertTrue (SGCustomProperty.isValidValue ("0"));
    assertTrue (SGCustomProperty.isValidValue ("."));
    assertTrue (SGCustomProperty.isValidValue ("-"));
    assertTrue (SGCustomProperty.isValidValue ("_"));
    assertTrue (SGCustomProperty.isValidValue ("_.-0123456789abcdefghijkjlmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    assertTrue (SGCustomProperty.isValidValue ("$"));

    // Characters forbidden because they cannot be properly represented in XML attributes
    assertFalse (SGCustomProperty.isValidValue ("\n"));
    assertFalse (SGCustomProperty.isValidValue ("\r"));
    assertFalse (SGCustomProperty.isValidValue ("\0"));
    assertFalse (SGCustomProperty.isValidValue ("abc\ndef"));
    assertFalse (SGCustomProperty.isValidValue ("abc\rdef"));
    assertFalse (SGCustomProperty.isValidValue ("abc\0def"));
    assertFalse (SGCustomProperty.isValidValue ("\r\n"));

    // Tabs and other whitespace are allowed
    assertTrue (SGCustomProperty.isValidValue ("\t"));
    assertTrue (SGCustomProperty.isValidValue ("abc\tdef"));
    assertTrue (SGCustomProperty.isValidValue (" "));
    assertTrue (SGCustomProperty.isValidValue ("abc def"));
  }

  @Test
  public void testConstructorRejectsInvalidValue ()
  {
    try
    {
      new SGCustomProperty (ESGCustomPropertyType.PUBLIC, "name", "abc\ndef");
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // Expected
    }

    try
    {
      new SGCustomProperty (ESGCustomPropertyType.PUBLIC, "name", "abc\rdef");
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // Expected
    }

    try
    {
      new SGCustomProperty (ESGCustomPropertyType.PUBLIC, "name", "abc\0def");
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // Expected
    }
  }
}
