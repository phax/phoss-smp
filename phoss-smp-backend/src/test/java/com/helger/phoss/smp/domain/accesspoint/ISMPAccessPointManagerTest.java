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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.helger.base.state.EChange;
import com.helger.phoss.smp.mock.MockSMPAccessPointManager;

/**
 * Test class for interface {@link ISMPAccessPointManager} based on the in-memory mock
 * implementation.
 *
 * @author Philip Helger
 */
public final class ISMPAccessPointManagerTest
{
  private static final String NAME1 = "ap1";
  private static final String NAME2 = "ap2";
  private static final String URL1 = "http://localhost/ap1";
  private static final String URL2 = "http://localhost/ap2";
  private static final String CERT1 = "cert1";
  private static final String CERT2 = "cert2";

  private MockSMPAccessPointManager m_aMgr;

  @Before
  public void before ()
  {
    m_aMgr = new MockSMPAccessPointManager ();
  }

  @Test
  public void testCreate ()
  {
    assertEquals (0, m_aMgr.getAccessPointCount ());

    final ISMPAccessPoint aAP = m_aMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);
    assertEquals (NAME1, aAP.getName ());
    assertEquals (URL1, aAP.getEndpointReference ());
    assertEquals (CERT1, aAP.getCertificate ());
    assertEquals (1, m_aMgr.getAccessPointCount ());
    assertTrue (m_aMgr.containsAccessPointWithName (NAME1));
    assertFalse (m_aMgr.containsAccessPointWithName (NAME2));
  }

  @Test
  public void testNameMustBeUnique ()
  {
    assertNotNull (m_aMgr.createAccessPoint (NAME1, URL1, CERT1));
    // Same name - even with different content - is not allowed
    assertNull (m_aMgr.createAccessPoint (NAME1, URL2, CERT2));
    // Name comparison is case insensitive
    assertNull (m_aMgr.createAccessPoint (NAME1.toUpperCase (java.util.Locale.ROOT), URL2, CERT2));
    assertEquals (1, m_aMgr.getAccessPointCount ());

    // A different name is fine - even with the same content
    assertNotNull (m_aMgr.createAccessPoint (NAME2, URL1, CERT1));
    assertEquals (2, m_aMgr.getAccessPointCount ());
  }

  @Test
  public void testGetOfName ()
  {
    final ISMPAccessPoint aAP = m_aMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);
    assertEquals (aAP, m_aMgr.getAccessPointOfName (NAME1));
    assertEquals (aAP, m_aMgr.getAccessPointOfName (NAME1.toUpperCase (java.util.Locale.ROOT)));
    assertEquals (aAP, m_aMgr.getAccessPointOfID (aAP.getID ()));
    assertNull (m_aMgr.getAccessPointOfName (NAME2));
    assertNull (m_aMgr.getAccessPointOfName (null));
    assertNull (m_aMgr.getAccessPointOfID (null));
  }

  @Test
  public void testUpdate ()
  {
    final ISMPAccessPoint aAP = m_aMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);

    assertEquals (EChange.UNCHANGED, m_aMgr.updateAccessPoint (aAP.getID (), NAME1, URL1, CERT1));
    assertEquals (EChange.CHANGED, m_aMgr.updateAccessPoint (aAP.getID (), NAME2, URL2, CERT2));
    assertEquals (NAME2, aAP.getName ());
    assertEquals (URL2, aAP.getEndpointReference ());
    assertEquals (CERT2, aAP.getCertificate ());

    // The old name is free again
    assertNull (m_aMgr.getAccessPointOfName (NAME1));
    assertNotNull (m_aMgr.getAccessPointOfName (NAME2));

    // Unknown ID
    assertEquals (EChange.UNCHANGED, m_aMgr.updateAccessPoint ("bla", NAME1, URL1, CERT1));
  }

  @Test
  public void testUpdateToExistingNameFails ()
  {
    final ISMPAccessPoint aAP1 = m_aMgr.createAccessPoint (NAME1, URL1, CERT1);
    final ISMPAccessPoint aAP2 = m_aMgr.createAccessPoint (NAME2, URL2, CERT2);
    assertNotNull (aAP1);
    assertNotNull (aAP2);

    assertEquals (EChange.UNCHANGED, m_aMgr.updateAccessPoint (aAP2.getID (), NAME1, URL2, CERT2));
    assertEquals (NAME2, aAP2.getName ());
  }

  @Test
  public void testUpdateCertificate ()
  {
    final ISMPAccessPoint aAP = m_aMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);
    assertEquals (EChange.UNCHANGED, m_aMgr.updateAccessPointCertificate (aAP.getID (), CERT1));
    assertEquals (EChange.CHANGED, m_aMgr.updateAccessPointCertificate (aAP.getID (), CERT2));
    assertEquals (CERT2, aAP.getCertificate ());
    assertEquals (EChange.UNCHANGED, m_aMgr.updateAccessPointCertificate ("bla", CERT2));
  }

  @Test
  public void testDelete ()
  {
    final ISMPAccessPoint aAP = m_aMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);
    assertEquals (EChange.UNCHANGED, m_aMgr.deleteAccessPoint ("bla"));
    assertEquals (EChange.UNCHANGED, m_aMgr.deleteAccessPoint (null));
    assertEquals (EChange.CHANGED, m_aMgr.deleteAccessPoint (aAP.getID ()));
    assertEquals (0, m_aMgr.getAccessPointCount ());
    assertTrue (m_aMgr.getAllAccessPoints ().isEmpty ());
  }
}
