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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;

/**
 * Test class for class {@link SMPTotpHelper}.
 *
 * @author Philip Helger
 */
public final class SMPTotpHelperTest
{
  @Test
  public void testCreateNewSecret ()
  {
    final String sSecret1 = SMPTotpHelper.createNewSecret ();
    assertNotNull (sSecret1);
    assertTrue (sSecret1.length () <= ISMPUserTotp.SECRET_MAX_LENGTH);

    // Must be random
    assertFalse (sSecret1.equals (SMPTotpHelper.createNewSecret ()));
  }

  @Test
  public void testMatchingTimeSlotInvalidParams ()
  {
    final String sSecret = SMPTotpHelper.createNewSecret ();
    assertNull (SMPTotpHelper.getMatchingTimeSlot (null, null));
    assertNull (SMPTotpHelper.getMatchingTimeSlot (sSecret, null));
    assertNull (SMPTotpHelper.getMatchingTimeSlot (sSecret, ""));
    assertNull (SMPTotpHelper.getMatchingTimeSlot (null, "123456"));
    // An invalid code must not throw
    assertNull (SMPTotpHelper.getMatchingTimeSlot (sSecret, "000000"));
    assertNull (SMPTotpHelper.getMatchingTimeSlot (sSecret, "not-a-code"));
    assertFalse (SMPTotpHelper.isValidCode (sSecret, "not-a-code"));
    // A malformed secret must not throw either
    assertNull (SMPTotpHelper.getMatchingTimeSlot ("this is not base32 !!!", "123456"));
  }

  @Test
  public void testCreateNewRecoveryCodes ()
  {
    final ICommonsList <String> aCodes = SMPTotpHelper.createNewRecoveryCodes ();
    assertNotNull (aCodes);
    assertEquals (SMPTotpHelper.RECOVERY_CODE_COUNT, aCodes.size ());

    // All codes must be unique
    final ICommonsSet <String> aUnique = new CommonsHashSet <> (aCodes);
    assertEquals (aCodes.size (), aUnique.size ());

    // Two calls must not return the same codes
    for (final String sCode : SMPTotpHelper.createNewRecoveryCodes ())
      assertFalse (aUnique.contains (sCode));
  }

  @Test
  public void testRecoveryCodeHash ()
  {
    assertNull (SMPTotpHelper.getRecoveryCodeHash (null));
    assertNull (SMPTotpHelper.getRecoveryCodeHash (""));
    assertNull (SMPTotpHelper.getRecoveryCodeHash ("  "));
    assertNull (SMPTotpHelper.getRecoveryCodeHash ("----"));

    final String sHash = SMPTotpHelper.getRecoveryCodeHash ("abcd-efgh-ijkl-mnop");
    assertNotNull (sHash);
    // SHA-512 hex
    assertEquals (128, sHash.length ());

    // Dashes, casing and surrounding whitespace must be irrelevant
    assertEquals (sHash, SMPTotpHelper.getRecoveryCodeHash ("ABCD-EFGH-IJKL-MNOP"));
    assertEquals (sHash, SMPTotpHelper.getRecoveryCodeHash ("abcdefghijklmnop"));
    assertEquals (sHash, SMPTotpHelper.getRecoveryCodeHash ("  abcd-EFGH-ijkl-MNOP  "));

    // But a different code must lead to a different hash
    assertFalse (sHash.equals (SMPTotpHelper.getRecoveryCodeHash ("abcd-efgh-ijkl-mnoq")));
  }

  @Test
  public void testGetOtpAuthURI ()
  {
    final String sSecret = SMPTotpHelper.createNewSecret ();
    final String sURI = SMPTotpHelper.getOtpAuthURI ("phoss SMP", "admin@helger.com", sSecret);
    assertNotNull (sURI);
    assertTrue (sURI.startsWith ("otpauth://totp/"));
    assertTrue (sURI.contains (sSecret));
  }
}
