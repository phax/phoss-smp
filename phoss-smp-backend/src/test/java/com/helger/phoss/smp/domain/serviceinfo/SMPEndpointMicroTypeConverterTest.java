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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

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
@SuppressWarnings ("deprecation")
public final class SMPEndpointMicroTypeConverterTest
{
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

  private SMPEndpoint _createEndpoint (final String sURL, final String sCert)
  {
    final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
    final SMPEndpoint ret = new SMPEndpoint ("epid",
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
    // Resolve against the manager, as every backend does before saving
    SMPEndpointHelper.resolveAccessPoint (m_aAPMgr, ret);
    return ret;
  }

  @Test
  public void testRoundTrip ()
  {
    final SMPEndpoint aEP = _createEndpoint (URL1, CERT1);

    final IMicroElement aElement = new SMPEndpointMicroTypeConverter ().convertToMicroElement (aEP, null, "endpoint");
    assertNotNull (aElement);

    // The URL and the certificate are no longer stored on the endpoint itself
    assertEquals (aEP.getAccessPointID (), aElement.getAttributeValue (SMPEndpointMicroTypeConverter.ATTR_ACCESS_POINT_ID));
    assertNull (aElement.getAttributeValue (SMPEndpointMicroTypeConverter.ATTR_ENDPOINT_REFERENCE));
    assertNull (aElement.getFirstChildElement (SMPEndpointMicroTypeConverter.ELEMENT_CERTIFICATE));

    final SMPEndpoint aReadEP = SMPEndpointMicroTypeConverter.convertToNative (aElement, m_aAPMgr);
    assertEquals (aEP.getID (), aReadEP.getID ());
    assertEquals (aEP.getTransportProfile (), aReadEP.getTransportProfile ());
    assertEquals (URL1, aReadEP.getEndpointReference ());
    assertEquals (CERT1, aReadEP.getCertificate ());
    assertEquals (aEP.isRequireBusinessLevelSignature (), aReadEP.isRequireBusinessLevelSignature ());
    assertEquals (aEP.getMinimumAuthenticationLevel (), aReadEP.getMinimumAuthenticationLevel ());
    assertEquals (aEP.getServiceDescription (), aReadEP.getServiceDescription ());
    assertEquals (aEP.getTechnicalContactUrl (), aReadEP.getTechnicalContactUrl ());
    assertEquals (aEP.getTechnicalInformationUrl (), aReadEP.getTechnicalInformationUrl ());

    // No additional Access Point was created while reading
    assertEquals (1, m_aAPMgr.getAccessPointCount ());
  }

  @Test
  public void testReadUsesSharedAccessPointInstance ()
  {
    final SMPEndpoint aEP = _createEndpoint (URL1, CERT1);
    final IMicroElement aElement = new SMPEndpointMicroTypeConverter ().convertToMicroElement (aEP, null, "endpoint");

    final SMPEndpoint aReadEP1 = SMPEndpointMicroTypeConverter.convertToNative (aElement, m_aAPMgr);
    final SMPEndpoint aReadEP2 = SMPEndpointMicroTypeConverter.convertToNative (aElement, m_aAPMgr);

    // All deserialized endpoints must reference the very same managed object,
    // otherwise an Access Point wide certificate change would not be visible
    final ISMPAccessPoint aManaged = m_aAPMgr.getAccessPointOfID (aEP.getAccessPointID ());
    assertSame (aManaged, aReadEP1.getAccessPoint ());
    assertSame (aManaged, aReadEP2.getAccessPoint ());

    // Update the Access Point only - both endpoints must see it
    m_aAPMgr.updateAccessPointCertificate (aManaged.getID (), CERT2);
    assertEquals (CERT2, aReadEP1.getCertificate ());
    assertEquals (CERT2, aReadEP2.getCertificate ());

    m_aAPMgr.updateAccessPointEndpointReference (aManaged.getID (), URL2);
    assertEquals (URL2, aReadEP1.getEndpointReference ());
    assertEquals (URL2, aReadEP2.getEndpointReference ());
  }

  @Test
  public void testSoftMigrationOfLegacyInlineData ()
  {
    // Data written before v8.4.4 had the URL and the certificate inline and no
    // Access Point reference at all
    final IMicroElement aElement = new MicroElement ("endpoint");
    aElement.setAttribute (SMPEndpointMicroTypeConverter.ATTR_ID, "epid");
    aElement.setAttribute (SMPEndpointMicroTypeConverter.ATTR_TRANSPORT_PROFILE, "tp");
    aElement.setAttribute (SMPEndpointMicroTypeConverter.ATTR_ENDPOINT_REFERENCE, URL1);
    aElement.addElement (SMPEndpointMicroTypeConverter.ELEMENT_CERTIFICATE).addText (CERT1);

    final SMPEndpoint aReadEP = SMPEndpointMicroTypeConverter.convertToNative (aElement, m_aAPMgr);
    assertNotNull (aReadEP);
    assertEquals ("epid", aReadEP.getID ());
    assertEquals (URL1, aReadEP.getEndpointReference ());
    assertEquals (CERT1, aReadEP.getCertificate ());

    // The Access Point was created on the fly
    assertEquals (1, m_aAPMgr.getAccessPointCount ());
    assertEquals (aReadEP.getAccessPointID (), m_aAPMgr.findAccessPoint (URL1).getID ());

    // A second legacy endpoint with the same URL must reuse it
    final IMicroElement aElement2 = new MicroElement ("endpoint");
    aElement2.setAttribute (SMPEndpointMicroTypeConverter.ATTR_ID, "epid2");
    aElement2.setAttribute (SMPEndpointMicroTypeConverter.ATTR_TRANSPORT_PROFILE, "tp2");
    aElement2.setAttribute (SMPEndpointMicroTypeConverter.ATTR_ENDPOINT_REFERENCE, URL1);
    aElement2.addElement (SMPEndpointMicroTypeConverter.ELEMENT_CERTIFICATE).addText (CERT1);

    final SMPEndpoint aReadEP2 = SMPEndpointMicroTypeConverter.convertToNative (aElement2, m_aAPMgr);
    assertEquals (1, m_aAPMgr.getAccessPointCount ());
    assertEquals (aReadEP.getAccessPointID (), aReadEP2.getAccessPointID ());
    assertSame (aReadEP.getAccessPoint (), aReadEP2.getAccessPoint ());
  }

  @Test
  public void testMakeSelfContained ()
  {
    // The export format must remain backwards compatible, so the URL and the
    // certificate have to be inlined again
    final SMPEndpoint aEP = _createEndpoint (URL1, CERT1);
    final IMicroElement aElement = new SMPEndpointMicroTypeConverter ().convertToMicroElement (aEP, null, "endpoint");

    SMPEndpointMicroTypeConverter.makeSelfContained (aElement, m_aAPMgr);

    assertNull (aElement.getAttributeValue (SMPEndpointMicroTypeConverter.ATTR_ACCESS_POINT_ID));
    assertEquals (URL1, aElement.getAttributeValue (SMPEndpointMicroTypeConverter.ATTR_ENDPOINT_REFERENCE));
    assertNotNull (aElement.getFirstChildElement (SMPEndpointMicroTypeConverter.ELEMENT_CERTIFICATE));
    assertEquals (CERT1,
                  aElement.getFirstChildElement (SMPEndpointMicroTypeConverter.ELEMENT_CERTIFICATE).getTextContent ());

    // Such an element must be readable by an SMP that does not know Access
    // Points at all, and by this one too
    final SMPEndpoint aReadEP = SMPEndpointMicroTypeConverter.convertToNative (aElement, m_aAPMgr);
    assertEquals (URL1, aReadEP.getEndpointReference ());
    assertEquals (CERT1, aReadEP.getCertificate ());
  }
}
