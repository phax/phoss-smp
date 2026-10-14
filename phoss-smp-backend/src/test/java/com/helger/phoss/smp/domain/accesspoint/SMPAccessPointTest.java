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
package com.helger.phoss.smp.domain.accesspoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.state.EChange;

/**
 * Test class for class {@link SMPAccessPoint}.
 *
 * @author Philip Helger
 */
public final class SMPAccessPointTest
{
  private static final String NAME1 = "ap1";
  private static final String NAME2 = "ap2";
  private static final String URL1 = "http://localhost/ap1";
  private static final String URL2 = "http://localhost/ap2";
  private static final String CERT1 = "cert1";
  private static final String CERT2 = "cert2";

  @Test
  public void testCreateWithNewID ()
  {
    final SMPAccessPoint aAP = SMPAccessPoint.createWithNewID (NAME1, URL1, CERT1);
    assertNotNull (aAP);
    assertNotNull (aAP.getID ());
    assertEquals (NAME1, aAP.getName ());
    assertEquals (URL1, aAP.getEndpointReference ());
    assertEquals (CERT1, aAP.getCertificate ());
    assertTrue (aAP.hasEndpointReference ());
    assertTrue (aAP.hasCertificate ());

    // Every new Access Point gets its own ID
    assertNotEquals (aAP.getID (), SMPAccessPoint.createWithNewID (NAME1, URL1, CERT1).getID ());
  }

  @Test
  public void testEmptyContent ()
  {
    final SMPAccessPoint aAP = SMPAccessPoint.createWithNewID (NAME1, null, null);
    assertEquals (NAME1, aAP.getName ());
    assertNull (aAP.getEndpointReference ());
    assertNull (aAP.getCertificate ());
    assertFalse (aAP.hasEndpointReference ());
    assertFalse (aAP.hasCertificate ());
  }

  @Test
  public void testSetName ()
  {
    final SMPAccessPoint aAP = SMPAccessPoint.createWithNewID (NAME1, URL1, CERT1);
    assertEquals (EChange.UNCHANGED, aAP.setName (NAME1));
    assertEquals (EChange.CHANGED, aAP.setName (NAME2));
    assertEquals (NAME2, aAP.getName ());
  }

  @Test
  public void testSetEndpointReference ()
  {
    final SMPAccessPoint aAP = SMPAccessPoint.createWithNewID (NAME1, URL1, CERT1);
    assertEquals (EChange.UNCHANGED, aAP.setEndpointReference (URL1));
    assertEquals (EChange.CHANGED, aAP.setEndpointReference (URL2));
    assertEquals (URL2, aAP.getEndpointReference ());
    assertEquals (EChange.CHANGED, aAP.setEndpointReference (null));
    assertNull (aAP.getEndpointReference ());
    // null and empty are equivalent
    assertEquals (EChange.UNCHANGED, aAP.setEndpointReference (""));
  }

  @Test
  public void testSetCertificate ()
  {
    final SMPAccessPoint aAP = SMPAccessPoint.createWithNewID (NAME1, URL1, CERT1);
    assertEquals (EChange.UNCHANGED, aAP.setCertificate (CERT1));
    assertEquals (EChange.CHANGED, aAP.setCertificate (CERT2));
    assertEquals (CERT2, aAP.getCertificate ());
  }

  @Test
  public void testHasSameName ()
  {
    final SMPAccessPoint aAP = SMPAccessPoint.createWithNewID (NAME1, URL1, CERT1);
    assertTrue (aAP.hasSameName (NAME1));
    // Name comparison is case insensitive
    assertTrue (aAP.hasSameName (NAME1.toUpperCase (java.util.Locale.ROOT)));
    assertFalse (aAP.hasSameName (NAME2));
    assertFalse (aAP.hasSameName (null));
  }

  @Test
  public void testHelperNames ()
  {
    assertTrue (SMPAccessPointHelper.isValidName ("ap1"));
    assertTrue (SMPAccessPointHelper.isValidName ("AP-1_test.x"));
    assertFalse (SMPAccessPointHelper.isValidName (null));
    assertFalse (SMPAccessPointHelper.isValidName (""));
    assertFalse (SMPAccessPointHelper.isValidName ("-ap"));
    assertFalse (SMPAccessPointHelper.isValidName ("a b"));
    assertFalse (SMPAccessPointHelper.isValidName ("x".repeat (SMPAccessPointHelper.NAME_MAX_LENGTH + 1)));

    assertEquals (SMPAccessPointHelper.createNameLookupKey ("AP1"), SMPAccessPointHelper.createNameLookupKey (" ap1 "));
  }

  @Test
  public void testRESTReference ()
  {
    final String sRef = SMPAccessPointHelper.createRESTReference (NAME1);
    assertEquals (SMPAccessPointHelper.REST_ACCESS_POINT_PREFIX + NAME1, sRef);
    assertEquals (NAME1, SMPAccessPointHelper.getAccessPointNameFromRESTReference (sRef));
    assertNull (SMPAccessPointHelper.getAccessPointNameFromRESTReference (URL1));
    assertNull (SMPAccessPointHelper.getAccessPointNameFromRESTReference (null));
  }
}
