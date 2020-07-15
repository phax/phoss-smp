/**
 * Copyright (C) 2014-2020 Philip Helger and contributors
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.mock.MockSMPClient;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.phoss.smp.rest2.Rest2Filter;
import com.helger.photon.security.CSecurity;
import com.helger.smpclient.exception.SMPClientException;
import com.helger.smpclient.exception.SMPClientNotFoundException;
import com.helger.smpclient.peppol.SMPClient;
import com.helger.smpclient.peppol.jaxb.ObjectFactory;
import com.helger.smpclient.peppol.jaxb.ServiceGroupType;
import com.helger.smpclient.peppol.jaxb.ServiceMetadataReferenceCollectionType;

/**
 * Test class for class {@link Rest2Filter}
 *
 * @author Philip Helger
 */
public final class ServiceGroupInterfaceTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceGroupInterfaceTest.class);
  private static final BasicAuthClientCredentials CREDENTIALS = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                                CSecurity.USER_ADMINISTRATOR_PASSWORD);

  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (ClassPathResource.getAsFile ("test-smp-server-xml.properties")
                                                                                           .getAbsolutePath ());

  private final ObjectFactory m_aObjFactory = new ObjectFactory ();

  @Nonnull
  private static Builder _addCredentials (@Nonnull final Builder aBuilder)
  {
    // Use default credentials for XML backend
    return aBuilder.header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ());
  }

  private static int _testResponseJerseyClient (@Nonnull final Response aResponseMsg, @Nonempty final int... aStatusCodes)
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
  public void testCreateAndDeleteServiceGroupJerseyClient ()
  {
    // Lower case version
    final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9930:de203827312");
    final String sPI_LC = aPI_LC.getURIEncoded ();
    // Upper case version
    final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9930:DE203827312");
    final String sPI_UC = aPI_UC.getURIEncoded ();

    final ServiceGroupType aSG_LC = new ServiceGroupType ();
    aSG_LC.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
    aSG_LC.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ServiceGroupType aSG_UC = new ServiceGroupType ();
    aSG_UC.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_UC));
    aSG_UC.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final WebTarget aTarget = ClientBuilder.newClient ().target (m_aRule.getFullURL ());
    Response aResponseMsg;

    // GET
    _testResponseJerseyClient (aTarget.path (sPI_LC).request ().get (), 404);
    _testResponseJerseyClient (aTarget.path (sPI_UC).request ().get (), 404);

    try
    {
      // PUT 1 - create
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC).request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG_LC)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // PUT 2 - upper case - already present
      aResponseMsg = _addCredentials (aTarget.path (sPI_UC).request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG_UC)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // Both regular and upper case must work
      assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
      assertNotNull (aTarget.path (sPI_UC).request ().get (ServiceGroupType.class));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

      // PUT 2 - overwrite
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC).request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG_LC)));
      _testResponseJerseyClient (aResponseMsg, 200);
      aResponseMsg = _addCredentials (aTarget.path (sPI_UC).request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG_UC)));
      _testResponseJerseyClient (aResponseMsg, 200);

      // Both regular and upper case must work
      assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
      assertNotNull (aTarget.path (sPI_UC).request ().get (ServiceGroupType.class));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

      // DELETE 1
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC).request ()).delete ();
      _testResponseJerseyClient (aResponseMsg, 200);

      // Both must be deleted
      _testResponseJerseyClient (aTarget.path (sPI_LC).request ().get (), 404);
      _testResponseJerseyClient (aTarget.path (sPI_UC).request ().get (), 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));
    }
    finally
    {
      // DELETE 2
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC).request ()).delete ();
      _testResponseJerseyClient (aResponseMsg, 200, 404);

      // Both must be deleted
      _testResponseJerseyClient (aTarget.path (sPI_LC).request ().get (), 404);
      _testResponseJerseyClient (aTarget.path (sPI_UC).request ().get (), 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));
    }
  }

  @Test
  public void testCreateAndDeleteServiceGroupSMPClient () throws SMPClientException
  {
    // Lower case version
    final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9930:de203827312");
    // Upper case version
    final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9930:DE203827312");

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final SMPClient aSMPClient = new MockSMPClient ();

    // GET
    assertNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
    assertNull (aSMPClient.getServiceGroupOrNull (aPI_UC));

    try
    {
      // PUT 1 - create
      aSMPClient.saveServiceGroup (aSG, CREDENTIALS);

      // Both regular and upper case must work
      assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
      assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

      // PUT 2 - overwrite
      aSMPClient.saveServiceGroup (aSG, CREDENTIALS);

      // Both regular and upper case must work
      assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
      assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

      // DELETE 1
      aSMPClient.deleteServiceGroup (aPI_LC, CREDENTIALS);

      // Both must be deleted
      assertNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
      assertNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));
    }
    finally
    {
      // DELETE 2
      try
      {
        aSMPClient.deleteServiceGroup (aPI_LC, CREDENTIALS);
      }
      catch (final SMPClientNotFoundException ex)
      {
        // Expected
      }

      // Both must be deleted
      assertNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
      assertNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));
    }
  }
}
