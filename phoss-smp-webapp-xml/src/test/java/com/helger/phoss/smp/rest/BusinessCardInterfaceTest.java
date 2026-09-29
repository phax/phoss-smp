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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.hc.core5.http.HttpEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.array.ArrayHelper;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.io.resource.FileSystemResource;
import com.helger.peppol.businesscard.v1.PD1APIHelper;
import com.helger.peppol.businesscard.v1.PD1BusinessCardMarshaller;
import com.helger.peppol.businesscard.v1.PD1BusinessCardType;
import com.helger.peppol.businesscard.v1.PD1BusinessEntityType;
import com.helger.peppol.businesscard.v2.PD2APIHelper;
import com.helger.peppol.businesscard.v2.PD2BusinessCardMarshaller;
import com.helger.peppol.businesscard.v2.PD2BusinessCardType;
import com.helger.peppol.businesscard.v2.PD2BusinessEntityType;
import com.helger.peppol.businesscard.v3.PD3APIHelper;
import com.helger.peppol.businesscard.v3.PD3BusinessCardMarshaller;
import com.helger.peppol.businesscard.v3.PD3BusinessCardType;
import com.helger.peppol.businesscard.v3.PD3BusinessEntityType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.mock.MockHttpClient;
import com.helger.phoss.smp.mock.MockHttpResponse;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceMetadataReferenceCollectionType;

/**
 * Test class for class {@link SMPRestFilter}
 *
 * @author Philip Helger
 */
