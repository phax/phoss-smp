/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smp.ObjectFactory;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smpserver.SMPServerTestRule;
import com.helger.peppol.smpserver.data.xml.mgr.XMLServiceGroupManager;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.photon.security.CSecurity;
import com.helger.web.http.CHTTPHeader;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * Test class for class {@link ServiceGroupInterface}
 *
 * @author Philip Helger
 */
public final class ServiceGroupInterfaceTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ServiceGroupInterfaceTest.class);

  @Rule
  public final TestRule m_aRule = new SMPServerTestRule (new ClassPathResource ("test-smp-server-xml.properties").getAsFile ().getAbsolutePath ());

  private HttpServer m_aServer;
  private WebTarget m_aTarget;
  private final ObjectFactory m_aObjFactory = new ObjectFactory ();

  @Before
  public void setUp ()
  {
    // http only
    m_aServer = MockServer.startRegularServer ();

    final Client aClient = ClientBuilder.newClient ();
    m_aTarget = aClient.target (MockServer.BASE_URI_HTTP);
  }

  @After
  public void tearDown ()
  {
    m_aServer.shutdownNow ();
  }

  @Nonnull
  private static Builder _addCredentials (@Nonnull final Builder aBuilder)
  {
    if (SMPMetaManager.getServiceGroupMgr () instanceof XMLServiceGroupManager)
    {
      // Use default credentials for XML backend
      return aBuilder.header (CHTTPHeader.AUTHORIZATION,
                              new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_LOGIN,
                                                              CSecurity.USER_ADMINISTRATOR_PASSWORD).getRequestValue ());
    }

    // Use default credentials for SQL backend
    return aBuilder.header (CHTTPHeader.AUTHORIZATION, new BasicAuthClientCredentials ("peppol", "Test1234").getRequestValue ());
  }

  private static void _testResponse (final Response aResponseMsg, final int nStatusCode)
  {
    assertNotNull (aResponseMsg);
    // Read response
    final String sResponse = aResponseMsg.readEntity (String.class);
    if (StringHelper.hasText (sResponse))
      s_aLogger.info ("HTTP Response: " + sResponse);
    assertEquals (nStatusCode, aResponseMsg.getStatus ());
  }

  @Test
  public void testCreateAndDeleteServiceGroup ()
  {
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:xxx");
    final String sPI = aPI.getURIEncoded ();
    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (aPI);

    Response aResponseMsg;

    try
    {
      // PUT 1
      aResponseMsg = _addCredentials (m_aTarget.path (sPI).request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
      _testResponse (aResponseMsg, 200);

      assertTrue (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI));

      // PUT 2
      aResponseMsg = _addCredentials (m_aTarget.path (sPI).request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
      _testResponse (aResponseMsg, 200);

      assertTrue (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI));

      // DELETE 1
      aResponseMsg = _addCredentials (m_aTarget.path (sPI).request ()).delete ();
      _testResponse (aResponseMsg, 200);

      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI));
    }
    finally
    {
      // DELETE 2
      aResponseMsg = _addCredentials (m_aTarget.path (sPI).request ()).delete ();
      _testResponse (aResponseMsg, 404);

      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI));
    }
  }
}
