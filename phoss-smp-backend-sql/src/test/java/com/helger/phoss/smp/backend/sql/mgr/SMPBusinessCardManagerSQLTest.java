package com.helger.phoss.smp.backend.sql.mgr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;

/**
 * Test class for class {@link SMPBusinessCardManagerSQL}.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardManagerSQLTest
{
  @Test
  public void testConvertStringList ()
  {
    final ICommonsList <String> x = new CommonsArrayList <> ("a", "bcd", "http://bla.foo.com");
    final String sJson = SMPBusinessCardManagerSQL.getStringAsJson (x).getAsJsonString ();
    final ICommonsList <String> y = SMPBusinessCardManagerSQL.getJsonAsString (sJson);
    assertEquals (x, y);
  }
}
