/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.PagingSpec;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.phoss.smp.security.SMPCertificateHelper;

/**
 * Test class for class {@link SMPAccessPointManagerJDBC}. Requires a running PostgreSQL instance as
 * started by "unittest-db-docker-compose.yml".
 *
 * @author Philip Helger
 */
public final class SMPAccessPointManagerJDBCTest
{
  private static final String NAME1 = "ap1";
  private static final String NAME2 = "ap2";
  private static final String URL1 = "http://localhost/ap1";
  private static final String URL2 = "http://localhost/ap2";
  private static final String CERT1 = "cert1";
  private static final String CERT2 = "cert2";

  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  private ISMPAccessPointManager m_aAPMgr;

  @Before
  public void before ()
  {
    m_aAPMgr = SMPMetaManager.getAccessPointMgr ();
    _deleteAll ();
  }

  @After
  public void after ()
  {
    _deleteAll ();
  }

  private void _deleteAll ()
  {
    for (final ISMPAccessPoint aAP : m_aAPMgr.getAllAccessPoints ())
      m_aAPMgr.deleteAccessPoint (aAP.getID ());
  }

  @Test
  public void testCreateAndUniqueName ()
  {
    assertEquals (0, m_aAPMgr.getAccessPointCount ());

    final ISMPAccessPoint aAP1 = m_aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP1);
    assertEquals (NAME1, aAP1.getName ());
    assertEquals (URL1, aAP1.getEndpointReference ());
    assertEquals (CERT1, aAP1.getCertificate ());

    // The name must be unique - case insensitive
    assertNull (m_aAPMgr.createAccessPoint (NAME1, URL2, CERT2));
    assertNull (m_aAPMgr.createAccessPoint (NAME1.toUpperCase (Locale.ROOT), URL2, CERT2));
    assertEquals (1, m_aAPMgr.getAccessPointCount ());

