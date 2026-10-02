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
package com.helger.phoss.smp.domain.serviceinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.mock.MockSMPAccessPointManager;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * Test class for class {@link SMPEndpointMicroTypeConverter}.
 *
 * @author Philip Helger
 */
public final class SMPEndpointMicroTypeConverterTest
{
  private static final String NAME1 = "ap1";
  private static final String URL1 = "http://localhost/ap1";
  private static final String URL2 = "http://localhost/ap2";
  private static final String CERT1 = "cert1";
  private static final String CERT2 = "cert2";

  private ISMPAccessPointManager m_aAPMgr;

  @Before
  public void before ()
  {
    m_aAPMgr = new MockSMPAccessPointManager ();
  }

  private static SMPEndpoint _createDirectEndpoint (final String sURL, final String sCert)
  {
    final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
    return new SMPEndpoint ("epid",
                            "tp",
                            sURL,
                            true,
                            "minauth",
                            aStartDT,
                            aStartDT.plusYears (1),
                            sCert,
                            "sd",
                            "tc",
                            "ti",
                            null);
  }

  private static SMPEndpoint _createAPEndpoint (final ISMPAccessPoint aAP)
  {
    final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
    return new SMPEndpoint ("epid", "tp", aAP, true, "minauth", aStartDT, aStartDT.plusYears (1), "sd", "tc", "ti", null);
  }

  @Test
  public void testRoundTripDirectData ()
  {
    // The "classic" Endpoint that contains all data directly
    final SMPEndpoint aEP = _createDirectEndpoint (URL1, CERT1);
    assertFalse (aEP.hasAccessPoint ());

    final IMicroElement aElement = new SMPEndpointMicroTypeConverter ().convertToMicroElement (aEP, null, "endpoint");
    assertNotNull (aElement);
    assertNull (aElement.getAttributeValue (SMPEndpointMicroTypeConverter.ATTR_ACCESS_POINT_ID));
    assertEquals (URL1, aElement.getAttributeValue (SMPEndpointMicroTypeConverter.ATTR_ENDPOINT_REFERENCE));
    assertEquals (CERT1,
                  aElement.getFirstChildElement (SMPEndpointMicroTypeConverter.ELEMENT_CERTIFICATE).getTextContent ());

    final SMPEndpoint aReadEP = SMPEndpointMicroTypeConverter.convertToNative (aElement, m_aAPMgr);
    assertEquals (aEP.getID (), aReadEP.getID ());
    assertEquals (aEP.getTransportProfile (), aReadEP.getTransportProfile ());
    assertEquals (URL1, aReadEP.getEndpointReference ());
    assertEquals (CERT1, aReadEP.getCertificate ());
    assertFalse (aReadEP.hasAccessPoint ());
    assertEquals (aEP.isRequireBusinessLevelSignature (), aReadEP.isRequireBusinessLevelSignature ());
    assertEquals (aEP.getMinimumAuthenticationLevel (), aReadEP.getMinimumAuthenticationLevel ());
    assertEquals (aEP.getServiceDescription (), aReadEP.getServiceDescription ());
    assertEquals (aEP.getTechnicalContactUrl (), aReadEP.getTechnicalContactUrl ());
    assertEquals (aEP.getTechnicalInformationUrl (), aReadEP.getTechnicalInformationUrl ());

    // No Access Point is ever created implicitly
    assertEquals (0, m_aAPMgr.getAccessPointCount ());
  }

  @Test
  public void testRoundTripAccessPoint ()
  {
    final ISMPAccessPoint aAP = m_aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);
    final SMPEndpoint aEP = _createAPEndpoint (aAP);
    assertTrue (aEP.hasAccessPoint ());

    final IMicroElement aElement = new SMPEndpointMicroTypeConverter ().convertToMicroElement (aEP, null, "endpoint");
    assertNotNull (aElement);

    // If an Access Point is referenced, the data is not duplicated
    assertEquals (aAP.getID (), aElement.getAttributeValue (SMPEndpointMicroTypeConverter.ATTR_ACCESS_POINT_ID));
    assertNull (aElement.getAttributeValue (SMPEndpointMicroTypeConverter.ATTR_ENDPOINT_REFERENCE));
    assertNull (aElement.getFirstChildElement (SMPEndpointMicroTypeConverter.ELEMENT_CERTIFICATE));

