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
package com.helger.phoss.smp.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.base.string.StringHelper;
import com.helger.photon.app.mock.PhotonAppWebTestRule;
import com.helger.photon.security.login.LoginThrottlePerIP;

/**
 * Test class for the throttling of the second authentication factor in {@link SecureLoginFilter}.
 *
 * @author Philip Helger
 */
public final class SecureLoginFilterTotpThrottleTest
{
  @Rule
  public final TestRule m_aRule = new PhotonAppWebTestRule ();

  private static final String USER_ID = "user-4711";
  private static final String IP = "203.0.113.7";

  @Test
  public void testThrottleKeyCannotCollideWithTheFirstFactorKey ()
  {
    assertTrue (StringHelper.isNotEmpty (SecureLoginFilter.FAILED_TOTP_THROTTLE_KEY_SUFFIX));
    // Even a user ID that looks like an IP address stays in its own namespace
    assertFalse (IP.equals (IP + SecureLoginFilter.FAILED_TOTP_THROTTLE_KEY_SUFFIX));
  }

  @Test
  public void testFirstFactorLoginDoesNotResetTheSecondFactorThrottle ()
  {
    final String sTotpKey = USER_ID + SecureLoginFilter.FAILED_TOTP_THROTTLE_KEY_SUFFIX;
    final LoginThrottlePerIP aThrottle = LoginThrottlePerIP.getInstance ();

    // Three failed second factor validations of that user
    aThrottle.onFailedLogin (sTotpKey);
    aThrottle.onFailedLogin (sTotpKey);
    assertEquals (3, aThrottle.onFailedLogin (sTotpKey));

    // An attacker that brute forces the second factor knows the password by definition, so they
    // can login again at will - which clears the throttle key of the FIRST factor
    aThrottle.onSuccessfulLogin (IP);

    // The second factor throttle must be unaffected by that
    assertEquals (3, aThrottle.getFailedLoginCount (sTotpKey));
    assertEquals (4, aThrottle.onFailedLogin (sTotpKey));

    // Only a successful second factor of that user clears it
    aThrottle.onSuccessfulLogin (sTotpKey);
    assertEquals (0, aThrottle.getFailedLoginCount (sTotpKey));
  }

  @Test
  public void testThrottleIsIndependentOfTheSourceIPAddress ()
  {
    final String sTotpKey = USER_ID + SecureLoginFilter.FAILED_TOTP_THROTTLE_KEY_SUFFIX;
    final LoginThrottlePerIP aThrottle = LoginThrottlePerIP.getInstance ();

    // The key does not contain the IP address, so rotating it does not help an attacker
    assertEquals (1, aThrottle.onFailedLogin (sTotpKey));
    assertEquals (2, aThrottle.onFailedLogin (sTotpKey));
    aThrottle.onSuccessfulLogin ("198.51.100.9");
    aThrottle.onSuccessfulLogin ("192.0.2.44");
    assertEquals (3, aThrottle.onFailedLogin (sTotpKey));
  }
}
