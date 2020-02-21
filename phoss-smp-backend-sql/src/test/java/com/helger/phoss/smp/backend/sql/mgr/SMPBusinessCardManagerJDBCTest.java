/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.sql.mgr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;

/**
 * Test class for class {@link SMPBusinessCardManagerJDBC}.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardManagerJDBCTest
{
  @Test
  public void testConvertStringList ()
  {
    final ICommonsList <String> x = new CommonsArrayList <> ("a", "bcd", "http://bla.foo.com");
    final String sJson = SMPBusinessCardManagerJDBC.getStringAsJson (x).getAsJsonString ();
    final ICommonsList <String> y = SMPBusinessCardManagerJDBC.getJsonAsString (sJson);
    assertEquals (x, y);
  }
}
