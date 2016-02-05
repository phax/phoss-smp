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

import static org.junit.Assert.assertEquals;
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
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.web.http.CHTTPHeader;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * Test class for class {@link ServiceMetadataInterface}. This test class is
 * automatically run for XML and SQL backend!
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
    return CollectionHelper.newList ("test-smp-server-sql.properties");
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
    // Use default credentials for SQL backend
    return aBuilder.header (CHTTPHeader.AUTHORIZATION,
                            new BasicAuthClientCredentials ("peppol_user", "Test1234").getRequestValue ());
  }

  private static int _testResponse (final Response aResponseMsg, final int... aStatusCodes)
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
    return aResponseMsg.getStatus ();
  }

  @Test
  public void testCreateAndDeleteServiceInformation ()
  {
    // Lower case
    final SimpleParticipantIdentifier aPI_LC = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:xxx");
    final String sPI_LC = aPI_LC.getURIEncoded ();
    // Upper case
    final SimpleParticipantIdentifier aPI_UC = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:XXX");
    final String sPI_UC = aPI_UC.getURIEncoded ();

    final SimpleDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
    final String sDT = aDT.getURIEncoded ();

    final SimpleProcessIdentifier aProcID = EPredefinedProcessIdentifier.BIS4A_V20.getAsProcessIdentifier ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (aPI_LC);

    final ServiceMetadataType aSM = new ServiceMetadataType ();
    final ServiceInformationType aSI = new ServiceInformationType ();
    aSI.setParticipantIdentifier (aPI_LC);
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

    final int nStatus = _testResponse (aTarget.path (sPI_LC).request ().get (),
                                       m_aRule.isSQLMode () ? new int [] { 404, 500 } : new int [] { 404 });
    if (m_aRule.isSQLMode () && nStatus == 500)
    {
      // Seems like MySQL is not running
      return;
    }
    _testResponse (aTarget.path (sPI_UC).request ().get (), 404);

    try
    {
      // PUT ServiceGroup
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                             .request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
      _testResponse (aResponseMsg, 200);

      // Read both
      assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
      assertNotNull (aTarget.path (sPI_UC).request ().get (ServiceGroupType.class));

      final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aPI_LC);
      assertNotNull (aServiceGroup);
      final ISMPServiceGroup aServiceGroup_UC = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aPI_UC);
      assertEquals (aServiceGroup, aServiceGroup_UC);

      try
      {
        // PUT 1 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                               .path ("services")
                                               .path (sDT)
                                               .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
        _testResponse (aResponseMsg, 200);
        assertNotNull (SMPMetaManager.getServiceInformationMgr ()
                                     .getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));

        // PUT 2 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                               .path ("services")
                                               .path (sDT)
                                               .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
        _testResponse (aResponseMsg, 200);
        assertNotNull (SMPMetaManager.getServiceInformationMgr ()
                                     .getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));

        // DELETE 1 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC).path ("services").path (sDT).request ()).delete ();
        _testResponse (aResponseMsg, 200);
        assertNull (SMPMetaManager.getServiceInformationMgr ()
                                  .getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));
      }
      finally
      {
        // DELETE 2 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC).path ("services").path (sDT).request ()).delete ();
        _testResponse (aResponseMsg, 200, 400);
        assertNull (SMPMetaManager.getServiceInformationMgr ()
                                  .getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDT));
      }

      assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
    }
    finally
    {
      // DELETE ServiceGroup
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC).request ()).delete ();
      _testResponse (aResponseMsg, 200, 404);

      _testResponse (aTarget.path (sPI_LC).request ().get (), 404);
      _testResponse (aTarget.path (sPI_UC).request ().get (), 404);
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_UC));
    }
  }

  @Test
  public void testCreateAndDeleteRedirect ()
  {
    // Lower case
    final SimpleParticipantIdentifier aPI_LC = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:xxx");
    final String sPI_LC = aPI_LC.getURIEncoded ();
    // Upper case
    final SimpleParticipantIdentifier aPI_UC = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:XXX");
    final String sPI_UC = aPI_UC.getURIEncoded ();

    final SimpleDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
    final String sDT = aDT.getURIEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (aPI_LC);

    final ServiceMetadataType aSM = new ServiceMetadataType ();
    final RedirectType aRedir = new RedirectType ();
    aRedir.setHref ("http://other-smp.domain.xyz");
    aRedir.setCertificateUID ("APP_0000000000000");
    aSM.setRedirect (aRedir);

    final WebTarget aTarget = m_aRule.getWebTarget ();
    Response aResponseMsg;

    final int nStatus = _testResponse (aTarget.path (sPI_LC).request ().get (),
                                       m_aRule.isSQLMode () ? new int [] { 404, 500 } : new int [] { 404 });
    if (m_aRule.isSQLMode () && nStatus == 500)
    {
      // Seems like MySQL is not running
      return;
    }

    _testResponse (aTarget.path (sPI_UC).request ().get (), 404);

    try
    {
      // PUT ServiceGroup
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                             .request ()).put (Entity.xml (m_aObjFactory.createServiceGroup (aSG)));
      _testResponse (aResponseMsg, 200);

      assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
      assertNotNull (aTarget.path (sPI_UC).request ().get (ServiceGroupType.class));

      final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aPI_LC);
      assertNotNull (aServiceGroup);
      final ISMPServiceGroup aServiceGroup_UC = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aPI_UC);
      assertEquals (aServiceGroup, aServiceGroup_UC);

      try
      {
        // PUT 1 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                               .path ("services")
                                               .path (sDT)
                                               .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
        _testResponse (aResponseMsg, 200);
        assertNotNull (SMPMetaManager.getRedirectMgr ().getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                     aDT));

        // PUT 2 ServiceInformation
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC)
                                               .path ("services")
                                               .path (sDT)
                                               .request ()).put (Entity.xml (m_aObjFactory.createServiceMetadata (aSM)));
        _testResponse (aResponseMsg, 200);
        assertNotNull (SMPMetaManager.getRedirectMgr ().getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                     aDT));

        // DELETE 1 Redirect
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC).path ("services").path (sDT).request ()).delete ();
        _testResponse (aResponseMsg, 200);
        assertNull (SMPMetaManager.getRedirectMgr ().getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));
      }
      finally
      {
        // DELETE 2 Redirect
        aResponseMsg = _addCredentials (aTarget.path (sPI_LC).path ("services").path (sDT).request ()).delete ();
        _testResponse (aResponseMsg, 200, 400);
        assertNull (SMPMetaManager.getRedirectMgr ().getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDT));
      }

      assertNotNull (aTarget.path (sPI_LC).request ().get (ServiceGroupType.class));
    }
    finally
    {
      // DELETE ServiceGroup
      aResponseMsg = _addCredentials (aTarget.path (sPI_LC).request ()).delete ();
      _testResponse (aResponseMsg, 200, 404);

      _testResponse (aTarget.path (sPI_LC).request ().get (), 404);
      _testResponse (aTarget.path (sPI_UC).request ().get (), 404);
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI_UC));
    }
  }
}
