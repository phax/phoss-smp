/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.apache.hc.core5.http.HttpEntity;
import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.io.resource.FileSystemResource;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.PeppolDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.mock.MockHttpClient;
import com.helger.phoss.smp.mock.MockHttpResponse;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceGroupType;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceMetadataType;
import com.helger.smpclient.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.xsds.peppol.smp1.EndpointType;
import com.helger.xsds.peppol.smp1.ProcessListType;
import com.helger.xsds.peppol.smp1.ProcessType;
import com.helger.xsds.peppol.smp1.ServiceEndpointList;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceInformationType;
import com.helger.xsds.peppol.smp1.ServiceMetadataReferenceCollectionType;
import com.helger.xsds.peppol.smp1.ServiceMetadataType;

/**
 * Test class for the optional HTTP caching with <code>Last-Modified</code> and
 * <code>If-Modified-Since</code> of {@link SMPRestHttpCacheHelper}. Only the Service Group is
 * queried, because the Service Metadata response requires a signing certificate.
 *
 * @author Philip Helger
 */
public final class RestHttpCacheLastModifiedTest
{
  private static final BasicAuthClientCredentials CREDENTIALS = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                                CSecurity.USER_ADMINISTRATOR_PASSWORD);
  private static final String LAST_MODIFIED = "Last-Modified";
  private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
  private static final String IF_NONE_MATCH = "If-None-Match";
  // The "Last-Modified" header is only sent, if the last modification lies in a past second
  private static final Duration WAIT_FOR_NEXT_SECOND = Duration.ofMillis (1100);

  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (new FileSystemResource ("src/test/resources/test-smp-server-xml-peppol-lastmodified.properties"));

  private MockHttpClient m_aClient;

  @Before
  public void before ()
  {
    m_aClient = new MockHttpClient (m_aRule.getFullURL ());
  }

  @After
  public void after ()
  {
    m_aClient.close ();
  }

  @NonNull
  private static HttpEntity _xmlEntity (@NonNull final ServiceGroupType aSG)
  {
    return MockHttpClient.createXMLEntity (new SMPMarshallerServiceGroupType (), aSG);
  }

  @NonNull
  private static HttpEntity _xmlEntity (@NonNull final ServiceMetadataType aSM)
  {
    return MockHttpClient.createXMLEntity (new SMPMarshallerServiceMetadataType (), aSM);
  }

  @NonNull
  private static ServiceMetadataType _createServiceMetadata (@NonNull final IParticipantIdentifier aPI,
                                                             @NonNull final PeppolDocumentTypeIdentifier aDT)
  {
    final ServiceMetadataType aSM = new ServiceMetadataType ();
    final ServiceInformationType aSI = new ServiceInformationType ();
    aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSI.setDocumentIdentifier (aDT);
    final ProcessListType aPL = new ProcessListType ();
    final ProcessType aProcess = new ProcessType ();
    aProcess.setProcessIdentifier (EPredefinedProcessIdentifier.BIS3_BILLING.getAsProcessIdentifier ());
    final ServiceEndpointList aSEL = new ServiceEndpointList ();
    final EndpointType aEndpoint = new EndpointType ();
    aEndpoint.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference ("http://test.smpserver/as4"));
    aEndpoint.setRequireBusinessLevelSignature (false);
    aEndpoint.setCertificate ("blacert");
    aEndpoint.setServiceDescription ("Unit test service");
    aEndpoint.setTechnicalContactUrl ("https://github.com/phax/phoss-smp");
    aEndpoint.setTransportProfile (ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2.getID ());
    aSEL.addEndpoint (aEndpoint);
    aProcess.setServiceEndpointList (aSEL);
    aPL.addProcess (aProcess);
    aSI.setProcessList (aPL);
    aSM.setServiceInformation (aSI);
    return aSM;
  }

  @NonNull
  private String _getLastModified (@NonNull final String sPath)
  {
    final MockHttpResponse aResponse = m_aClient.get (sPath);
    assertEquals (200, aResponse.getStatusCode ());
    assertTrue (aResponse.hasBody ());
    // No ETag is sent, because it is disabled
    assertNull (aResponse.getFirstHeaderValue ("ETag"));
    // Must be revalidated
    final String sCacheControl = aResponse.getFirstHeaderValue ("Cache-Control");
    assertNotNull (sCacheControl);
    assertTrue (sCacheControl, sCacheControl.contains ("no-cache"));
    final String sLastModified = aResponse.getFirstHeaderValue (LAST_MODIFIED);
    assertNotNull (sLastModified);
    assertTrue (sLastModified, sLastModified.endsWith (" GMT"));
    // The configured time zone is not UTC, but the HTTP date must be the real UTC time
    final Instant aLastModified = ZonedDateTime.parse (sLastModified, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant ();
    assertTrue (sLastModified, Math.abs (Duration.between (aLastModified, Instant.now ()).toSeconds ()) < 60);
    return sLastModified;
  }

  private void _assertHead (@NonNull final String sPath, @NonNull final String sExpectedLastModified)
  {
    final MockHttpResponse aResponse = m_aClient.head (sPath, Map.of ());
    assertEquals (200, aResponse.getStatusCode ());
    assertFalse (aResponse.hasBody ());
    // The ETag is never sent for HEAD
    assertNull (aResponse.getFirstHeaderValue ("ETag"));
    final String sContentType = aResponse.getFirstHeaderValue ("Content-Type");
    assertNotNull (sContentType);
    assertTrue (sContentType, sContentType.startsWith ("text/xml"));
    final String sCacheControl = aResponse.getFirstHeaderValue ("Cache-Control");
    assertNotNull (sCacheControl);
    assertTrue (sCacheControl, sCacheControl.contains ("no-cache"));
    assertEquals (sExpectedLastModified, aResponse.getFirstHeaderValue (LAST_MODIFIED));

    // Conditional HEAD
    final MockHttpResponse aResponse304 = m_aClient.head (sPath, Map.of (IF_MODIFIED_SINCE, sExpectedLastModified));
    assertEquals (304, aResponse304.getStatusCode ());
    assertEquals (sExpectedLastModified, aResponse304.getFirstHeaderValue (LAST_MODIFIED));
  }

  @Test
  public void testLastModified ()
  {
    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:lastmodified");
    final String sPI = aPI.getURIPercentEncoded ();
    final PeppolDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();
    final String sDT = aDT.getURIPercentEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    // Unknown participant: error responses keep the "no caching" headers
    MockHttpResponse aResponse = m_aClient.get (sPI);
    assertEquals (404, aResponse.getStatusCode ());
    assertNull (aResponse.getFirstHeaderValue (LAST_MODIFIED));
    aResponse = m_aClient.head (sPI, Map.of ());
    assertEquals (404, aResponse.getStatusCode ());
    assertNull (aResponse.getFirstHeaderValue (LAST_MODIFIED));
    assertEquals (404, m_aClient.head (sPI + "/services/" + sDT, Map.of ()).getStatusCode ());

    try
    {
      assertEquals (200, m_aClient.put (sPI, CREDENTIALS, _xmlEntity (aSG)).getStatusCode ());

      // A change in the current second never results in a "Last-Modified" header
      aResponse = m_aClient.get (sPI);
      assertEquals (200, aResponse.getStatusCode ());
      assertNull (aResponse.getFirstHeaderValue (LAST_MODIFIED));

      ThreadHelper.sleep (WAIT_FOR_NEXT_SECOND);
      final String sLM1 = _getLastModified (sPI);
      _assertHead (sPI, sLM1);

      // Unknown document type
      assertEquals (404, m_aClient.head (sPI + "/services/" + sDT, Map.of ()).getStatusCode ());

      // Not modified since
      aResponse = m_aClient.get (sPI, Map.of (IF_MODIFIED_SINCE, sLM1));
      assertEquals (304, aResponse.getStatusCode ());
      assertFalse (aResponse.hasBody ());
      assertEquals (sLM1, aResponse.getFirstHeaderValue (LAST_MODIFIED));

      // A later date is also not modified
      aResponse = m_aClient.get (sPI, Map.of (IF_MODIFIED_SINCE, "Fri, 31 Dec 2100 23:59:59 GMT"));
      assertEquals (304, aResponse.getStatusCode ());

      // An earlier date was modified
      aResponse = m_aClient.get (sPI, Map.of (IF_MODIFIED_SINCE, "Tue, 01 Dec 2020 19:14:44 GMT"));
      assertEquals (200, aResponse.getStatusCode ());
      assertTrue (aResponse.hasBody ());

      // An invalid date is ignored
      aResponse = m_aClient.get (sPI, Map.of (IF_MODIFIED_SINCE, "gibberish"));
      assertEquals (200, aResponse.getStatusCode ());

      // If-Modified-Since is ignored if If-None-Match is present
      aResponse = m_aClient.get (sPI, Map.of (IF_MODIFIED_SINCE, sLM1, IF_NONE_MATCH, "\"0000\""));
      assertEquals (200, aResponse.getStatusCode ());

      // Add a Service Information
      assertEquals (200, m_aClient.put (sPI + "/services/" + sDT, CREDENTIALS, _xmlEntity (_createServiceMetadata (aPI, aDT)))
                                  .getStatusCode ());
      ThreadHelper.sleep (WAIT_FOR_NEXT_SECOND);

      // The Service Group response changed
      aResponse = m_aClient.get (sPI, Map.of (IF_MODIFIED_SINCE, sLM1));
      assertEquals (200, aResponse.getStatusCode ());
      assertTrue (aResponse.getBodyAsString ().contains ("services/"));
      final String sLM2 = aResponse.getFirstHeaderValue (LAST_MODIFIED);
      assertNotNull (sLM2);
      assertNotEquals (sLM1, sLM2);
      assertEquals (304, m_aClient.get (sPI, Map.of (IF_MODIFIED_SINCE, sLM2)).getStatusCode ());
      _assertHead (sPI, sLM2);
      // Service Metadata - HEAD works without a signing certificate
      _assertHead (sPI + "/services/" + sDT, sLM2);

      // Delete the Service Information
      assertEquals (200, m_aClient.delete (sPI + "/services/" + sDT, CREDENTIALS).getStatusCode ());
      ThreadHelper.sleep (WAIT_FOR_NEXT_SECOND);

      // The Service Group response changed again
      aResponse = m_aClient.get (sPI, Map.of (IF_MODIFIED_SINCE, sLM2));
      assertEquals (200, aResponse.getStatusCode ());
      assertFalse (aResponse.getBodyAsString ().contains ("services/"));
      final String sLM3 = aResponse.getFirstHeaderValue (LAST_MODIFIED);
      assertNotNull (sLM3);
      assertNotEquals (sLM2, sLM3);
      _assertHead (sPI, sLM3);
      assertEquals (404, m_aClient.head (sPI + "/services/" + sDT, Map.of ()).getStatusCode ());
    }
    finally
    {
      m_aClient.delete (sPI + "/services/" + sDT, CREDENTIALS);
      m_aClient.delete (sPI, CREDENTIALS);
    }
  }
}
