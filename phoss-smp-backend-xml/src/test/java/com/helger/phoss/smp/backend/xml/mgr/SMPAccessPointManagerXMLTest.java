/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.xml.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import org.jspecify.annotations.NonNull;

import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpointMicroTypeConverter;
import com.helger.phoss.smp.security.SMPCertificateHelper;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.xml.microdom.IMicroElement;

/**
 * Test class for class {@link SMPAccessPointManagerXML}.
 *
 * @author Philip Helger
 */
public final class SMPAccessPointManagerXMLTest
{
  private static final String NAME1 = "ap1";
  private static final String NAME2 = "ap2";
  private static final String URL1 = "http://localhost/ap1";
  private static final String URL2 = "http://localhost/ap2";
  private static final String CERT1 = "cert1";
  private static final String CERT2 = "cert2";

  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @NonNull
  private static SMPEndpoint _createEndpoint (final String sID, final String sURL, final String sCert)
  {
    final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
    return new SMPEndpoint (sID,
                            "tp",
                            sURL,
                            false,
                            "minauth",
                            aStartDT,
                            aStartDT.plusYears (1),
                            sCert,
                            "sd",
                            "tc",
                            "ti",
                            null);
  }

  @Before
  public void before ()
  {
    // The XML backend data survives between the tests
    final ISMPAccessPointManager aAPMgr = SMPMetaManager.getAccessPointMgr ();
    for (final ISMPAccessPoint aAP : aAPMgr.getAllAccessPoints ())
      aAPMgr.deleteAccessPoint (aAP.getID ());
  }

  @After
  public void after ()
  {
    before ();
  }

  @Test
  public void testCreateAndUniqueName ()
  {
    final ISMPAccessPointManager aAPMgr = SMPMetaManager.getAccessPointMgr ();

    final ISMPAccessPoint aAP1 = aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP1);
    assertEquals (NAME1, aAP1.getName ());
    assertEquals (URL1, aAP1.getEndpointReference ());
    assertEquals (CERT1, aAP1.getCertificate ());

    // The name must be unique - case insensitive
    assertNull (aAPMgr.createAccessPoint (NAME1, URL2, CERT2));
    assertNull (aAPMgr.createAccessPoint (NAME1.toUpperCase (Locale.ROOT), URL2, CERT2));
    assertEquals (1, aAPMgr.getAccessPointCount ());

    // Different name with the same content is allowed
    final ISMPAccessPoint aAP2 = aAPMgr.createAccessPoint (NAME2, URL1, CERT1);
    assertNotNull (aAP2);
    assertEquals (2, aAPMgr.getAccessPointCount ());

    // Lookup by name
    assertEquals (aAP1.getID (), aAPMgr.getAccessPointOfName (NAME1).getID ());
    assertEquals (aAP2.getID (), aAPMgr.getAccessPointOfName (NAME2.toUpperCase (Locale.ROOT)).getID ());
    assertNull (aAPMgr.getAccessPointOfName ("does-not-exist"));
  }

  @Test
  public void testUpdateAndDelete ()
  {
    final ISMPAccessPointManager aAPMgr = SMPMetaManager.getAccessPointMgr ();
    final ISMPAccessPoint aAP = aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);

    assertTrue (aAPMgr.updateAccessPoint (aAP.getID (), NAME2, URL2, CERT2).isChanged ());
    assertNull (aAPMgr.getAccessPointOfName (NAME1));
    assertNotNull (aAPMgr.getAccessPointOfName (NAME2));
    assertEquals (URL2, aAPMgr.getAccessPointOfID (aAP.getID ()).getEndpointReference ());

    assertTrue (aAPMgr.deleteAccessPoint (aAP.getID ()).isChanged ());
    assertEquals (0, aAPMgr.getAccessPointCount ());
    assertNull (aAPMgr.getAccessPointOfName (NAME2));

    // The name is free again
    assertNotNull (aAPMgr.createAccessPoint (NAME2, URL2, CERT2));
  }

  @Test
  public void testDeserializedEndpointUsesManagedAccessPoint ()
  {
    final ISMPAccessPointManager aAPMgr = SMPMetaManager.getAccessPointMgr ();
    final ISMPAccessPoint aAP = aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);

    final SMPEndpoint aEP = _createEndpoint ("epid", URL1, CERT1);
    aEP.setAccessPoint (aAP);

    // Serialize and deserialize, as the XML backend does upon startup
    final IMicroElement aElement = new SMPEndpointMicroTypeConverter ().convertToMicroElement (aEP, null, "endpoint");
    final SMPEndpoint aReadEP = SMPEndpointMicroTypeConverter.convertToNative (aElement, aAPMgr);
    assertNotNull (aReadEP);

    // Must be the very same instance, so that Access Point changes are
    // immediately visible for all endpoints referencing it
    assertSame (aAPMgr.getAccessPointOfID (aAP.getID ()), aReadEP.getAccessPoint ());
    assertEquals (CERT1, aReadEP.getCertificate ());

    // Update the certificate on the Access Point only - the endpoint must see
    // the new certificate without being touched itself
    assertEquals (1, aAPMgr.updateAllAccessPointCertificates (SMPCertificateHelper.getNormalizedCert (CERT1), CERT2));
    assertEquals (CERT2, aReadEP.getCertificate ());
    assertEquals (URL1, aReadEP.getEndpointReference ());
  }
}