    final SMPEndpoint aReadEP = SMPEndpointMicroTypeConverter.convertToNative (aElement, m_aAPMgr);
    assertEquals (URL1, aReadEP.getEndpointReference ());
    assertEquals (CERT1, aReadEP.getCertificate ());
    assertEquals (aAP.getID (), aReadEP.getAccessPointID ());
    assertEquals (NAME1, aReadEP.getAccessPointName ());
    assertEquals (1, m_aAPMgr.getAccessPointCount ());
  }

  @Test
  public void testReadUsesSharedAccessPointInstance ()
  {
    final ISMPAccessPoint aAP = m_aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);
    final SMPEndpoint aEP = _createAPEndpoint (aAP);
    final IMicroElement aElement = new SMPEndpointMicroTypeConverter ().convertToMicroElement (aEP, null, "endpoint");

    final SMPEndpoint aReadEP1 = SMPEndpointMicroTypeConverter.convertToNative (aElement, m_aAPMgr);
    final SMPEndpoint aReadEP2 = SMPEndpointMicroTypeConverter.convertToNative (aElement, m_aAPMgr);

    // All deserialized endpoints must reference the very same managed object,
    // otherwise an Access Point wide certificate change would not be visible
    final ISMPAccessPoint aManaged = m_aAPMgr.getAccessPointOfID (aAP.getID ());
    assertSame (aManaged, aReadEP1.getAccessPoint ());
    assertSame (aManaged, aReadEP2.getAccessPoint ());

    // Update the Access Point only - both endpoints must see it
    m_aAPMgr.updateAccessPointCertificate (aManaged.getID (), CERT2);
    assertEquals (CERT2, aReadEP1.getCertificate ());
    assertEquals (CERT2, aReadEP2.getCertificate ());

    m_aAPMgr.updateAccessPoint (aManaged.getID (), NAME1, URL2, CERT2);
    assertEquals (URL2, aReadEP1.getEndpointReference ());
    assertEquals (URL2, aReadEP2.getEndpointReference ());
  }

  @Test
  public void testReadUnresolvableAccessPointReference ()
  {
    final IMicroElement aElement = new MicroElement ("endpoint");
    aElement.setAttribute (SMPEndpointMicroTypeConverter.ATTR_ID, "epid");
    aElement.setAttribute (SMPEndpointMicroTypeConverter.ATTR_TRANSPORT_PROFILE, "tp");
    aElement.setAttribute (SMPEndpointMicroTypeConverter.ATTR_ACCESS_POINT_ID, "does-not-exist");
    aElement.setAttribute (SMPEndpointMicroTypeConverter.ATTR_ENDPOINT_REFERENCE, URL1);
    aElement.addElement (SMPEndpointMicroTypeConverter.ELEMENT_CERTIFICATE).addText (CERT1);

    // Falls back to the contained direct data - and creates no Access Point
    final SMPEndpoint aReadEP = SMPEndpointMicroTypeConverter.convertToNative (aElement, m_aAPMgr);
    assertNotNull (aReadEP);
    assertFalse (aReadEP.hasAccessPoint ());
    assertEquals (URL1, aReadEP.getEndpointReference ());
    assertEquals (CERT1, aReadEP.getCertificate ());
    assertEquals (0, m_aAPMgr.getAccessPointCount ());
  }

  @Test
  public void testMakeSelfContained ()
  {
    // The export format must be readable by SMPs that do not know Access
    // Points, so the URL and the certificate have to be inlined
    final ISMPAccessPoint aAP = m_aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);
    final SMPEndpoint aEP = _createAPEndpoint (aAP);
    final IMicroElement aElement = new SMPEndpointMicroTypeConverter ().convertToMicroElement (aEP, null, "endpoint");

    SMPEndpointMicroTypeConverter.makeSelfContained (aElement, m_aAPMgr);

    assertNull (aElement.getAttributeValue (SMPEndpointMicroTypeConverter.ATTR_ACCESS_POINT_ID));
    assertEquals (URL1, aElement.getAttributeValue (SMPEndpointMicroTypeConverter.ATTR_ENDPOINT_REFERENCE));
    assertNotNull (aElement.getFirstChildElement (SMPEndpointMicroTypeConverter.ELEMENT_CERTIFICATE));
    assertEquals (CERT1,
                  aElement.getFirstChildElement (SMPEndpointMicroTypeConverter.ELEMENT_CERTIFICATE).getTextContent ());

    final SMPEndpoint aReadEP = SMPEndpointMicroTypeConverter.convertToNative (aElement, m_aAPMgr);
    assertFalse (aReadEP.hasAccessPoint ());
    assertEquals (URL1, aReadEP.getEndpointReference ());
    assertEquals (CERT1, aReadEP.getCertificate ());
  }
}
