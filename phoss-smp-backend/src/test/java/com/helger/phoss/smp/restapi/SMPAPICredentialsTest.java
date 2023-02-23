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
