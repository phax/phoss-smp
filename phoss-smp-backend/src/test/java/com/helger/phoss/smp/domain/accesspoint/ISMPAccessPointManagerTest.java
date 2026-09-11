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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.helger.base.state.EChange;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.phoss.smp.mock.MockSMPAccessPointManager;
import com.helger.phoss.smp.security.SMPCertificateHelper;

/**
 * Test class for the {@link ISMPAccessPointManager} contract, especially the default methods used
 * for the bulk certificate rollover. Uses the in-memory mock implementation, because the contract is
 * identical for all backends.
 *
 * @author Philip Helger
 */
public final class ISMPAccessPointManagerTest
{
  private static final String URL1 = "http://localhost/ap1";
  private static final String URL2 = "http://localhost/ap2";
  private static final String URL3 = "http://localhost/ap3";
  private static final String CERT1 = "cert1";
  private static final String CERT2 = "cert2";

  private ISMPAccessPointManager m_aMgr;

  @Before
  public void before ()
  {
    m_aMgr = new MockSMPAccessPointManager ();
  }

  @Test
  public void testGetOrCreateDeduplicatesByURL ()
  {
    assertEquals (0, m_aMgr.getAccessPointCount ());

    final ISMPAccessPoint aAP1 = m_aMgr.getOrCreateAccessPoint (URL1, CERT1);
    assertNotNull (aAP1);
    assertEquals (1, m_aMgr.getAccessPointCount ());

    // Same URL -> same Access Point, no new object
    assertSame (aAP1, m_aMgr.getOrCreateAccessPoint (URL1, CERT1));
    assertEquals (1, m_aMgr.getAccessPointCount ());

    // Different URL -> new Access Point
    final ISMPAccessPoint aAP2 = m_aMgr.getOrCreateAccessPoint (URL2, CERT1);
    assertEquals (2, m_aMgr.getAccessPointCount ());
    assertFalse (aAP1.getID ().equals (aAP2.getID ()));
  }

  @Test
  public void testGetOrCreateOverwritesCertificate ()
  {
    // An Access Point can only have one certificate, so the last write wins -
    // and it is effective for every endpoint referencing that URL
    final ISMPAccessPoint aAP = m_aMgr.getOrCreateAccessPoint (URL1, CERT1);
    assertEquals (CERT1, aAP.getCertificate ());

    m_aMgr.getOrCreateAccessPoint (URL1, CERT2);
    assertEquals (1, m_aMgr.getAccessPointCount ());
    assertEquals (CERT2, m_aMgr.getAccessPointOfID (aAP.getID ()).getCertificate ());
  }

  @Test
  public void testFindAccessPoint ()
  {
    final ISMPAccessPoint aAP = m_aMgr.getOrCreateAccessPoint (URL1, CERT1);
    assertEquals (aAP.getID (), m_aMgr.findAccessPoint (URL1).getID ());
    assertNull (m_aMgr.findAccessPoint (URL2));
    assertNull (m_aMgr.findAccessPoint (null));

    // null and empty must be treated identically
    final ISMPAccessPoint aEmpty = m_aMgr.getOrCreateAccessPoint (null, CERT1);
    assertEquals (aEmpty.getID (), m_aMgr.findAccessPoint ("").getID ());
    assertEquals (aEmpty.getID (), m_aMgr.findAccessPoint (null).getID ());
  }

  @Test
  public void testUpdateAccessPointCertificate ()
  {
    final ISMPAccessPoint aAP = m_aMgr.getOrCreateAccessPoint (URL1, CERT1);

    assertEquals (EChange.UNCHANGED, m_aMgr.updateAccessPointCertificate (aAP.getID (), CERT1));
    assertEquals (EChange.CHANGED, m_aMgr.updateAccessPointCertificate (aAP.getID (), CERT2));
    assertEquals (CERT2, m_aMgr.getAccessPointOfID (aAP.getID ()).getCertificate ());

    // Unknown and null IDs are no-ops
    assertEquals (EChange.UNCHANGED, m_aMgr.updateAccessPointCertificate ("does-not-exist", CERT1));
    assertEquals (EChange.UNCHANGED, m_aMgr.updateAccessPointCertificate (null, CERT1));
  }

  @Test
  public void testUpdateAccessPointEndpointReference ()
  {
    final ISMPAccessPoint aAP = m_aMgr.getOrCreateAccessPoint (URL1, CERT1);

    assertEquals (EChange.UNCHANGED, m_aMgr.updateAccessPointEndpointReference (aAP.getID (), URL1));
    assertEquals (EChange.CHANGED, m_aMgr.updateAccessPointEndpointReference (aAP.getID (), URL2));
    assertEquals (URL2, m_aMgr.getAccessPointOfID (aAP.getID ()).getEndpointReference ());

    // The lookup must follow the rename
    assertNull (m_aMgr.findAccessPoint (URL1));
    assertEquals (aAP.getID (), m_aMgr.findAccessPoint (URL2).getID ());

    assertEquals (EChange.UNCHANGED, m_aMgr.updateAccessPointEndpointReference ("does-not-exist", URL1));
  }

