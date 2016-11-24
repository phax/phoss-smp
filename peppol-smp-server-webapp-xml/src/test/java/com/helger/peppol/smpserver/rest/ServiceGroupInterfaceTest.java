/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.annotation.Nonnull;
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
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;
import com.helger.http.CHTTPHeader;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smp.ObjectFactory;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.photon.security.CSecurity;

/**
 * Test class for class {@link ServiceGroupInterface}.
 *
 * @author Philip Helger
 */
public final class ServiceGroupInterfaceTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ServiceGroupInterfaceTest.class);

  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (ClassPathResource.getAsFile ("test-smp-server-xml.properties")
                                                                                           .getAbsolutePath ());

  private final ObjectFactory m_aObjFactory = new ObjectFactory ();

  @Nonnull
  private static Builder _addCredentials (@Nonnull final Builder aBuilder)
  {
    // Use default credentials for XML backend
    return aBuilder.header (CHTTPHeader.AUTHORIZATION,
                            new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_LOGIN,
                                                            CSecurity.USER_ADMINISTRATOR_PASSWORD).getRequestValue ());
  }

  private static int _testResponse (@Nonnull final Response aResponseMsg, @Nonempty final int... aStatusCodes)
  {
    ValueEnforcer.notNull (aResponseMsg, "ResponseMsg");
    ValueEnforcer.notEmpty (aStatusCodes, "StatusCodes");

    assertNotNull (aResponseMsg);
    // Read response
    final String sResponse = aResponseMsg.readEntity (String.class);
    if (StringHelper.hasText (sResponse))
      s_aLogger.info ("HTTP Response: " + sResponse);
    assertTrue (aResponseMsg.getStatus () +
                " is not in " +
                Arrays.toString (aStatusCodes),
                ArrayHelper.contains (aStatusCodes, aResponseMsg.getStatus ()));
    return aResponseMsg.getStatus ();
  }

  @Test
  public void testCreateAndDeleteServiceGroup ()
  {
    // Lower case version
    final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
    final String sPI_LC = aPI_LC.getURIEncoded ();
    // Upper case version
    final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:XXX");
    final String sPI_UC = aPI_UC.getURIEncoded ();
    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));

    final WebTarget aTarget = m_aRule.getWebTarget ();
    Response aResponseMsg;

    // GET
    _testResponse (aTarget.path (sPI_LC).request ().get (), 404);
    _testResponse (aTarget.path (sPI_UC).request ().get (), 404);

    try
    {
      // PUT 1 - create
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                             .request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
      _testResponse (aResponseMsg, 200);

      // Both regular and upper case must work
      assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
      assertNotNull (aTarget.path (sPI_UC).request ().get (ServiceGroupType.class));
      assertTrue (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_LC));
      assertTrue (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_UC));

      // PUT 2 - overwrite
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                             .request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
      _testResponse (aResponseMsg, 200);
      aResponseMsg = _addCredentials (aTarget.path (sPI_UC)
                                             .request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
      _testResponse (aResponseMsg, 200);

      // Both regular and upper case must work
      assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
      assertNotNull (aTarget.path (sPI_UC).request ().get (ServiceGroupType.class));
      assertTrue (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_LC));
      assertTrue (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_UC));

      // DELETE 1
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC).request ()).delete ();
      _testResponse (aResponseMsg, 200);

      // Both must be deleted
      _testResponse (aTarget.path (sPI_LC).request ().get (), 404);
      _testResponse (aTarget.path (sPI_UC).request ().get (), 404);
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_UC));
    }
    finally
    {
      // DELETE 2
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC).request ()).delete ();
      // May be 500 if no MySQL is running
      _testResponse (aResponseMsg, 200, 404);

      // Both must be deleted
      _testResponse (aTarget.path (sPI_LC).request ().get (), 404);
      _testResponse (aTarget.path (sPI_UC).request ().get (), 404);
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_UC));
    }
  }
}
