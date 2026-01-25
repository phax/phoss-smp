package com.helger.phoss.smp.domain.sgprops;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.helger.json.IJsonArray;
import com.helger.unittest.support.TestHelper;

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
    aList.add (SGCustomProperty.createPrivate ("key1", "value1"));
    aList.add (SGCustomProperty.createPublic ("key2", "value2"));
    final IJsonArray aJson = aList.getAsJson ();
    assertNotNull (aJson);
    final SGCustomPropertyList aList2 = SGCustomPropertyList.fromJson (aJson);
    assertNotNull (aList2);
    TestHelper.testDefaultImplementationWithEqualContentObject (aList, aList2);
  }
}
