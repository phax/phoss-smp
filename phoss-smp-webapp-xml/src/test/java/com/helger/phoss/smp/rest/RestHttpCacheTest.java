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

import java.util.Map;

import org.apache.hc.core5.http.HttpEntity;
import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.helger.annotation.misc.DevelopersNote;
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
 * Test class for the optional HTTP caching headers (Cache-Control, ETag) of
 * {@link SMPRestHttpCacheHelper}
 *
 * @author Philip Helger
 */
@Ignore
@DevelopersNote ("This test can only be executed locally, if a proper signing certificate is configured for local execution (e.g. with a private-application.properties)")
public final class RestHttpCacheTest
{
  private static final BasicAuthClientCredentials CREDENTIALS = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                                CSecurity.USER_ADMINISTRATOR_PASSWORD);
  private static final String IF_NONE_MATCH = "If-None-Match";

  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (new FileSystemResource ("src/test/resources/test-smp-server-xml-peppol-httpcache.properties"));

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

  private static void _assertFreshResponse (@NonNull final MockHttpResponse aResponse)
  {
    assertEquals (200, aResponse.getStatusCode ());
    assertTrue (aResponse.hasBody ());
    final String sCacheControl = aResponse.getFirstHeaderValue ("Cache-Control");
    assertNotNull (sCacheControl);
    assertTrue (sCacheControl, sCacheControl.contains ("max-age=60"));
    final String sETag = aResponse.getFirstHeaderValue ("ETag");
    assertNotNull (sETag);
    assertTrue (sETag, sETag.startsWith ("\"") && sETag.endsWith ("\""));
  }

  private void _testConditionalGet (@NonNull final String sPath)
  {
    // Fresh response
    final MockHttpResponse aFirst = m_aClient.get (sPath);
    _assertFreshResponse (aFirst);
    final String sETag = aFirst.getFirstHeaderValue ("ETag");

    // Strong match
    MockHttpResponse aResponse = m_aClient.get (sPath, Map.of (IF_NONE_MATCH, sETag));
    assertEquals (304, aResponse.getStatusCode ());
    assertFalse (aResponse.hasBody ());
    assertEquals (sETag, aResponse.getFirstHeaderValue ("ETag"));
    final String sCacheControl = aResponse.getFirstHeaderValue ("Cache-Control");
    assertNotNull (sCacheControl);
    assertTrue (sCacheControl, sCacheControl.contains ("max-age=60"));

    // Weak match
    aResponse = m_aClient.get (sPath, Map.of (IF_NONE_MATCH, "W/" + sETag));
    assertEquals (304, aResponse.getStatusCode ());
    assertFalse (aResponse.hasBody ());

    // No match
    aResponse = m_aClient.get (sPath, Map.of (IF_NONE_MATCH, "\"0000\""));
    _assertFreshResponse (aResponse);
    assertEquals (sETag, aResponse.getFirstHeaderValue ("ETag"));
  }

  @Test
  public void testHttpCaching ()
  {
    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:httpcache");
    final String sPI = aPI.getURIPercentEncoded ();
    final PeppolDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();
    final String sDT = aDT.getURIPercentEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ServiceMetadataType aSM = new ServiceMetadataType ();
    final ServiceInformationType aSI = new ServiceInformationType ();
    aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSI.setDocumentIdentifier (aDT);
    {
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
    }
    aSM.setServiceInformation (aSI);

    // Unknown participant: error responses keep the "no caching" headers
    MockHttpResponse aResponse = m_aClient.get (sPI);
    assertEquals (404, aResponse.getStatusCode ());
    assertNull (aResponse.getFirstHeaderValue ("ETag"));
    final String sNoCache = aResponse.getFirstHeaderValue ("Cache-Control");
    assertNotNull (sNoCache);
    assertTrue (sNoCache, sNoCache.contains ("no-store"));

    try
    {
      assertEquals (200, m_aClient.put (sPI, CREDENTIALS, _xmlEntity (aSG)).getStatusCode ());

      // Service Group
      _testConditionalGet (sPI);
      final String sOldETag = m_aClient.get (sPI).getFirstHeaderValue ("ETag");

      // Change the participant
      assertEquals (200, m_aClient.put (sPI + "/services/" + sDT, CREDENTIALS, _xmlEntity (aSM)).getStatusCode ());

      // The old ETag does not match anymore
      aResponse = m_aClient.get (sPI, Map.of (IF_NONE_MATCH, sOldETag));
      _assertFreshResponse (aResponse);
      assertNotEquals (sOldETag, aResponse.getFirstHeaderValue ("ETag"));

      // Service Metadata
      _testConditionalGet (sPI + "/services/" + sDT);
    }
    finally
    {
      m_aClient.delete (sPI + "/services/" + sDT, CREDENTIALS);
      m_aClient.delete (sPI, CREDENTIALS);
    }
  }
}
