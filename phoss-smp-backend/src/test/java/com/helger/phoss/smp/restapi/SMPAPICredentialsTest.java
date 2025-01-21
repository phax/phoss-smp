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
package com.helger.phoss.smp.restapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.http.basicauth.BasicAuthClientCredentials;

/**
 * Test class for class {@link SMPAPICredentials}.
 *
 * @author Philip Helger
 * @since 6.1.0
 */
public final class SMPAPICredentialsTest
{
  @Test
  public void testBasicAuth ()
  {
    final BasicAuthClientCredentials aBasicAuth = new BasicAuthClientCredentials ("user", "pw");
    final SMPAPICredentials aCreds = SMPAPICredentials.createForBasicAuth (aBasicAuth);
    assertNotNull (aCreds);

    assertTrue (aCreds.hasBasicAuth ());
    assertSame (aBasicAuth, aCreds.getBasicAuth ());
    assertFalse (aCreds.hasBearerToken ());
    assertNull (aCreds.getBearerToken ());

    assertNotNull (aCreds.toString ());
  }

  @Test
  public void testBearerToken ()
  {
    final String sToken = "blafoo";
    final SMPAPICredentials aCreds = SMPAPICredentials.createForBearerToken (sToken);
    assertNotNull (aCreds);

    assertFalse (aCreds.hasBasicAuth ());
    assertNull (aCreds.getBasicAuth ());
    assertTrue (aCreds.hasBearerToken ());
    assertEquals (sToken, aCreds.getBearerToken ());

    assertNotNull (aCreds.toString ());
  }
}
