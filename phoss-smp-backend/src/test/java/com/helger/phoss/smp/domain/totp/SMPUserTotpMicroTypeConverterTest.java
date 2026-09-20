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
