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
package com.helger.phoss.smp.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for classes {@link SmpEndpointMarshaller}, {@link Bdxr1EndpointMarshaller} and
 * {@link Bdxr2EndpointMarshaller}.
 *
 * @author Philip Helger
 */
public final class EndpointMarshallerTest
{
  @Test
  public void testPeppolReadWrite ()
  {
    final String sXML = "<Endpoint xmlns=\"http://busdox.org/serviceMetadata/publishing/1.0/\" transportProfile=\"peppol-transport-as4-v2_0\">" +
                        "<EndpointReference xmlns=\"http://www.w3.org/2005/08/addressing\"><Address>http://test.smpserver/as4</Address></EndpointReference>" +
                        "<RequireBusinessLevelSignature>false</RequireBusinessLevelSignature>" +
                        "<Certificate>YmxhY2VydA==</Certificate>" +
                        "<ServiceDescription>Unit test service</ServiceDescription>" +
                        "<TechnicalContactUrl>https://github.com/phax/phoss-smp</TechnicalContactUrl>" +
                        "</Endpoint>";
    final SmpEndpointMarshaller aMarshaller = new SmpEndpointMarshaller ();
    // No global Endpoint element in the Peppol SMP XSD
    assertFalse (aMarshaller.isUseSchema ());
    final com.helger.xsds.peppol.smp1.EndpointType aEndpoint = aMarshaller.read (sXML);
    assertNotNull (aEndpoint);
    assertEquals ("peppol-transport-as4-v2_0", aEndpoint.getTransportProfile ());
    assertEquals ("Unit test service", aEndpoint.getServiceDescription ());

    assertNotNull (aMarshaller.getAsDocument (aEndpoint));
  }

  @Test
  public void testBdxr1ReadWrite ()
  {
    final String sXML = "<Endpoint xmlns=\"http://docs.oasis-open.org/bdxr/ns/SMP/2016/05\" transportProfile=\"bdxr-transport-ebms3-as4-v1p0\">" +
                        "<EndpointURI>http://test.smpserver/as4</EndpointURI>" +
                        "<RequireBusinessLevelSignature>false</RequireBusinessLevelSignature>" +
                        "<Certificate>YmxhY2VydA==</Certificate>" +
                        "<ServiceDescription>Unit test service</ServiceDescription>" +
                        "<TechnicalContactUrl>https://github.com/phax/phoss-smp</TechnicalContactUrl>" +
                        "</Endpoint>";
    final Bdxr1EndpointMarshaller aMarshaller = new Bdxr1EndpointMarshaller ();
    // No global Endpoint element in the OASIS BDXR SMP 1.0 XSD
    assertFalse (aMarshaller.isUseSchema ());
    final com.helger.xsds.bdxr.smp1.EndpointType aEndpoint = aMarshaller.read (sXML);
    assertNotNull (aEndpoint);
    assertEquals ("bdxr-transport-ebms3-as4-v1p0", aEndpoint.getTransportProfile ());
    assertEquals ("Unit test service", aEndpoint.getServiceDescription ());

    assertNotNull (aMarshaller.getAsDocument (aEndpoint));
  }

  @Test
  public void testBdxr2ReadWrite ()
  {
    final String sXML = "<Endpoint xmlns=\"http://docs.oasis-open.org/bdxr/ns/SMP/2/AggregateComponents\"" +
                        " xmlns:smb=\"http://docs.oasis-open.org/bdxr/ns/SMP/2/BasicComponents\">" +
                        "<smb:TransportProfileID>bdxr-transport-ebms3-as4-v1p0</smb:TransportProfileID>" +
                        "<smb:Description>Unit test service</smb:Description>" +
                        "<smb:AddressURI>http://test.smpserver/as4</smb:AddressURI>" +
                        "</Endpoint>";
    // The OASIS BDXR SMP 2.0 Aggregate Components XSD has a global Endpoint element,
    // so this one is read with XML Schema validation
    final Bdxr2EndpointMarshaller aMarshaller = new Bdxr2EndpointMarshaller ();
    assertTrue (aMarshaller.isUseSchema ());
    final com.helger.xsds.bdxr.smp2.ac.EndpointType aEndpoint = aMarshaller.read (sXML);
    assertNotNull (aEndpoint);
    assertEquals ("bdxr-transport-ebms3-as4-v1p0", aEndpoint.getTransportProfileIDValue ());
    assertEquals ("Unit test service", aEndpoint.getDescriptionValue ());

    assertNotNull (aMarshaller.getAsDocument (aEndpoint));
  }
}
