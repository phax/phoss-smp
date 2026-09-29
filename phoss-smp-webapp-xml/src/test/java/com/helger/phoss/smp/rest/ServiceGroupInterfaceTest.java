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
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.mock.MockHttpClient;
import com.helger.phoss.smp.mock.MockHttpResponse;
import com.helger.phoss.smp.mock.MockSMPClient;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.smpclient.exception.SMPClientException;
import com.helger.smpclient.exception.SMPClientNotFoundException;
import com.helger.smpclient.peppol.SMPClient;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceMetadataReferenceCollectionType;

/**
 * Test class for class {@link SMPRestFilter}
 *
 * @author Philip Helger
 */
public final class ServiceGroupInterfaceTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceGroupInterfaceTest.class);
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

  @Test
  public void testCreateAndDeleteServiceGroupHttpClient ()
  {
    // Lower case version
    final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9930:de203827312");
    final String sPI_LC = aPI_LC.getURIPercentEncoded ();
    // Upper case version
    final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9930:DE203827312");
    final String sPI_UC = aPI_UC.getURIPercentEncoded ();

    final ServiceGroupType aSG_LC = new ServiceGroupType ();
    aSG_LC.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
    aSG_LC.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ServiceGroupType aSG_UC = new ServiceGroupType ();
    aSG_UC.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_UC));
    aSG_UC.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();

    // GET
    _testResponse (m_aClient.get (sPI_LC), 404);
    _testResponse (m_aClient.get (sPI_UC), 404);

    try
    {
      // PUT 1 - create
      _testResponse (m_aClient.put (sPI_LC, CREDENTIALS, _xmlEntity (aSG_LC)), 200);

      // PUT 2 - upper case - already present
      _testResponse (m_aClient.put (sPI_UC, CREDENTIALS, _xmlEntity (aSG_UC)), 200);

      // Both regular and upper case must work
      assertNotNull (_getServiceGroup (sPI_LC));
      assertNotNull (_getServiceGroup (sPI_UC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

      // PUT 2 - overwrite
      _testResponse (m_aClient.put (sPI_LC, CREDENTIALS, _xmlEntity (aSG_LC)), 200);
      _testResponse (m_aClient.put (sPI_UC, CREDENTIALS, _xmlEntity (aSG_UC)), 200);

      // Both regular and upper case must work
      assertNotNull (_getServiceGroup (sPI_LC));
      assertNotNull (_getServiceGroup (sPI_UC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

      // DELETE 1
      _testResponse (m_aClient.delete (sPI_LC, CREDENTIALS), 200);

      // Both must be deleted
      _testResponse (m_aClient.get (sPI_LC), 404);
      _testResponse (m_aClient.get (sPI_UC), 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));
    }
    finally
    {
      // DELETE 2
      _testResponse (m_aClient.delete (sPI_LC, CREDENTIALS), 200, 404);

      // Both must be deleted
      _testResponse (m_aClient.get (sPI_LC), 404);
      _testResponse (m_aClient.get (sPI_UC), 404);
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