    // Different name with the same content is allowed
    assertNotNull (m_aAPMgr.createAccessPoint (NAME2, URL1, CERT1));
    assertEquals (2, m_aAPMgr.getAccessPointCount ());
  }

  @Test
  public void testGetOfNameAndID ()
  {
    final ISMPAccessPoint aAP = m_aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);

    assertEquals (aAP.getID (), m_aAPMgr.getAccessPointOfName (NAME1).getID ());
    assertEquals (aAP.getID (), m_aAPMgr.getAccessPointOfName (NAME1.toUpperCase (Locale.ROOT)).getID ());
    assertEquals (aAP.getID (), m_aAPMgr.getAccessPointOfID (aAP.getID ()).getID ());
    assertNull (m_aAPMgr.getAccessPointOfName ("does-not-exist"));
    assertNull (m_aAPMgr.getAccessPointOfName (null));
    assertNull (m_aAPMgr.getAccessPointOfID ("does-not-exist"));
    assertNull (m_aAPMgr.getAccessPointOfID (null));
    assertTrue (m_aAPMgr.containsAccessPointWithName (NAME1));
  }

  @Test
  public void testUpdateAndDelete ()
  {
    final ISMPAccessPoint aAP = m_aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);

    assertTrue (m_aAPMgr.updateAccessPoint (aAP.getID (), NAME2, URL2, CERT2).isChanged ());
    assertNull (m_aAPMgr.getAccessPointOfName (NAME1));
    final ISMPAccessPoint aReadAP = m_aAPMgr.getAccessPointOfID (aAP.getID ());
    assertNotNull (aReadAP);
    assertEquals (NAME2, aReadAP.getName ());
    assertEquals (URL2, aReadAP.getEndpointReference ());
    assertEquals (CERT2, aReadAP.getCertificate ());

    // Unknown ID
    assertTrue (m_aAPMgr.updateAccessPoint ("does-not-exist", NAME1, URL1, CERT1).isUnchanged ());

    assertTrue (m_aAPMgr.deleteAccessPoint (aAP.getID ()).isChanged ());
    assertEquals (0, m_aAPMgr.getAccessPointCount ());
    assertTrue (m_aAPMgr.deleteAccessPoint (aAP.getID ()).isUnchanged ());

    // The name is free again
    assertNotNull (m_aAPMgr.createAccessPoint (NAME2, URL2, CERT2));
  }

  @Test
  public void testUpdateToExistingNameFails ()
  {
    final ISMPAccessPoint aAP1 = m_aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    final ISMPAccessPoint aAP2 = m_aAPMgr.createAccessPoint (NAME2, URL2, CERT2);
    assertNotNull (aAP1);
    assertNotNull (aAP2);

    assertTrue (m_aAPMgr.updateAccessPoint (aAP2.getID (), NAME1, URL2, CERT2).isUnchanged ());
    assertEquals (NAME2, m_aAPMgr.getAccessPointOfID (aAP2.getID ()).getName ());
  }

  @Test
  public void testUpdateCertificate ()
  {
    final ISMPAccessPoint aAP = m_aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);

    assertTrue (m_aAPMgr.updateAccessPointCertificate (aAP.getID (), CERT2).isChanged ());
    assertEquals (CERT2, m_aAPMgr.getAccessPointOfID (aAP.getID ()).getCertificate ());
    assertTrue (m_aAPMgr.updateAccessPointCertificate ("does-not-exist", CERT2).isUnchanged ());

    // Bulk certificate change of the Access Points
    assertEquals (1, m_aAPMgr.updateAllAccessPointCertificates (SMPCertificateHelper.getNormalizedCert (CERT2), CERT1));
    assertEquals (CERT1, m_aAPMgr.getAccessPointOfID (aAP.getID ()).getCertificate ());
  }

  @Test
  public void testPaging ()
  {
    for (int i = 1; i <= 5; ++i)
      assertNotNull (m_aAPMgr.createAccessPoint ("paging-ap-" + i, "http://localhost/paging/" + i, "cert-" + i));
    assertEquals (5, m_aAPMgr.getAccessPointCount ());

    final ICommonsList <ISMPAccessPoint> aPage1 = m_aAPMgr.getAllAccessPoints (new PagingSpec (0, 2), null);
    assertEquals (2, aPage1.size ());
    assertEquals ("paging-ap-1", aPage1.get (0).getName ());
    assertEquals ("paging-ap-2", aPage1.get (1).getName ());

    final ICommonsList <ISMPAccessPoint> aPage2 = m_aAPMgr.getAllAccessPoints (new PagingSpec (2, 2), null);
    assertEquals (2, aPage2.size ());
    assertEquals ("paging-ap-3", aPage2.get (0).getName ());
    assertEquals ("paging-ap-4", aPage2.get (1).getName ());

    final ICommonsList <ISMPAccessPoint> aPage3 = m_aAPMgr.getAllAccessPoints (new PagingSpec (4, 2), null);
    assertEquals (1, aPage3.size ());
    assertEquals ("paging-ap-5", aPage3.get (0).getName ());
  }

  @Test
  public void testSearchText ()
  {
    assertNotNull (m_aAPMgr.createAccessPoint ("search-alpha", "http://localhost/ap/a", CERT1));
    assertNotNull (m_aAPMgr.createAccessPoint ("search-beta", "http://localhost/ap/needle", CERT1));
    assertNotNull (m_aAPMgr.createAccessPoint ("needle-gamma", "http://localhost/ap/c", CERT1));
    assertNotNull (m_aAPMgr.createAccessPoint ("search-delta", "http://localhost/ap/d", CERT1));

    assertEquals (2, m_aAPMgr.getAccessPointCount ("needle"));

    final ICommonsList <ISMPAccessPoint> aAll = m_aAPMgr.getAllAccessPoints (new PagingSpec (0, 10), "needle");
    assertEquals (2, aAll.size ());
    assertEquals ("needle-gamma", aAll.get (0).getName ());
    assertEquals ("search-beta", aAll.get (1).getName ());

    final ICommonsList <ISMPAccessPoint> aPaged = m_aAPMgr.getAllAccessPoints (new PagingSpec (1, 1), "needle");
    assertEquals (1, aPaged.size ());
    assertEquals ("search-beta", aPaged.get (0).getName ());
  }
}
