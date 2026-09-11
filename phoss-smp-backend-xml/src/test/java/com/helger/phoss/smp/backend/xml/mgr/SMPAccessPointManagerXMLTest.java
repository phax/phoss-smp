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

  @Test
  public void testOneCertificatePerURL ()
  {
    final ISMPAccessPointManager aAPMgr = SMPMetaManager.getAccessPointMgr ();

    final ISMPAccessPoint aAP1 = aAPMgr.getOrCreateAccessPoint (URL1, CERT1);
    assertNotNull (aAP1);
    assertEquals (URL1, aAP1.getEndpointReference ());
    assertEquals (CERT1, aAP1.getCertificate ());

    // Same URL, same certificate -> same Access Point
    assertSame (aAP1, aAPMgr.getOrCreateAccessPoint (URL1, CERT1));

    // Same URL, different certificate -> same Access Point, but updated
    // certificate, because an Access Point can only have one certificate
    final ISMPAccessPoint aAP1b = aAPMgr.getOrCreateAccessPoint (URL1, CERT2);
    assertEquals (aAP1.getID (), aAP1b.getID ());
    assertEquals (CERT2, aAPMgr.getAccessPointOfID (aAP1.getID ()).getCertificate ());

    // Different URL -> different Access Point
    final ISMPAccessPoint aAP2 = aAPMgr.getOrCreateAccessPoint (URL2, CERT2);
    assertNotNull (aAP2);
    assertEquals (URL2, aAP2.getEndpointReference ());

    // Lookup by URL only
    assertEquals (aAP1.getID (), aAPMgr.findAccessPoint (URL1).getID ());
    assertEquals (aAP2.getID (), aAPMgr.findAccessPoint (URL2).getID ());
    assertNull (aAPMgr.findAccessPoint ("http://localhost/does-not-exist"));
  }

  @Test
  public void testDeserializedEndpointUsesManagedAccessPoint ()
  {
    final ISMPAccessPointManager aAPMgr = SMPMetaManager.getAccessPointMgr ();
    final ISMPAccessPoint aAP = aAPMgr.getOrCreateAccessPoint (URL1, CERT1);
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