public final class BusinessCardInterfaceTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BusinessCardInterfaceTest.class);
  private static final BasicAuthClientCredentials CREDENTIALS = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                                CSecurity.USER_ADMINISTRATOR_PASSWORD);

  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (new FileSystemResource ("src/test/resources/test-smp-server-xml-peppol.properties"));

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
  private static HttpEntity _xmlEntity (@NonNull final PD1BusinessCardType aBC)
  {
    return MockHttpClient.createXMLEntity (new PD1BusinessCardMarshaller (), aBC);
  }

  @NonNull
  private static HttpEntity _xmlEntity (@NonNull final PD2BusinessCardType aBC)
  {
    return MockHttpClient.createXMLEntity (new PD2BusinessCardMarshaller (), aBC);
  }

  @NonNull
  private static HttpEntity _xmlEntity (@NonNull final PD3BusinessCardType aBC)
  {
    return MockHttpClient.createXMLEntity (new PD3BusinessCardMarshaller (), aBC);
  }

  private static int _testResponse (@NonNull final MockHttpResponse aResponseMsg, @Nonempty final int... aStatusCodes)
  {
    ValueEnforcer.notNull (aResponseMsg, "ResponseMsg");
    ValueEnforcer.notEmpty (aStatusCodes, "StatusCodes");

    // Read response
    final String sResponse = aResponseMsg.getBodyAsString ();
    if (StringHelper.isNotEmpty (sResponse))
      LOGGER.info ("HTTP Response: " + sResponse);
    assertTrue (aResponseMsg.getStatusCode () + " is not in " + Arrays.toString (aStatusCodes),
                ArrayHelper.contains (aStatusCodes, aResponseMsg.getStatusCode ()));
    return aResponseMsg.getStatusCode ();
  }

  @Nullable
  private ServiceGroupType _getServiceGroup (@NonNull final String sPath)
  {
    final MockHttpResponse aResponseMsg = m_aClient.get (sPath);
    assertEquals (200, aResponseMsg.getStatusCode ());
    final String sBody = aResponseMsg.getBodyAsString ();
    return sBody == null ? null : new SMPMarshallerServiceGroupType ().read (sBody);
  }

  @Nullable
  private PD3BusinessCardType _getBusinessCard (@NonNull final String sPath)
  {
    final MockHttpResponse aResponseMsg = m_aClient.get (sPath);
    assertEquals (200, aResponseMsg.getStatusCode ());
    final String sBody = aResponseMsg.getBodyAsString ();
    return sBody == null ? null : new PD3BusinessCardMarshaller ().read (sBody);
  }

  @Test
  public void testGetCreateV1GetDeleteGet ()
  {
    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    assertNotNull (aSGMgr);
    final ISMPBusinessCardManager aBCMgr = SMPMetaManager.getBusinessCardMgr ();
    assertNotNull (aBCMgr);

    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:tester");
    final String sPI = aPI.getURIPercentEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    MockHttpResponse aResponseMsg;

    try
    {
      // Create SG
      aResponseMsg = m_aClient.put (sPI, CREDENTIALS, _xmlEntity (aSG));
      _testResponse (aResponseMsg, 200);

      // Get SG - must work
      assertNotNull (_getServiceGroup (sPI));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI));

      // Get BC - not existing
      aResponseMsg = m_aClient.get ("businesscard/" + sPI);
      _testResponse (aResponseMsg, 404);

      // Create BC with some entities
      final PD1BusinessCardType aBC = new PD1BusinessCardType ();
      aBC.setParticipantIdentifier (PD1APIHelper.createIdentifier (aPI.getScheme (), aPI.getValue ()));
      PD1BusinessEntityType aBE = new PD1BusinessEntityType ();
      aBE.setName ("BusinessEntity1");
      aBE.setCountryCode ("AT");
      aBE.setGeographicalInformation ("Vienna");
      aBC.addBusinessEntity (aBE);
      aBE = new PD1BusinessEntityType ();
      aBE.setName ("BusinessEntity2");
      aBE.setCountryCode ("DE");
      aBE.setGeographicalInformation ("Berlin");
      aBC.addBusinessEntity (aBE);

      aResponseMsg = m_aClient.put ("businesscard/" + sPI, CREDENTIALS, _xmlEntity (aBC));
      _testResponse (aResponseMsg, 200);

      // Get BC - must work (always V3)
      PD3BusinessCardType aReadBC = _getBusinessCard ("businesscard/" + sPI);
      assertNotNull (aReadBC);
      assertEquals (2, aReadBC.getBusinessEntityCount ());

      ISMPBusinessCard aGetBC = aBCMgr.getSMPBusinessCardOfID (aPI);
      assertNotNull (aGetBC);

      // Update BC - add entity
      aBE = new PD1BusinessEntityType ();
      aBE.setName ("BusinessEntity3");
      aBE.setCountryCode ("SE");
      aBE.setGeographicalInformation ("Stockholm");
      aBC.addBusinessEntity (aBE);
      aResponseMsg = m_aClient.put ("businesscard/" + sPI, CREDENTIALS, _xmlEntity (aBC));
      _testResponse (aResponseMsg, 200);

      // Get BC - must work (always V3)
      aReadBC = _getBusinessCard ("businesscard/" + sPI);
      assertNotNull (aReadBC);
      assertEquals (3, aReadBC.getBusinessEntityCount ());

      aGetBC = aBCMgr.getSMPBusinessCardOfID (aPI);
      assertNotNull (aGetBC);
    }
    finally
    {
      // Delete Business Card
      aResponseMsg = m_aClient.delete ("businesscard/" + sPI, CREDENTIALS);
      _testResponse (aResponseMsg, 200, 404);

      // must be deleted
      _testResponse (m_aClient.get ("businesscard/" + sPI), 404);
      assertNull (aBCMgr.getSMPBusinessCardOfID (aPI));

      // Delete service Group
      aResponseMsg = m_aClient.delete (sPI, CREDENTIALS);
      _testResponse (aResponseMsg, 200, 404);

      // must be deleted
      _testResponse (m_aClient.get (sPI), 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI));
    }
  }

  @Test
  public void testGetCreateV2GetDeleteGet ()
  {
    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    assertNotNull (aSGMgr);
    final ISMPBusinessCardManager aBCMgr = SMPMetaManager.getBusinessCardMgr ();
    assertNotNull (aBCMgr);

    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:tester");
    final String sPI = aPI.getURIPercentEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    MockHttpResponse aResponseMsg;

    try
    {
      // Create SG
      aResponseMsg = m_aClient.put (sPI, CREDENTIALS, _xmlEntity (aSG));
      _testResponse (aResponseMsg, 200);

      // Get SG - must work
      assertNotNull (_getServiceGroup (sPI));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI));

      // Get BC - not existing
      aResponseMsg = m_aClient.get ("businesscard/" + sPI);
      _testResponse (aResponseMsg, 404);

      // Create BC with some entities
      final PD2BusinessCardType aBC = new PD2BusinessCardType ();
      aBC.setParticipantIdentifier (PD2APIHelper.createIdentifier (aPI.getScheme (), aPI.getValue ()));
      PD2BusinessEntityType aBE = new PD2BusinessEntityType ();
      aBE.setName ("BusinessEntity1");
      aBE.setCountryCode ("AT");
      aBE.setGeographicalInformation ("Vienna");
      aBC.addBusinessEntity (aBE);
      aBE = new PD2BusinessEntityType ();
      aBE.setName ("BusinessEntity2");
      aBE.setCountryCode ("DE");
      aBE.setGeographicalInformation ("Berlin");
      aBC.addBusinessEntity (aBE);

      aResponseMsg = m_aClient.put ("businesscard/" + sPI, CREDENTIALS, _xmlEntity (aBC));
      _testResponse (aResponseMsg, 200);

      // Get BC - must work (always V3)
      PD3BusinessCardType aReadBC = _getBusinessCard ("businesscard/" + sPI);
      assertNotNull (aReadBC);
      assertEquals (2, aReadBC.getBusinessEntityCount ());

      ISMPBusinessCard aGetBC = aBCMgr.getSMPBusinessCardOfID (aPI);
      assertNotNull (aGetBC);

      // Update BC - add entity
      aBE = new PD2BusinessEntityType ();
      aBE.setName ("BusinessEntity3");
      aBE.setCountryCode ("SE");
      aBE.setGeographicalInformation ("Stockholm");
      aBC.addBusinessEntity (aBE);
      aResponseMsg = m_aClient.put ("businesscard/" + sPI, CREDENTIALS, _xmlEntity (aBC));
      _testResponse (aResponseMsg, 200);

      // Get BC - must work (always V3)
      aReadBC = _getBusinessCard ("businesscard/" + sPI);
      assertNotNull (aReadBC);
      assertEquals (3, aReadBC.getBusinessEntityCount ());

      aGetBC = aBCMgr.getSMPBusinessCardOfID (aPI);
      assertNotNull (aGetBC);
    }
    finally
    {
      // Delete Business Card
      aResponseMsg = m_aClient.delete ("businesscard/" + sPI, CREDENTIALS);
      _testResponse (aResponseMsg, 200, 404);

      // must be deleted
      _testResponse (m_aClient.get ("businesscard/" + sPI), 404);
      assertNull (aBCMgr.getSMPBusinessCardOfID (aPI));

      // Delete service Group
      aResponseMsg = m_aClient.delete (sPI, CREDENTIALS);
      _testResponse (aResponseMsg, 200, 404);

      // must be deleted
      _testResponse (m_aClient.get (sPI), 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI));
    }
  }

  @Test
  public void testGetCreateV3GetDeleteGet ()
  {
    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    assertNotNull (aSGMgr);
    final ISMPBusinessCardManager aBCMgr = SMPMetaManager.getBusinessCardMgr ();
    assertNotNull (aBCMgr);

    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:tester");
    final String sPI = aPI.getURIPercentEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    MockHttpResponse aResponseMsg;

    try
    {
      // Create SG
      aResponseMsg = m_aClient.put (sPI, CREDENTIALS, _xmlEntity (aSG));
      _testResponse (aResponseMsg, 200);

      // Get SG - must work
      assertNotNull (_getServiceGroup (sPI));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI));

      // Get BC - not existing
      aResponseMsg = m_aClient.get ("businesscard/" + sPI);
      _testResponse (aResponseMsg, 404);

      // Create BC with some entities
      final PD3BusinessCardType aBC = new PD3BusinessCardType ();
      aBC.setParticipantIdentifier (PD3APIHelper.createIdentifier (aPI.getScheme (), aPI.getValue ()));
      PD3BusinessEntityType aBE = new PD3BusinessEntityType ();
      aBE.addName (PD3APIHelper.createName ("BusinessEntity1", null));
      aBE.setCountryCode ("AT");
      aBE.setGeographicalInformation ("Vienna");
      aBC.addBusinessEntity (aBE);
      aBE = new PD3BusinessEntityType ();
      aBE.addName (PD3APIHelper.createName ("BusinessEntity2", null));
      aBE.setCountryCode ("DE");
      aBE.setGeographicalInformation ("Berlin");
      aBC.addBusinessEntity (aBE);

      aResponseMsg = m_aClient.put ("businesscard/" + sPI, CREDENTIALS, _xmlEntity (aBC));
      _testResponse (aResponseMsg, 200);

      // Get BC - must work (always V3)
      PD3BusinessCardType aReadBC = _getBusinessCard ("businesscard/" + sPI);
      assertNotNull (aReadBC);
      assertEquals (2, aReadBC.getBusinessEntityCount ());

      ISMPBusinessCard aGetBC = aBCMgr.getSMPBusinessCardOfID (aPI);
      assertNotNull (aGetBC);

      // Update BC - add entity
      aBE = new PD3BusinessEntityType ();
      aBE.addName (PD3APIHelper.createName ("BusinessEntity3", null));
      aBE.setCountryCode ("SE");
      aBE.setGeographicalInformation ("Stockholm");
      aBC.addBusinessEntity (aBE);
      aResponseMsg = m_aClient.put ("businesscard/" + sPI, CREDENTIALS, _xmlEntity (aBC));
      _testResponse (aResponseMsg, 200);

      // Get BC - must work (always V3)
      aReadBC = _getBusinessCard ("businesscard/" + sPI);
      assertNotNull (aReadBC);
      assertEquals (3, aReadBC.getBusinessEntityCount ());

      aGetBC = aBCMgr.getSMPBusinessCardOfID (aPI);
      assertNotNull (aGetBC);
    }
    finally
    {
      // Delete Business Card
      aResponseMsg = m_aClient.delete ("businesscard/" + sPI, CREDENTIALS);
      _testResponse (aResponseMsg, 200, 404);

      // must be deleted
      _testResponse (m_aClient.get ("businesscard/" + sPI), 404);
      assertNull (aBCMgr.getSMPBusinessCardOfID (aPI));

      // Delete service Group
      aResponseMsg = m_aClient.delete (sPI, CREDENTIALS);
      _testResponse (aResponseMsg, 200, 404);

      // must be deleted
      _testResponse (m_aClient.get (sPI), 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI));
    }
  }
}
