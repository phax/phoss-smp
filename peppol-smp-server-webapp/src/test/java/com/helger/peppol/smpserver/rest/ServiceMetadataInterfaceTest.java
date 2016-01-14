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
import static org.junit.Assert.assertNull;
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
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.process.EPredefinedProcessIdentifier;
import com.helger.peppol.identifier.process.SimpleProcessIdentifier;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smp.ObjectFactory;
import com.helger.peppol.smp.ProcessListType;
import com.helger.peppol.smp.ProcessType;
import com.helger.peppol.smp.RedirectType;
import com.helger.peppol.smp.ServiceEndpointList;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smp.ServiceInformationType;
import com.helger.peppol.smp.ServiceMetadataType;
import com.helger.peppol.smpserver.data.xml.mgr.XMLServiceGroupManager;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.photon.security.CSecurity;
import com.helger.web.http.CHTTPHeader;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * Test class for class {@link ServiceMetadataInterface}
 *
 * @author Philip Helger
 */
@RunWith (Parameterized.class)
public final class ServiceMetadataInterfaceTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ServiceMetadataInterfaceTest.class);

  @Parameters (name = "{index}.: {0}")
  public static Collection <String> data ()
  {
    return CollectionHelper.newList ("test-smp-server-xml.properties", "test-smp-server-sql.properties");
  }

  @Rule
  public final SMPServerRESTTestRule m_aRule;

  public ServiceMetadataInterfaceTest (@Nonnull final String sClassPath)
  {
    m_aRule = new SMPServerRESTTestRule (new ClassPathResource (sClassPath).getAsFile ().getAbsolutePath ());
  }

  private final ObjectFactory m_aObjFactory = new ObjectFactory ();

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
    return aBuilder.header (CHTTPHeader.AUTHORIZATION, new BasicAuthClientCredentials ("peppol_user", "Test1234").getRequestValue ());
  }

  private static void _testResponse (final Response aResponseMsg, final int... aStatusCodes)
  {
    ValueEnforcer.notNull (aResponseMsg, "ResponseMsg");
    ValueEnforcer.notEmpty (aStatusCodes, "StatusCodes");

    assertNotNull (aResponseMsg);
    // Read response
    final String sResponse = aResponseMsg.readEntity (String.class);
    if (StringHelper.hasText (sResponse))
      s_aLogger.info ("HTTP Response: " + sResponse);
    assertTrue (Arrays.toString (aStatusCodes) +
                " does not contain " +
                aResponseMsg.getStatus (),
                ArrayHelper.contains (aStatusCodes, aResponseMsg.getStatus ()));
  }

  @Test
  public void testCreateAndDeleteServiceInformation ()
  {
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:xxx");
    final String sPI = aPI.getURIEncoded ();

    final SimpleDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
    final String sDT = aDT.getURIEncoded ();

    final SimpleProcessIdentifier aProcID = EPredefinedProcessIdentifier.BIS4A_V20.getAsProcessIdentifier ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (aPI);

    final ServiceMetadataType aSM = new ServiceMetadataType ();
    final ServiceInformationType aSI = new ServiceInformationType ();
    aSI.setParticipantIdentifier (aPI);
    aSI.setDocumentIdentifier (aDT);
    {
      final ProcessListType aPL = new ProcessListType ();
      final ProcessType aProcess = new ProcessType ();
      aProcess.setProcessIdentifier (aProcID);
      final ServiceEndpointList aSEL = new ServiceEndpointList ();
      final EndpointType aEndpoint = new EndpointType ();
      aEndpoint.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference ("http://test.smpserver/as2"));
      aEndpoint.setRequireBusinessLevelSignature (false);
      aEndpoint.setCertificate ("blacert");
      aEndpoint.setServiceDescription ("Unit test service");
      aEndpoint.setTechnicalContactUrl ("https://github.com/phax/peppol-smp-server");
      aEndpoint.setTransportProfile (ESMPTransportProfile.TRANSPORT_PROFILE_AS2.getID ());
      aSEL.addEndpoint (aEndpoint);
      aProcess.setServiceEndpointList (aSEL);
      aPL.addProcess (aProcess);
      aSI.setProcessList (aPL);
    }
    aSM.setServiceInformation (aSI);

    final WebTarget aTarget = m_aRule.getWebTarget ();
    Response aResponseMsg;

    try
    {
      _testResponse (aTarget.path (sPI).request ().get (), 404);

      // PUT ServiceGroup
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
      _testResponse (aResponseMsg, 200);

      assertNotNull (aTarget.path (sPI).request ().get (ServiceGroupType.class));
      final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aPI);
      assertNotNull (aServiceGroup);

      try
      {
        // PUT 1 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI)
                                               .path ("services")
                                               .path (sDT)
                                               .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
        _testResponse (aResponseMsg, 200);
        assertNotNull (SMPMetaManager.getServiceInformationMgr ().getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));

        // PUT 2 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI)
                                               .path ("services")
                                               .path (sDT)
                                               .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
        _testResponse (aResponseMsg, 200);
        assertNotNull (SMPMetaManager.getServiceInformationMgr ().getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));

        // DELETE 1 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI).path ("services").path (sDT).request ()).delete ();
        _testResponse (aResponseMsg, 200);
        assertNull (SMPMetaManager.getServiceInformationMgr ().getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));
      }
      finally
      {
        // DELETE 2 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI).path ("services").path (sDT).request ()).delete ();
        _testResponse (aResponseMsg, 200, 400);
        assertNull (SMPMetaManager.getServiceInformationMgr ().getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));
      }

      assertNotNull (aTarget.path (sPI).request ().get (ServiceGroupType.class));
    }
    finally
    {
      // DELETE ServiceGroup
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).delete ();
      _testResponse (aResponseMsg, 200, 404);

      _testResponse (aTarget.path (sPI).request ().get (), 404);
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI));
    }
  }

  @Test
  public void testCreateAndDeleteRedirect ()
  {
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:xxx");
    final String sPI = aPI.getURIEncoded ();

    final SimpleDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
    final String sDT = aDT.getURIEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (aPI);

    final ServiceMetadataType aSM = new ServiceMetadataType ();
    final RedirectType aRedir = new RedirectType ();
    aRedir.setHref ("http://other-smp.domain.xyz");
    aRedir.setCertificateUID ("APP_0000000000000");
    aSM.setRedirect (aRedir);

    final WebTarget aTarget = m_aRule.getWebTarget ();
    Response aResponseMsg;

    try
    {
      _testResponse (aTarget.path (sPI).request ().get (), 404);

      // PUT ServiceGroup
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
      _testResponse (aResponseMsg, 200);

      assertNotNull (aTarget.path (sPI).request ().get (ServiceGroupType.class));
      final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aPI);
      assertNotNull (aServiceGroup);

      try
      {
        // PUT 1 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI)
                                               .path ("services")
                                               .path (sDT)
                                               .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
        _testResponse (aResponseMsg, 200);
        assertNotNull (SMPMetaManager.getRedirectMgr ().getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));

        // PUT 2 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI)
                                               .path ("services")
                                               .path (sDT)
                                               .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
        _testResponse (aResponseMsg, 200);
        assertNotNull (SMPMetaManager.getRedirectMgr ().getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));

        // DELETE 1 Redirect
        aResponseMsg = _addCredentials (aTarget.path (sPI).path ("services").path (sDT).request ()).delete ();
        _testResponse (aResponseMsg, 200);
        assertNull (SMPMetaManager.getRedirectMgr ().getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));
      }
      finally
      {
        // DELETE 2 Redirect
        aResponseMsg = _addCredentials (aTarget.path (sPI).path ("services").path (sDT).request ()).delete ();
        _testResponse (aResponseMsg, 200, 400);
        assertNull (SMPMetaManager.getRedirectMgr ().getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));
      }

      assertNotNull (aTarget.path (sPI).request ().get (ServiceGroupType.class));
    }
    finally
    {
      // DELETE ServiceGroup
      aResponseMsg = _addCredentials (aTarget.path (sPI).request ()).delete ();
      _testResponse (aResponseMsg, 200, 404);

      _testResponse (aTarget.path (sPI).request ().get (), 404);
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI));
    }
  }
}
