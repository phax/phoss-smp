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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smp.ObjectFactory;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.web.http.CHTTPHeader;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * Test class for class {@link ServiceGroupInterface}
 *
 * @author Philip Helger
 */
@RunWith (Parameterized.class)
public final class ServiceGroupInterfaceTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ServiceGroupInterfaceTest.class);

  @Parameters (name = "{index}.: {0}")
  public static Collection <String> data ()
  {
    return CollectionHelper.newList ("test-smp-server-sql.properties");
  }

  @Rule
  public final SMPServerRESTTestRule m_aRule;

  public ServiceGroupInterfaceTest (@Nonnull final String sClassPath)
  {
    m_aRule = new SMPServerRESTTestRule (new ClassPathResource (sClassPath).getAsFile ().getAbsolutePath ());
  }

  private final ObjectFactory m_aObjFactory = new ObjectFactory ();

  @Nonnull
  private static Builder _addCredentials (@Nonnull final Builder aBuilder)
  {
    // Use default credentials for SQL backend
    return aBuilder.header (CHTTPHeader.AUTHORIZATION,
                            new BasicAuthClientCredentials ("peppol_user", "Test1234").getRequestValue ());
  }

  private static void _testResponse (@Nonnull final Response aResponseMsg, @Nonempty final int... aStatusCodes)
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
  }

  @Test
  public void testCreateAndDeleteServiceGroup ()
  {
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:xxx");
    final String sPI = aPI.getURIEncoded ();
    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (aPI);

    final WebTarget aTarget = m_aRule.getWebTarget ();
    Response aResponseMsg;

    try
    {
      // GET
      _testResponse (aTarget.path (sPI).request ().get (), 404);

      // PUT 1
      aResponseMsg = _addCredentials (aTarget.path (sPI)
                                             .request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
      _testResponse (aResponseMsg, 200);

      assertNotNull (aTarget.path (sPI).request ().get (ServiceGroupType.class));
      assertTrue (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI));

      // PUT 2
      aResponseMsg = _addCredentials (aTarget.path (sPI)
                                             .request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
      _testResponse (aResponseMsg, 200);

      assertNotNull (aTarget.path (sPI).request ().get (ServiceGroupType.class));
      assertTrue (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI));

      // DELETE 1
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).delete ();
      _testResponse (aResponseMsg, 200);

      _testResponse (aTarget.path (sPI).request ().get (), 404);
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI));
    }
    finally
    {
      // DELETE 2
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).delete ();
      _testResponse (aResponseMsg, 200, 404);

      _testResponse (aTarget.path (sPI).request ().get (), 404);
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI));
    }
  }
}
