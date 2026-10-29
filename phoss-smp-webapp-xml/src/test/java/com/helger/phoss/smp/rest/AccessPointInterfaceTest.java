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
import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.io.resource.FileSystemResource;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.mock.MockHttpClient;
import com.helger.phoss.smp.mock.MockHttpResponse;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.phoss.smp.restapi.SMPAccessPointRESTHelper;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;
import com.helger.xml.microdom.serialize.MicroWriter;


/**
 * Test class for the Access Point management REST API - see
 * {@link com.helger.phoss.smp.restapi.AccessPointServerAPI}.
 *
 * @author Philip Helger
 */
public final class AccessPointInterfaceTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AccessPointInterfaceTest.class);
  private static final BasicAuthClientCredentials CREDENTIALS = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                                CSecurity.USER_ADMINISTRATOR_PASSWORD);
  private static final BasicAuthClientCredentials CREDENTIALS_INVALID = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                                        "wrong-password");

  private static final String PATH_LIST = "accesspoint/list";
  private static final String PATH_NAME = "accesspoint/name/";

  private static final String AP_NAME = "rest-ap1";
  private static final String URL1 = "http://localhost/rest-ap1";
  private static final String URL2 = "http://localhost/rest-ap2";
  private static final String CERT1 = "rest-cert1";
  private static final String CERT2 = "rest-cert2";

  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (new FileSystemResource ("src/test/resources/test-smp-server-xml-peppol.properties"));

  private MockHttpClient m_aClient;
  private IIdentifierFactory m_aIF;
  private ISMPAccessPointManager m_aAPMgr;
  private ISMPServiceGroupManager m_aSGMgr;
  private ISMPServiceInformationManager m_aSIMgr;
  private IParticipantIdentifier m_aPI;
  private IDocumentTypeIdentifier m_aDT;
  private IProcessIdentifier m_aProcID;

  @Before
  public void before ()
  {
    m_aClient = new MockHttpClient (m_aRule.getFullURL ());
    m_aIF = SMPMetaManager.getIdentifierFactory ();
    m_aAPMgr = SMPMetaManager.getAccessPointMgr ();
    m_aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    m_aSIMgr = SMPMetaManager.getServiceInformationMgr ();

    m_aPI = m_aIF.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME, "0088:restap");
    m_aDT = m_aIF.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                "xml::xml##restap::1");
    m_aProcID = m_aIF.createProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME, "restproc");

    _cleanup ();
  }

  @After
  public void after ()
  {
    m_aClient.close ();
    _cleanup ();
  }

  private void _cleanup ()
  {
    m_aSGMgr.deleteSMPServiceGroupNoEx (m_aPI, true);
    // The XML backend data survives between the tests
    for (final ISMPAccessPoint aAP : m_aAPMgr.getAllAccessPoints ())
      m_aAPMgr.deleteAccessPoint (aAP.getID ());
  }

  @NonNull
  private static HttpEntity _xml (@NonNull final String sXML)
  {
    return MockHttpClient.createXMLEntity (sXML);
  }

  @NonNull
  private static String _getAccessPointXML (@Nullable final String sEndpointReference, @Nullable final String sCert)
  {
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.addElement (SMPAccessPointRESTHelper.ELEMENT_ACCESS_POINT);
    if (sEndpointReference != null)
      eRoot.addElement (SMPAccessPointRESTHelper.ELEMENT_ENDPOINT_REFERENCE).appendText (sEndpointReference);
    if (sCert != null)
      eRoot.addElement (SMPAccessPointRESTHelper.ELEMENT_CERTIFICATE).appendText (sCert);
    return MicroWriter.getNodeAsString (aDoc);
  }

  @NonNull
  private static String _testResponse (@NonNull final MockHttpResponse aResponseMsg, @Nonempty final int... aStatusCodes)
  {
    ValueEnforcer.notNull (aResponseMsg, "ResponseMsg");
    ValueEnforcer.notEmpty (aStatusCodes, "StatusCodes");

    final String sResponse = aResponseMsg.getBodyAsString ();
    if (StringHelper.isNotEmpty (sResponse))
      LOGGER.info ("HTTP Response: " + sResponse);
    assertTrue (aResponseMsg.getStatusCode () + " is not in " + Arrays.toString (aStatusCodes),
                ArrayHelper.contains (aStatusCodes, aResponseMsg.getStatusCode ()));
    return sResponse;
  }

  @NonNull
  private static IMicroElement _parse (@NonNull final String sResponse)
  {
    final IMicroDocument aDoc = MicroReader.readMicroXML (sResponse);
    assertNotNull ("Response is not XML: " + sResponse, aDoc);
    final IMicroElement eRoot = aDoc.getDocumentElement ();
    assertNotNull (eRoot);
    return eRoot;
  }

  @Test
  public void testCreateReadUpdateDelete ()
  {
    // Not existing - but needs authentication first
    _testResponse (m_aClient.get (PATH_NAME + AP_NAME, CREDENTIALS), 404);

    // Create
    String sResponse = _testResponse (m_aClient.put (PATH_NAME + AP_NAME, CREDENTIALS, _xml (_getAccessPointXML (URL1, CERT1))),
                                      200);
    IMicroElement eRoot = _parse (sResponse);
    assertEquals (SMPAccessPointRESTHelper.ELEMENT_ACCESS_POINT, eRoot.getTagName ());
    assertEquals (AP_NAME, eRoot.getFirstChildElement (SMPAccessPointRESTHelper.ELEMENT_NAME).getTextContent ());
    assertEquals (URL1,
                  eRoot.getFirstChildElement (SMPAccessPointRESTHelper.ELEMENT_ENDPOINT_REFERENCE).getTextContent ());

    ISMPAccessPoint aAP = m_aAPMgr.getAccessPointOfName (AP_NAME);
    assertNotNull (aAP);
    assertEquals (URL1, aAP.getEndpointReference ());
    assertEquals (CERT1, aAP.getCertificate ());

    // Read
    sResponse = _testResponse (m_aClient.get (PATH_NAME + AP_NAME, CREDENTIALS), 200);
    eRoot = _parse (sResponse);
    assertEquals (CERT1, eRoot.getFirstChildElement (SMPAccessPointRESTHelper.ELEMENT_CERTIFICATE).getTextContent ());

    // List
    sResponse = _testResponse (m_aClient.get (PATH_LIST, CREDENTIALS), 200);
    eRoot = _parse (sResponse);
    assertEquals (SMPAccessPointRESTHelper.ELEMENT_ACCESS_POINT_LIST, eRoot.getTagName ());
    assertEquals (1, eRoot.getAllChildElements (SMPAccessPointRESTHelper.ELEMENT_ACCESS_POINT).size ());

    // Update the existing one
    _testResponse (m_aClient.put (PATH_NAME + AP_NAME, CREDENTIALS, _xml (_getAccessPointXML (URL2, CERT2))), 200);
    aAP = m_aAPMgr.getAccessPointOfName (AP_NAME);
    assertNotNull (aAP);
    assertEquals (URL2, aAP.getEndpointReference ());
    assertEquals (CERT2, aAP.getCertificate ());
    // Still only one Access Point
    assertEquals (1, m_aAPMgr.getAccessPointCount ());

    // Delete
    _testResponse (m_aClient.delete (PATH_NAME + AP_NAME, CREDENTIALS), 200);
    assertNull (m_aAPMgr.getAccessPointOfName (AP_NAME));

    // Delete again - not found
    _testResponse (m_aClient.delete (PATH_NAME + AP_NAME, CREDENTIALS), 404);
  }

  @Test
  public void testAuthenticationRequired ()
  {
    // No credentials at all
    _testResponse (m_aClient.get (PATH_LIST), 403);
    _testResponse (m_aClient.get (PATH_NAME + AP_NAME), 403);
    _testResponse (m_aClient.put (PATH_NAME + AP_NAME, (String) null, _xml (_getAccessPointXML (URL1, CERT1))), 403);
    _testResponse (m_aClient.delete (PATH_NAME + AP_NAME, (String) null), 403);

    // Invalid credentials
    _testResponse (m_aClient.put (PATH_NAME + AP_NAME, CREDENTIALS_INVALID, _xml (_getAccessPointXML (URL1, CERT1))),
                   403);

    // Nothing was created
    assertEquals (0, m_aAPMgr.getAccessPointCount ());
  }

  @Test
  public void testCreateInvalidData ()
  {
    // No endpoint reference
    _testResponse (m_aClient.put (PATH_NAME + AP_NAME, CREDENTIALS, _xml (_getAccessPointXML (null, CERT1))), 400);
    // No certificate
    _testResponse (m_aClient.put (PATH_NAME + AP_NAME, CREDENTIALS, _xml (_getAccessPointXML (URL1, null))), 400);
    // Not XML at all
    _testResponse (m_aClient.put (PATH_NAME + AP_NAME, CREDENTIALS, _xml ("this is no XML")),
                   400);

    assertEquals (0, m_aAPMgr.getAccessPointCount ());
  }

  @Test
  public void testUseForMatchingEndpoints () throws Exception
  {
    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);
    m_aSGMgr.createSMPServiceGroup (aTestUser.getID (), m_aPI, null, null, true);

    // The endpoint contains URL1 and CERT1 directly
    final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
    final SMPEndpoint aEP = new SMPEndpoint (GlobalIDFactory.getNewPersistentStringID (),
                                             "tp",
                                             URL1,
                                             false,
                                             "minauth",
                                             aStartDT,
                                             aStartDT.plusYears (1),
                                             CERT1,
                                             "sd",
                                             "tc",
                                             "ti",
                                             null);
    final SMPProcess aProcess = new SMPProcess (m_aProcID, new CommonsArrayList <> (aEP), null);
    assertTrue (m_aSIMgr.mergeSMPServiceInformation (new SMPServiceInformation (m_aPI,
                                                                                m_aDT,
                                                                                new CommonsArrayList <> (aProcess),
                                                                                null)).isSuccess ());
    assertNull (_getEndpoint ().getAccessPointID ());

    // Create the matching Access Point
    _testResponse (m_aClient.put (PATH_NAME + AP_NAME, CREDENTIALS, _xml (_getAccessPointXML (URL1, CERT1))), 200);

    // Not authenticated
    _testResponse (m_aClient.post (PATH_NAME + AP_NAME + "/use-for-matching-endpoints", (String) null), 403);

    // Let all matching endpoints use the Access Point
    final String sResponse = _testResponse (m_aClient.post (PATH_NAME + AP_NAME + "/use-for-matching-endpoints", CREDENTIALS), 200);
    final IMicroElement eRoot = _parse (sResponse);
    assertEquals ("1", eRoot.getAttributeValue ("changedendpoints"));

    // The endpoint now references the Access Point instead of containing the data
    final ISMPAccessPoint aAP = m_aAPMgr.getAccessPointOfName (AP_NAME);
    assertNotNull (aAP);
    final ISMPEndpoint aChangedEP = _getEndpoint ();
    assertEquals (aAP.getID (), aChangedEP.getAccessPointID ());

    // A referenced Access Point may not be deleted
    _testResponse (m_aClient.delete (PATH_NAME + AP_NAME, CREDENTIALS), 412);
    assertNotNull (m_aAPMgr.getAccessPointOfName (AP_NAME));

    // Second invocation changes nothing
    final String sResponse2 = _testResponse (m_aClient.post (PATH_NAME + AP_NAME + "/use-for-matching-endpoints", CREDENTIALS), 200);
    assertEquals ("0", _parse (sResponse2).getAttributeValue ("changedendpoints"));
  }

  @NonNull
  private ISMPEndpoint _getEndpoint ()
  {
    final ISMPServiceInformation aSI = m_aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (m_aPI, m_aDT);
    assertNotNull (aSI);
    final ISMPProcess aProcess = aSI.getProcessOfID (m_aProcID);
    assertNotNull (aProcess);
    return aProcess.getAllEndpoints ().get (0);
  }
}
