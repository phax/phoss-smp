/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.backend.sql.mgr;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.photon.core.mock.PhotonCoreTestRule;

/**
 * Test class for class {@link SMPBusinessCardManagerJDBC}.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardManagerJDBCTest
{
  @Rule
  public final PhotonCoreTestRule m_aRule = new PhotonCoreTestRule ();

  @Test
  public void testConvertStringList ()
  {
    final ICommonsList <String> x = new CommonsArrayList <> ("a", "bcd", "http://bla.foo.com");
    final String sJson = SMPBusinessCardManagerJDBC.getStringAsJson (x).getAsJsonString ();
    final ICommonsList <String> y = SMPBusinessCardManagerJDBC.getJsonAsString (sJson);
    assertEquals (x, y);
  }
}
