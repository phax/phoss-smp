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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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
  private static final String URL1 = "http://localhost/ap1";
  private static final String URL2 = "http://localhost/ap2";
  private static final String CERT1 = "cert1";
  private static final String CERT2 = "cert2";

  @Test
  public void testCreateDetached ()
  {
    final SMPAccessPoint aAP = SMPAccessPoint.createDetached (URL1, CERT1);
    assertNotNull (aAP);
    assertNotNull (aAP.getID ());
    assertEquals (URL1, aAP.getEndpointReference ());
    assertEquals (CERT1, aAP.getCertificate ());
    assertTrue (aAP.hasEndpointReference ());
    assertTrue (aAP.hasCertificate ());
    assertFalse (aAP.hasNoContent ());

    // Every detached Access Point gets its own ID
    assertNotEquals (aAP.getID (), SMPAccessPoint.createDetached (URL1, CERT1).getID ());
  }

  @Test
  public void testEmptyContent ()
  {
    final SMPAccessPoint aAP = SMPAccessPoint.createDetached (null, null);
    assertNull (aAP.getEndpointReference ());
    assertNull (aAP.getCertificate ());
    assertFalse (aAP.hasEndpointReference ());
    assertFalse (aAP.hasCertificate ());
    assertTrue (aAP.hasNoContent ());
  }

  @Test
  public void testIdentityIsUrlOnly ()
  {
    final SMPAccessPoint aAP = SMPAccessPoint.createDetached (URL1, CERT1);
    assertTrue (aAP.hasSameEndpointReference (URL1));
    assertFalse (aAP.hasSameEndpointReference (URL2));

    // null and empty are treated identically, because that is how the backends
    // store "no value"
    final SMPAccessPoint aEmpty = SMPAccessPoint.createDetached (null, null);
    assertTrue (aEmpty.hasSameEndpointReference (null));
    assertTrue (aEmpty.hasSameEndpointReference (""));
    assertEquals (SMPAccessPointHelper.createLookupKey ((String) null), SMPAccessPointHelper.createLookupKey (""));
    assertEquals (URL1, SMPAccessPointHelper.createLookupKey (URL1));
    assertEquals (SMPAccessPointHelper.createLookupKey (URL1), SMPAccessPointHelper.createLookupKey (aAP));

    // The certificate is NOT part of the identity
    assertEquals (SMPAccessPointHelper.createLookupKey (aAP),
                  SMPAccessPointHelper.createLookupKey (SMPAccessPoint.createDetached (URL1, CERT2)));
  }

  @Test
  public void testSettersMutateInPlace ()
  {
    // Setters are for the manager only - they mutate the shared object, so that
    // the change is immediately visible for all endpoints referencing it
    final SMPAccessPoint aAP = SMPAccessPoint.createDetached (URL1, CERT1);
    final String sID = aAP.getID ();

    assertEquals (EChange.UNCHANGED, aAP.setCertificate (CERT1));
    assertEquals (EChange.CHANGED, aAP.setCertificate (CERT2));
    assertEquals (CERT2, aAP.getCertificate ());
    assertEquals (EChange.UNCHANGED, aAP.setCertificate (CERT2));

    assertEquals (EChange.UNCHANGED, aAP.setEndpointReference (URL1));
    assertEquals (EChange.CHANGED, aAP.setEndpointReference (URL2));
    assertEquals (URL2, aAP.getEndpointReference ());

    // The ID never changes
    assertEquals (sID, aAP.getID ());
  }

  @Test
  public void testWithCreatesDetachedCopy ()
  {
    // "with" is for entity level edits - it must never modify the shared object
    final SMPAccessPoint aAP = SMPAccessPoint.createDetached (URL1, CERT1);

    final SMPAccessPoint aNewCert = aAP.withCertificate (CERT2);
    assertNotSame (aAP, aNewCert);
    assertNotEquals (aAP.getID (), aNewCert.getID ());
    assertEquals (URL1, aNewCert.getEndpointReference ());
    assertEquals (CERT2, aNewCert.getCertificate ());
    // Original is untouched
    assertEquals (CERT1, aAP.getCertificate ());

    final SMPAccessPoint aNewURL = aAP.withEndpointReference (URL2);
    assertNotSame (aAP, aNewURL);
    assertNotEquals (aAP.getID (), aNewURL.getID ());
    assertEquals (URL2, aNewURL.getEndpointReference ());
    assertEquals (CERT1, aNewURL.getCertificate ());
    // Original is untouched
    assertEquals (URL1, aAP.getEndpointReference ());

    // No change means no new object, so that unrelated saves do not create
    // garbage Access Points
    assertSame (aAP, aAP.withCertificate (CERT1));
    assertSame (aAP, aAP.withEndpointReference (URL1));
  }

  @Test
  public void testEqualsHashCodeByID ()
  {
    final SMPAccessPoint aAP = SMPAccessPoint.createDetached (URL1, CERT1);

    // Same ID, different content -> equal, because the ID identifies the object
    final SMPAccessPoint aSameID = new SMPAccessPoint (aAP.getID (), URL2, CERT2);
    assertEquals (aAP, aSameID);
    assertEquals (aAP.hashCode (), aSameID.hashCode ());

    // Different ID, same content -> not equal
    assertNotEquals (aAP, SMPAccessPoint.createDetached (URL1, CERT1));
  }
}