  @Test
  public void testGetAllAccessPointIDsWithCertificate ()
  {
    final ISMPAccessPoint aAP1 = m_aMgr.getOrCreateAccessPoint (URL1, CERT1);
    final ISMPAccessPoint aAP2 = m_aMgr.getOrCreateAccessPoint (URL2, CERT1);
    m_aMgr.getOrCreateAccessPoint (URL3, CERT2);
    // No certificate at all -> must never match
    m_aMgr.getOrCreateAccessPoint ("http://localhost/ap4", null);

    final ICommonsSet <String> aIDs = m_aMgr.getAllAccessPointIDsWithCertificate (SMPCertificateHelper.getNormalizedCert (CERT1));
    assertEquals (new CommonsHashSet <> (aAP1.getID (), aAP2.getID ()), aIDs);

    assertTrue (m_aMgr.getAllAccessPointIDsWithCertificate ("not-used-anywhere").isEmpty ());
  }

  @Test
  public void testGetAllAccessPointIDsWithCertificateIsNormalized ()
  {
    // The stored form differs from the searched form only by PEM headers and
    // whitespace - it must still match
    final String sPem = "-----BEGIN CERTIFICATE-----\ncert1\n-----END CERTIFICATE-----";
    final ISMPAccessPoint aAP = m_aMgr.getOrCreateAccessPoint (URL1, sPem);

    final ICommonsSet <String> aIDs = m_aMgr.getAllAccessPointIDsWithCertificate (SMPCertificateHelper.getNormalizedCert (CERT1));
    assertEquals (new CommonsHashSet <> (aAP.getID ()), aIDs);
  }

  @Test
  public void testUpdateAllAccessPointCertificates ()
  {
    final ISMPAccessPoint aAP1 = m_aMgr.getOrCreateAccessPoint (URL1, CERT1);
    final ISMPAccessPoint aAP2 = m_aMgr.getOrCreateAccessPoint (URL2, CERT1);
    final ISMPAccessPoint aAP3 = m_aMgr.getOrCreateAccessPoint (URL3, CERT2);

    // This is the whole point of the normalization: a certificate rollover is
    // one write per Access Point, regardless of the number of endpoints
    assertEquals (2, m_aMgr.updateAllAccessPointCertificates (SMPCertificateHelper.getNormalizedCert (CERT1), CERT2));

    assertEquals (CERT2, m_aMgr.getAccessPointOfID (aAP1.getID ()).getCertificate ());
    assertEquals (CERT2, m_aMgr.getAccessPointOfID (aAP2.getID ()).getCertificate ());
    // Untouched
    assertEquals (CERT2, m_aMgr.getAccessPointOfID (aAP3.getID ()).getCertificate ());

    // Nothing left to change
    assertEquals (0, m_aMgr.updateAllAccessPointCertificates (SMPCertificateHelper.getNormalizedCert (CERT1), CERT2));

    // No Access Point is created or deleted by a rollover
    assertEquals (3, m_aMgr.getAccessPointCount ());
  }

  @Test
  public void testDeleteAccessPoint ()
  {
    final ISMPAccessPoint aAP = m_aMgr.getOrCreateAccessPoint (URL1, CERT1);
    assertEquals (EChange.UNCHANGED, m_aMgr.deleteAccessPoint (null));
    assertEquals (EChange.UNCHANGED, m_aMgr.deleteAccessPoint ("does-not-exist"));
    assertEquals (EChange.CHANGED, m_aMgr.deleteAccessPoint (aAP.getID ()));
    assertEquals (0, m_aMgr.getAccessPointCount ());
  }

  @Test
  public void testDeleteAllUnusedAccessPoints ()
  {
    final ISMPAccessPoint aAP1 = m_aMgr.getOrCreateAccessPoint (URL1, CERT1);
    m_aMgr.getOrCreateAccessPoint (URL2, CERT1);
    m_aMgr.getOrCreateAccessPoint (URL3, CERT2);

    // Only aAP1 is still referenced by an endpoint
    assertEquals (2, m_aMgr.deleteAllUnusedAccessPoints (new CommonsHashSet <> (aAP1.getID ())));
    assertEquals (1, m_aMgr.getAccessPointCount ());
    assertNotNull (m_aMgr.getAccessPointOfID (aAP1.getID ()));

    // Nothing left to collect
    assertEquals (0, m_aMgr.deleteAllUnusedAccessPoints (new CommonsHashSet <> (aAP1.getID ())));
  }
}
