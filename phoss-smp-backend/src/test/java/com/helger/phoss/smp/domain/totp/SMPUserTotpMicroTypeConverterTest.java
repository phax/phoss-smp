/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.domain.totp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;

/**
 * Test class for class {@link SMPUserTotpMicroTypeConverter}.
 *
 * @author Philip Helger
 */
public final class SMPUserTotpMicroTypeConverterTest
{
  private static void _testRoundTrip (final SMPUserTotp aSrc)
  {
    final IMicroElement aElement = MicroTypeConverter.convertToMicroElement (aSrc, "totp");
    assertNotNull (aElement);

    final SMPUserTotp aDst = MicroTypeConverter.convertToNative (aElement, SMPUserTotp.class);
    assertNotNull (aDst);

    assertEquals (aSrc.getID (), aDst.getID ());
    assertEquals (aSrc.getSecret (), aDst.getSecret ());
    assertTrue (aSrc.isEnabled () == aDst.isEnabled ());
    assertEquals (aSrc.getRegistrationDateTime (), aDst.getRegistrationDateTime ());
    assertEquals (aSrc.getLastUsedTimeSlot (), aDst.getLastUsedTimeSlot ());
    assertEquals (aSrc.getAllRecoveryCodeHashes (), aDst.getAllRecoveryCodeHashes ());
  }

  @Test
  public void testMinimal ()
  {
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();
    final SMPUserTotp aSrc = new SMPUserTotp ("user1", "ABCDEFGHIJKLMNOP", false, aNow, null, null);
    assertNull (aSrc.getLastUsedTimeSlot ());
    assertTrue (aSrc.getAllRecoveryCodeHashes ().isEmpty ());
    _testRoundTrip (aSrc);
  }

  @Test
  public void testAllFields ()
  {
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();
    final ICommonsList <String> aHashes = new CommonsArrayList <> ("hash1", "hash2", "hash3");
    final SMPUserTotp aSrc = new SMPUserTotp ("user2",
                                              "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567",
                                              true,
                                              aNow,
                                              Long.valueOf (123456789L),
                                              aHashes);
    assertEquals (3, aSrc.getRecoveryCodeCount ());
    _testRoundTrip (aSrc);
  }
}
