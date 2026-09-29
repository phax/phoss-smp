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

import java.time.Month;
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

import com.helger.base.array.ArrayHelper;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.datetime.helper.PDTFactory;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.io.resource.FileSystemResource;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.PeppolDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.peppolid.peppol.process.PeppolProcessIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.jaxb.SmpEndpointMarshaller;
import com.helger.phoss.smp.mock.MockHttpClient;
import com.helger.phoss.smp.mock.MockHttpResponse;
import com.helger.phoss.smp.mock.MockSMPClient;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.smpclient.exception.SMPClientException;
import com.helger.smpclient.exception.SMPClientNotFoundException;
import com.helger.smpclient.peppol.SMPClient;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceGroupType;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceMetadataType;
import com.helger.smpclient.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.xsds.peppol.smp1.EndpointType;
import com.helger.xsds.peppol.smp1.ProcessListType;
import com.helger.xsds.peppol.smp1.ProcessType;
import com.helger.xsds.peppol.smp1.RedirectType;
import com.helger.xsds.peppol.smp1.ServiceEndpointList;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceInformationType;
import com.helger.xsds.peppol.smp1.ServiceMetadataReferenceCollectionType;
import com.helger.xsds.peppol.smp1.ServiceMetadataType;

/**
 * Test class for class {@link SMPRestFilter}.
 *
 * @author Philip Helger
 */
public final class ServiceMetadataInterfaceTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceMetadataInterfaceTest.class);
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
  private static HttpEntity _xmlEntity (@NonNull final ServiceMetadataType aSM)
  {
    return MockHttpClient.createXMLEntity (new SMPMarshallerServiceMetadataType (), aSM);
  }

  @NonNull
  private static EndpointType _createEndpoint (@NonNull final String sTransportProfile, @NonNull final String sURL)
  {
    final EndpointType ret = new EndpointType ();
    ret.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference (sURL));
    ret.setRequireBusinessLevelSignature (false);
    ret.setCertificate ("blacert");
    ret.setServiceDescription ("Unit test service");
    ret.setTechnicalContactUrl ("https://github.com/phax/phoss-smp");
    ret.setTransportProfile (sTransportProfile);
    return ret;
  }

  @NonNull
  private static HttpEntity _endpointEntity (@NonNull final String sTransportProfile, @NonNull final String sURL)
  {
    final String sXML = new SmpEndpointMarshaller ().getAsString (_createEndpoint (sTransportProfile, sURL));
    assertNotNull (sXML);
    return MockHttpClient.createXMLEntity (sXML);
  }

  @NonNull
  private static RedirectType _createRedirect ()
  {
    final RedirectType aRedir = new RedirectType ();
    aRedir.setHref ("http://other-smp.domain.xyz");
    aRedir.setCertificateUID ("APP_0000000000000");
    return aRedir;
  }

  private static int _testResponse (final MockHttpResponse aResponseMsg, final int... aStatusCodes)
  {
    ValueEnforcer.notNull (aResponseMsg, "ResponseMsg");
    ValueEnforcer.notEmpty (aStatusCodes, "StatusCodes");

    // Read response
    final String sResponse = aResponseMsg.getBodyAsString ();
    if (StringHelper.isNotEmpty (sResponse))
      LOGGER.info ("HTTP Response: " + sResponse);
    assertTrue (Arrays.toString (aStatusCodes) + " does not contain " + aResponseMsg.getStatusCode (),
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
  public void testCreateAndDeleteServiceInformationHttpClient ()
  {
    // Lower case
    final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
    final String sPI_LC = aPI_LC.getURIPercentEncoded ();
    // Upper case
    final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:XXX");
    final String sPI_UC = aPI_UC.getURIPercentEncoded ();

    final PeppolDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();
    final String sDT = aDT.getURIPercentEncoded ();

    final PeppolProcessIdentifier aProcID = EPredefinedProcessIdentifier.BIS3_BILLING.getAsProcessIdentifier ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ServiceMetadataType aSM = new ServiceMetadataType ();
    final ServiceInformationType aSI = new ServiceInformationType ();
    aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
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
      aEndpoint.setTechnicalContactUrl ("https://github.com/phax/phoss-smp");
      aEndpoint.setTransportProfile (ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2.getID ());
      aSEL.addEndpoint (aEndpoint);
      aProcess.setServiceEndpointList (aSEL);
      aPL.addProcess (aProcess);
      aSI.setProcessList (aPL);
    }
    aSM.setServiceInformation (aSI);

    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aSIMgr = SMPMetaManager.getServiceInformationMgr ();
    MockHttpResponse aResponseMsg;

    _testResponse (m_aClient.get (sPI_LC), 404);
    _testResponse (m_aClient.get (sPI_UC), 404);

    try
    {
      // PUT ServiceGroup
      aResponseMsg = m_aClient.put (sPI_LC, CREDENTIALS, _xmlEntity (aSG));
      _testResponse (aResponseMsg, 200);

      // Read both
      assertNotNull (_getServiceGroup (sPI_LC));
      assertNotNull (_getServiceGroup (sPI_UC));

      final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (aPI_LC);
      assertNotNull (aServiceGroup);
      final ISMPServiceGroup aServiceGroup_UC = aSGMgr.getSMPServiceGroupOfID (aPI_UC);
      assertEquals (aServiceGroup, aServiceGroup_UC);

      try
      {
        // PUT 1 ServiceInformation
        aResponseMsg = m_aClient.put (sPI_LC + "/services/" + sDT, CREDENTIALS, _xmlEntity (aSM));
        _testResponse (aResponseMsg, 200);
        assertNotNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT));

        // PUT 2 ServiceInformation
        aResponseMsg = m_aClient.put (sPI_LC + "/services/" + sDT, CREDENTIALS, _xmlEntity (aSM));
        _testResponse (aResponseMsg, 200);
        assertNotNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT));

        // DELETE 1 ServiceInformation
        aResponseMsg = m_aClient.delete (sPI_LC + "/services/" + sDT, CREDENTIALS);
        _testResponse (aResponseMsg, 200);
        assertNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT));
      }
      finally
      {
        // DELETE 2 ServiceInformation
        aResponseMsg = m_aClient.delete (sPI_LC + "/services/" + sDT, CREDENTIALS);
        _testResponse (aResponseMsg, 200, 404);
        assertNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT));
      }

      assertNotNull (_getServiceGroup (sPI_LC));
    }
    finally
    {
      // DELETE ServiceGroup
      aResponseMsg = m_aClient.delete (sPI_LC, CREDENTIALS);
      // May be 500 if no MySQL is running
      _testResponse (aResponseMsg, 200, 404);

      _testResponse (m_aClient.get (sPI_LC), 404);
      _testResponse (m_aClient.get (sPI_UC), 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));
    }
  }

  @Test
  public void testCreateAndDeleteServiceInformationSMPClient () throws SMPClientException
  {
    // Lower case
    final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
    // Upper case
    final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:XXX");

    final PeppolDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();

    final PeppolProcessIdentifier aProcID = EPredefinedProcessIdentifier.BIS3_BILLING.getAsProcessIdentifier ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ServiceInformationType aSI = new ServiceInformationType ();
    aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
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
      aEndpoint.setTechnicalContactUrl ("https://github.com/phax/phoss-smp");
      aEndpoint.setTransportProfile (ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2.getID ());
      aSEL.addEndpoint (aEndpoint);
      aProcess.setServiceEndpointList (aSEL);
      aPL.addProcess (aProcess);
      aSI.setProcessList (aPL);
    }

    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aSIMgr = SMPMetaManager.getServiceInformationMgr ();
    final SMPClient aSMPClient = new MockSMPClient ();

    assertNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
    assertNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
    assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
    assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

    try
    {
      // PUT ServiceGroup
      aSMPClient.saveServiceGroup (aSG, CREDENTIALS);

      // Read both
      assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
      assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

      final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (aPI_LC);
      assertNotNull (aServiceGroup);
      final ISMPServiceGroup aServiceGroup_UC = aSGMgr.getSMPServiceGroupOfID (aPI_UC);
      assertEquals (aServiceGroup, aServiceGroup_UC);

      try
      {
        // PUT 1 ServiceInformation
        aSMPClient.saveServiceInformation (aSI, CREDENTIALS);
        assertNotNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT));

        // PUT 2 ServiceInformation
        aSMPClient.saveServiceInformation (aSI, CREDENTIALS);
        assertNotNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT));

        // DELETE 1 ServiceInformation
        aSMPClient.deleteServiceRegistration (aPI_LC, aDT, CREDENTIALS);
        assertNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT));
      }
      finally
      {
        // DELETE 2 ServiceInformation
        try
        {
          aSMPClient.deleteServiceRegistration (aPI_LC, aDT, CREDENTIALS);
        }
        catch (final SMPClientNotFoundException ex)
        {
          // Expected
        }
        assertNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT));
      }

      assertNotNull (aSMPClient.getServiceGroup (aPI_LC));
    }
    finally
    {
      // DELETE ServiceGroup
      try
      {
        aSMPClient.deleteServiceGroup (aPI_LC, CREDENTIALS);
      }
      catch (final SMPClientNotFoundException ex)
      {
        // Expected
      }

      assertNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
      assertNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));
    }
  }

  @Test
  public void testCreateAndDeleteRedirectHttpClient ()
  {
    // Lower case
    final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
    final String sPI_LC = aPI_LC.getURIPercentEncoded ();
    // Upper case
    final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:XXX");
    final String sPI_UC = aPI_UC.getURIPercentEncoded ();

    final IDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();
    final String sDT = aDT.getURIPercentEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ServiceMetadataType aSM = new ServiceMetadataType ();
    aSM.setRedirect (_createRedirect ());

    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPRedirectManager aSRMgr = SMPMetaManager.getRedirectMgr ();
    MockHttpResponse aResponseMsg;

    _testResponse (m_aClient.get (sPI_LC), 404);
    _testResponse (m_aClient.get (sPI_UC), 404);

    try
    {
      // PUT ServiceGroup
      aResponseMsg = m_aClient.put (sPI_LC, CREDENTIALS, _xmlEntity (aSG));
      _testResponse (aResponseMsg, 200);

      assertNotNull (_getServiceGroup (sPI_LC));
      assertNotNull (_getServiceGroup (sPI_UC));

      final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (aPI_LC);
      assertNotNull (aServiceGroup);
      final ISMPServiceGroup aServiceGroup_UC = aSGMgr.getSMPServiceGroupOfID (aPI_UC);
      assertEquals (aServiceGroup, aServiceGroup_UC);

      try
      {
        // PUT 1 ServiceInformation
        aResponseMsg = m_aClient.put (sPI_LC + "/services/" + sDT, CREDENTIALS, _xmlEntity (aSM));
        _testResponse (aResponseMsg, 200);
        assertNotNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI_LC, aDT));

        // PUT 2 ServiceInformation
        aResponseMsg = m_aClient.put (sPI_LC + "/services/" + sDT, CREDENTIALS, _xmlEntity (aSM));
        _testResponse (aResponseMsg, 200);
        assertNotNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI_LC, aDT));

        // DELETE 1 Redirect
        aResponseMsg = m_aClient.delete (sPI_LC + "/services/" + sDT, CREDENTIALS);
        _testResponse (aResponseMsg, 200);
        assertNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI_LC, aDT));
      }
      finally
      {
        // DELETE 2 Redirect
        aResponseMsg = m_aClient.delete (sPI_LC + "/services/" + sDT, CREDENTIALS);
        _testResponse (aResponseMsg, 200, 404);
        assertNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI_LC, aDT));
      }

      assertNotNull (_getServiceGroup (sPI_LC));
    }
    finally
    {
      // DELETE ServiceGroup
      aResponseMsg = m_aClient.delete (sPI_LC, CREDENTIALS);
      _testResponse (aResponseMsg, 200, 404);

      _testResponse (m_aClient.get (sPI_LC), 404);
      _testResponse (m_aClient.get (sPI_UC), 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));
    }
  }

  @Test
  public void testCreateAndDeleteRedirectSMPClient () throws SMPClientException
  {
    // Lower case
    final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
    // Upper case
    final IParticipantIdentifier aPI_UC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:XXX");
    final IDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final RedirectType aRedir = _createRedirect ();

    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPRedirectManager aSRMgr = SMPMetaManager.getRedirectMgr ();
    final SMPClient aSMPClient = new MockSMPClient ();

    assertNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
    assertNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
    assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
    assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

    try
    {
      // PUT ServiceGroup
      aSMPClient.saveServiceGroup (aSG, CREDENTIALS);

      assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
      assertNotNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));

      final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (aPI_LC);
      assertNotNull (aServiceGroup);
      final ISMPServiceGroup aServiceGroup_UC = aSGMgr.getSMPServiceGroupOfID (aPI_UC);
      assertEquals (aServiceGroup, aServiceGroup_UC);

      try
      {
        // PUT 1 ServiceInformation
        aSMPClient.saveServiceRedirect (aPI_LC, aDT, aRedir, CREDENTIALS);
        assertNotNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI_LC, aDT));

        // PUT 2 ServiceInformation
        aSMPClient.saveServiceRedirect (aPI_LC, aDT, aRedir, CREDENTIALS);
        assertNotNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI_LC, aDT));

        // DELETE 1 Redirect
        aSMPClient.deleteServiceRegistration (aPI_LC, aDT, CREDENTIALS);
        assertNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI_LC, aDT));
      }
      finally
      {
        // DELETE 2 Redirect
        try
        {
          aSMPClient.deleteServiceRegistration (aPI_LC, aDT, CREDENTIALS);
        }
        catch (final SMPClientNotFoundException ex)
        {
          // Expected
        }
        assertNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI_LC, aDT));
      }

      assertNotNull (aSGMgr.getSMPServiceGroupOfID (aPI_LC));
    }
    finally
    {
      // DELETE ServiceGroup
      try
      {
        aSMPClient.deleteServiceGroup (aPI_LC, CREDENTIALS);
      }
      catch (final SMPClientNotFoundException ex)
      {
        // Expected
      }

      assertNull (aSMPClient.getServiceGroupOrNull (aPI_LC));
      assertNull (aSMPClient.getServiceGroupOrNull (aPI_UC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_UC));
    }
  }

  @Test
  public void testRemoveAllContentOfServiceGroup ()
  {
    // Lower case
    final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
    final String sPI_LC = aPI_LC.getURIPercentEncoded ();

    final IDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();
    final String sDT = aDT.getURIPercentEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ServiceMetadataType aSM = new ServiceMetadataType ();
    aSM.setRedirect (_createRedirect ());

    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPRedirectManager aSRMgr = SMPMetaManager.getRedirectMgr ();
    MockHttpResponse aResponseMsg;

    _testResponse (m_aClient.get (sPI_LC), 404);

    try
    {
      // PUT ServiceGroup
      aResponseMsg = m_aClient.put (sPI_LC, CREDENTIALS, _xmlEntity (aSG));
      _testResponse (aResponseMsg, 200);

      // GET ServiceGroup
      assertNotNull (_getServiceGroup (sPI_LC));
      final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (aPI_LC);
      assertNotNull (aServiceGroup);

      try
      {
        // PUT 1 ServiceInformation
        aResponseMsg = m_aClient.put (sPI_LC + "/services/" + sDT, CREDENTIALS, _xmlEntity (aSM));
        _testResponse (aResponseMsg, 200);
        assertNotNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI_LC, aDT));

        // DELETE 1 Redirect
        aResponseMsg = m_aClient.delete (sPI_LC + "/services", CREDENTIALS);
        _testResponse (aResponseMsg, 200);
        assertNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI_LC, aDT));
      }
      finally
      {
        // DELETE 2 Redirect
        aResponseMsg = m_aClient.delete (sPI_LC + "/services", CREDENTIALS);
        _testResponse (aResponseMsg, 200, 404);
        assertNull (aSRMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI_LC, aDT));
      }

      assertNotNull (_getServiceGroup (sPI_LC));
    }
    finally
    {
      // DELETE ServiceGroup
      aResponseMsg = m_aClient.delete (sPI_LC, CREDENTIALS);
      _testResponse (aResponseMsg, 200, 404);

      _testResponse (m_aClient.get (sPI_LC), 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
    }
  }

  @Test
  public void testOverlappingEndpointValidityPeriods ()
  {
    // Lower case
    final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
    final String sPI_LC = aPI_LC.getURIPercentEncoded ();

    final PeppolDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();
    final String sDT = aDT.getURIPercentEncoded ();

    final PeppolProcessIdentifier aProcID = EPredefinedProcessIdentifier.BIS3_BILLING.getAsProcessIdentifier ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ServiceMetadataType aSM = new ServiceMetadataType ();
    final ServiceInformationType aSI = new ServiceInformationType ();
    aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
    aSI.setDocumentIdentifier (aDT);
    {
      final ProcessListType aPL = new ProcessListType ();
      final ProcessType aProcess = new ProcessType ();
      aProcess.setProcessIdentifier (aProcID);
      final ServiceEndpointList aSEL = new ServiceEndpointList ();
      {
        final EndpointType aEndpoint = new EndpointType ();
        aEndpoint.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference ("http://test.smpserver/as2"));
        aEndpoint.setRequireBusinessLevelSignature (false);
        aEndpoint.setServiceActivationDate (PDTFactory.createLocalDateTime (2026, Month.JANUARY, 1));
        aEndpoint.setServiceExpirationDate (PDTFactory.createLocalDateTime (2026, Month.JANUARY, 31));
        aEndpoint.setCertificate ("blacert");
        aEndpoint.setServiceDescription ("Unit test service");
        aEndpoint.setTechnicalContactUrl ("https://github.com/phax/phoss-smp");
        aEndpoint.setTransportProfile (ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2.getID ());
        aSEL.addEndpoint (aEndpoint);
      }
      {
        final EndpointType aEndpoint = new EndpointType ();
        aEndpoint.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference ("http://test.smpserver/as3"));
        aEndpoint.setRequireBusinessLevelSignature (false);
        // Has an overlap
        aEndpoint.setServiceActivationDate (PDTFactory.createLocalDateTime (2026, Month.JANUARY, 20));
        aEndpoint.setServiceExpirationDate (PDTFactory.createLocalDateTime (2026, Month.MARCH, 13));
        aEndpoint.setCertificate ("blacert");
        aEndpoint.setServiceDescription ("Unit test service");
        aEndpoint.setTechnicalContactUrl ("https://github.com/phax/phoss-smp");
        aEndpoint.setTransportProfile (ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2.getID ());
        aSEL.addEndpoint (aEndpoint);
      }
      aProcess.setServiceEndpointList (aSEL);
      aPL.addProcess (aProcess);
      aSI.setProcessList (aPL);
    }
    aSM.setServiceInformation (aSI);

    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aSIMgr = SMPMetaManager.getServiceInformationMgr ();
    MockHttpResponse aResponseMsg;

    try
    {
      // PUT ServiceGroup
      aResponseMsg = m_aClient.put (sPI_LC, CREDENTIALS, _xmlEntity (aSG));
      _testResponse (aResponseMsg, 200);

      // Read both
      assertNotNull (_getServiceGroup (sPI_LC));

      final ISMPServiceGroup aServiceGroup = aSGMgr.getSMPServiceGroupOfID (aPI_LC);
      assertNotNull (aServiceGroup);

      // PUT 1 ServiceInformation
      aResponseMsg = m_aClient.put (sPI_LC + "/services/" + sDT, CREDENTIALS, _xmlEntity (aSM));
      _testResponse (aResponseMsg, 400);
      assertNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT));
    }
    finally
    {
      // DELETE ServiceGroup
      aResponseMsg = m_aClient.delete (sPI_LC, CREDENTIALS);
      // May be 500 if no MySQL is running
      _testResponse (aResponseMsg, 200, 404);

      _testResponse (m_aClient.get (sPI_LC), 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
    }
  }

  @Test
  public void testAddSingleEndpointHttpClient ()
  {
    final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
    final String sPI_LC = aPI_LC.getURIPercentEncoded ();

    final PeppolDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();
    final String sDT = aDT.getURIPercentEncoded ();

    final PeppolProcessIdentifier aProcID = EPredefinedProcessIdentifier.BIS3_BILLING.getAsProcessIdentifier ();
    final String sProcID = aProcID.getURIPercentEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aSIMgr = SMPMetaManager.getServiceInformationMgr ();
    MockHttpResponse aResponseMsg;

    try
    {
      // PUT ServiceGroup
      aResponseMsg = m_aClient.put (sPI_LC, CREDENTIALS, _xmlEntity (aSG));
      _testResponse (aResponseMsg, 200);

      try
      {
        // PUT Endpoint 1 - creates Service Information and Process on the fly
        aResponseMsg = m_aClient.put (sPI_LC + "/services/" + sDT + "/" + sProcID,
                                      CREDENTIALS,
                                      _endpointEntity (ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2.getID (),
                                                       "http://test.smpserver/as4"));
        _testResponse (aResponseMsg, 200);

        ISMPServiceInformation aSI = aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT);
        assertNotNull (aSI);
        assertEquals (1, aSI.getProcessCount ());
        assertEquals (1, aSI.getProcessOfID (aProcID).getEndpointCount ());

        // PUT Endpoint 2 - different Transport Profile, must be added to the
        // existing Endpoint
        aResponseMsg = m_aClient.put (sPI_LC + "/services/" + sDT + "/" + sProcID,
                                      CREDENTIALS,
                                      _endpointEntity (ESMPTransportProfile.TRANSPORT_PROFILE_AS2.getID (),
                                                       "http://test.smpserver/as2"));
        _testResponse (aResponseMsg, 200);

        aSI = aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT);
        assertNotNull (aSI);
        assertEquals (1, aSI.getProcessCount ());
        assertEquals (2, aSI.getProcessOfID (aProcID).getEndpointCount ());

        // PUT Endpoint 3 - same Transport Profile as Endpoint 1 and overlapping
        // validity, must be rejected
        aResponseMsg = m_aClient.put (sPI_LC + "/services/" + sDT + "/" + sProcID,
                                      CREDENTIALS,
                                      _endpointEntity (ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2.getID (),
                                                       "http://test.smpserver/as4-other"));
        _testResponse (aResponseMsg, 400);

        aSI = aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT);
        assertNotNull (aSI);
        assertEquals (2, aSI.getProcessOfID (aProcID).getEndpointCount ());

        // PUT Endpoint 4 - no Endpoint at all, must be rejected
        aResponseMsg = m_aClient.put (sPI_LC + "/services/" + sDT + "/" + sProcID,
                                      CREDENTIALS,
                                      MockHttpClient.createXMLEntity ("<Bogus xmlns=\"urn:phoss:smp:test\" />"));
        _testResponse (aResponseMsg, 400);
      }
      finally
      {
        // DELETE ServiceInformation
        aResponseMsg = m_aClient.delete (sPI_LC + "/services/" + sDT, CREDENTIALS);
        _testResponse (aResponseMsg, 200, 404);
        assertNull (aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI_LC, aDT));
      }
    }
    finally
    {
      // DELETE ServiceGroup
      aResponseMsg = m_aClient.delete (sPI_LC, CREDENTIALS);
      _testResponse (aResponseMsg, 200, 404);
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI_LC));
    }
  }
}
