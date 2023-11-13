/*
 * Copyright (C) 2014-2023 Philip Helger and contributors
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

import javax.annotation.Nonnull;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.string.StringHelper;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.businesscard.v1.PD1APIHelper;
import com.helger.peppol.businesscard.v1.PD1BusinessCardType;
import com.helger.peppol.businesscard.v1.PD1BusinessEntityType;
import com.helger.peppol.businesscard.v2.PD2APIHelper;
import com.helger.peppol.businesscard.v2.PD2BusinessCardType;
import com.helger.peppol.businesscard.v2.PD2BusinessEntityType;
import com.helger.peppol.businesscard.v3.PD3APIHelper;
import com.helger.peppol.businesscard.v3.PD3BusinessCardType;
import com.helger.peppol.businesscard.v3.PD3BusinessEntityType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.xsds.peppol.smp1.ObjectFactory;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceMetadataReferenceCollectionType;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

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

  private final ObjectFactory m_aObjFactory = new ObjectFactory ();
  private final com.helger.peppol.businesscard.v1.ObjectFactory m_aBC1ObjFactory = new com.helger.peppol.businesscard.v1.ObjectFactory ();
  private final com.helger.peppol.businesscard.v2.ObjectFactory m_aBC2ObjFactory = new com.helger.peppol.businesscard.v2.ObjectFactory ();
  private final com.helger.peppol.businesscard.v3.ObjectFactory m_aBC3ObjFactory = new com.helger.peppol.businesscard.v3.ObjectFactory ();

  @Nonnull
  private static Builder _addCredentials (@Nonnull final Builder aBuilder)
  {
    // Use default credentials for XML backend
    return aBuilder.header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ());
  }

  private static int _testResponseJerseyClient (@Nonnull final Response aResponseMsg,
                                                @Nonempty final int... aStatusCodes)
  {
    ValueEnforcer.notNull (aResponseMsg, "ResponseMsg");
    ValueEnforcer.notEmpty (aStatusCodes, "StatusCodes");

    assertNotNull (aResponseMsg);
    // Read response
    final String sResponse = aResponseMsg.readEntity (String.class);
    if (StringHelper.hasText (sResponse))
      LOGGER.info ("HTTP Response: " + sResponse);
    assertTrue (aResponseMsg.getStatus () + " is not in " + Arrays.toString (aStatusCodes),
                ArrayHelper.contains (aStatusCodes, aResponseMsg.getStatus ()));
    return aResponseMsg.getStatus ();
  }

  @Test
  public void testGetCreateV1GetDeleteGet ()
  {
    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    assertNotNull (aSGMgr);
    final ISMPBusinessCardManager aBCMgr = SMPMetaManager.getBusinessCardMgr ();
    assertNotNull (aBCMgr);

    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:tester");
    final String sPI = aPI.getURIEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final WebTarget aTarget = ClientBuilder.newClient ().target (m_aRule.getFullURL ());
    Response aResponseMsg;

    try
    {
      // Create SG
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (
                                                                                                                        aSG)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // Get SG - must work
      assertNotNull (aTarget.path (sPI).request ().get (ServiceGroupType.class));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI));

      // Get BC - not existing
      aResponseMsg = aTarget.path ("businesscard").path (sPI).request ().get ();
      _testResponseJerseyClient (aResponseMsg, 404);

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

      aResponseMsg = _addCredentials (aTarget.path ("businesscard").path (sPI).request ()).put (Entity.xml (
                                                                                                            m_aBC1ObjFactory.createBusinessCard (aBC)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // Get BC - must work (always V3)
      PD3BusinessCardType aReadBC = aTarget.path ("businesscard").path (sPI).request ().get (PD3BusinessCardType.class);
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
      aResponseMsg = _addCredentials (aTarget.path ("businesscard").path (sPI).request ()).put (Entity.xml (
                                                                                                            m_aBC1ObjFactory.createBusinessCard (aBC)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // Get BC - must work (always V3)
      aReadBC = aTarget.path ("businesscard").path (sPI).request ().get (PD3BusinessCardType.class);
      assertNotNull (aReadBC);
      assertEquals (3, aReadBC.getBusinessEntityCount ());

      aGetBC = aBCMgr.getSMPBusinessCardOfID (aPI);
      assertNotNull (aGetBC);
    }
    finally
    {
      // Delete Business Card
      aResponseMsg = _addCredentials (aTarget.path ("businesscard").path (sPI).request ()).delete ();
      _testResponseJerseyClient (aResponseMsg, 200, 404);

      // must be deleted
      _testResponseJerseyClient (aTarget.path ("businesscard").path (sPI).request ().get (), 404);
      assertNull (aBCMgr.getSMPBusinessCardOfID (aPI));

      // Delete service Group
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).delete ();
      _testResponseJerseyClient (aResponseMsg, 200, 404);

      // must be deleted
      _testResponseJerseyClient (aTarget.path (sPI).request ().get (), 404);
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
    final String sPI = aPI.getURIEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final WebTarget aTarget = ClientBuilder.newClient ().target (m_aRule.getFullURL ());
    Response aResponseMsg;

    try
    {
      // Create SG
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (
                                                                                                                        aSG)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // Get SG - must work
      assertNotNull (aTarget.path (sPI).request ().get (ServiceGroupType.class));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI));

      // Get BC - not existing
      aResponseMsg = aTarget.path ("businesscard").path (sPI).request ().get ();
      _testResponseJerseyClient (aResponseMsg, 404);

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

      aResponseMsg = _addCredentials (aTarget.path ("businesscard").path (sPI).request ()).put (Entity.xml (
                                                                                                            m_aBC2ObjFactory.createBusinessCard (aBC)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // Get BC - must work (always V3)
      PD3BusinessCardType aReadBC = aTarget.path ("businesscard").path (sPI).request ().get (PD3BusinessCardType.class);
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
      aResponseMsg = _addCredentials (aTarget.path ("businesscard").path (sPI).request ()).put (Entity.xml (
                                                                                                            m_aBC2ObjFactory.createBusinessCard (aBC)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // Get BC - must work (always V3)
      aReadBC = aTarget.path ("businesscard").path (sPI).request ().get (PD3BusinessCardType.class);
      assertNotNull (aReadBC);
      assertEquals (3, aReadBC.getBusinessEntityCount ());

      aGetBC = aBCMgr.getSMPBusinessCardOfID (aPI);
      assertNotNull (aGetBC);
    }
    finally
    {
      // Delete Business Card
      aResponseMsg = _addCredentials (aTarget.path ("businesscard").path (sPI).request ()).delete ();
      _testResponseJerseyClient (aResponseMsg, 200, 404);

      // must be deleted
      _testResponseJerseyClient (aTarget.path ("businesscard").path (sPI).request ().get (), 404);
      assertNull (aBCMgr.getSMPBusinessCardOfID (aPI));

      // Delete service Group
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).delete ();
      _testResponseJerseyClient (aResponseMsg, 200, 404);

      // must be deleted
      _testResponseJerseyClient (aTarget.path (sPI).request ().get (), 404);
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
    final String sPI = aPI.getURIEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final WebTarget aTarget = ClientBuilder.newClient ().target (m_aRule.getFullURL ());
    Response aResponseMsg;

    try
    {
      // Create SG
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (
                                                                                                                        aSG)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // Get SG - must work
      assertNotNull (aTarget.path (sPI).request ().get (ServiceGroupType.class));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI));

      // Get BC - not existing
      aResponseMsg = aTarget.path ("businesscard").path (sPI).request ().get ();
      _testResponseJerseyClient (aResponseMsg, 404);

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

      aResponseMsg = _addCredentials (aTarget.path ("businesscard").path (sPI).request ()).put (Entity.xml (
                                                                                                            m_aBC3ObjFactory.createBusinessCard (aBC)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // Get BC - must work (always V3)
      PD3BusinessCardType aReadBC = aTarget.path ("businesscard").path (sPI).request ().get (PD3BusinessCardType.class);
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
      aResponseMsg = _addCredentials (aTarget.path ("businesscard").path (sPI).request ()).put (Entity.xml (
                                                                                                            m_aBC3ObjFactory.createBusinessCard (aBC)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // Get BC - must work (always V3)
      aReadBC = aTarget.path ("businesscard").path (sPI).request ().get (PD3BusinessCardType.class);
      assertNotNull (aReadBC);
      assertEquals (3, aReadBC.getBusinessEntityCount ());

      aGetBC = aBCMgr.getSMPBusinessCardOfID (aPI);
      assertNotNull (aGetBC);
    }
    finally
    {
      // Delete Business Card
      aResponseMsg = _addCredentials (aTarget.path ("businesscard").path (sPI).request ()).delete ();
      _testResponseJerseyClient (aResponseMsg, 200, 404);

      // must be deleted
      _testResponseJerseyClient (aTarget.path ("businesscard").path (sPI).request ().get (), 404);
      assertNull (aBCMgr.getSMPBusinessCardOfID (aPI));

      // Delete service Group
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).delete ();
      _testResponseJerseyClient (aResponseMsg, 200, 404);

      // must be deleted
      _testResponseJerseyClient (aTarget.path (sPI).request ().get (), 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI));
    }
  }
}
