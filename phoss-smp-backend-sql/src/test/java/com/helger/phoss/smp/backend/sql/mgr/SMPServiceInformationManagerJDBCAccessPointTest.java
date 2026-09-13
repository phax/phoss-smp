/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
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
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Test class for the Access Point related operations of {@link SMPServiceInformationManagerJDBC}.
 * Requires a running PostgreSQL instance as started by "unittest-db-docker-compose.yml".
 *
 * @author Philip Helger
 */
public final class SMPServiceInformationManagerJDBCAccessPointTest
{
  private static final String URL1 = "http://localhost/ap1";
  private static final String URL2 = "http://localhost/ap2";
  private static final String URL3 = "http://localhost/ap3";
  private static final String NAME1 = "ap1";
  private static final String CERT1 = "cert1";
  private static final String CERT2 = "cert2";
  private static final String CERT3 = "cert3";

  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  private WebScoped m_aWebScoped;
  private IIdentifierFactory m_aIF;
  private ISMPServiceGroupManager m_aSGMgr;
  private ISMPServiceInformationManager m_aSIMgr;
  private ISMPAccessPointManager m_aAPMgr;
  private IParticipantIdentifier m_aPI1;
  private IParticipantIdentifier m_aPI2;
  private IDocumentTypeIdentifier m_aDT1;
  private IDocumentTypeIdentifier m_aDT2;
  private IProcessIdentifier m_aProcID;

  @Before
  public void before () throws SMPServerException
  {
    // The SQL backend needs a request scope, e.g. for the settings
    m_aWebScoped = new WebScoped ();

    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);

    m_aIF = SMPMetaManager.getIdentifierFactory ();
    m_aSGMgr = SMPMetaManager.getServiceGroupMgr ();
    m_aSIMgr = SMPMetaManager.getServiceInformationMgr ();
    m_aAPMgr = SMPMetaManager.getAccessPointMgr ();

    m_aPI1 = m_aIF.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME, "0088:apone");
    m_aPI2 = m_aIF.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME, "0088:aptwo");
    m_aDT1 = m_aIF.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                 "xml::xml##doctype1::1");
    m_aDT2 = m_aIF.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                 "xml::xml##doctype2::1");
    m_aProcID = m_aIF.createProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME, "testproc");

    m_aSGMgr.deleteSMPServiceGroupNoEx (m_aPI1, true);
    m_aSGMgr.deleteSMPServiceGroupNoEx (m_aPI2, true);
    // The database content survives between the tests
    for (final ISMPAccessPoint aAP : m_aAPMgr.getAllAccessPoints ())
      m_aAPMgr.deleteAccessPoint (aAP.getID ());
    m_aSGMgr.createSMPServiceGroup (aTestUser.getID (), m_aPI1, null, null, true);
    m_aSGMgr.createSMPServiceGroup (aTestUser.getID (), m_aPI2, null, null, true);

    // 3 endpoints share URL1/CERT1, 1 endpoint uses URL2/CERT2
    _createSI (m_aPI1, m_aDT1, URL1, CERT1);
    _createSI (m_aPI1, m_aDT2, URL1, CERT1);
    _createSI (m_aPI2, m_aDT1, URL1, CERT1);
    _createSI (m_aPI2, m_aDT2, URL2, CERT2);

    // All endpoints contain their data directly - no Access Point is ever
    // created implicitly
    assertEquals (4, _getEndpointCount ());
    assertEquals (0, m_aAPMgr.getAccessPointCount ());
  }

  @After
  public void after ()
  {
    m_aSGMgr.deleteSMPServiceGroupNoEx (m_aPI1, true);
    m_aSGMgr.deleteSMPServiceGroupNoEx (m_aPI2, true);
    for (final ISMPAccessPoint aAP : m_aAPMgr.getAllAccessPoints ())
      m_aAPMgr.deleteAccessPoint (aAP.getID ());

    m_aWebScoped.close ();
  }

  private void _createSI (final IParticipantIdentifier aPI,
                          final IDocumentTypeIdentifier aDT,
                          final String sURL,
                          final String sCert)
  {
    final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
    final SMPEndpoint aEP = new SMPEndpoint (GlobalIDFactory.getNewPersistentStringID (),
                                             "tp",
                                             sURL,
                                             false,
                                             "minauth",
                                             aStartDT,
                                             aStartDT.plusYears (1),
                                             sCert,
                                             "sd",
                                             "tc",
                                             "ti",
                                             null);
    final SMPProcess aProcess = new SMPProcess (m_aProcID, new CommonsArrayList <> (aEP), null);
    assertTrue (m_aSIMgr.mergeSMPServiceInformation (new SMPServiceInformation (aPI,
                                                                                aDT,
                                                                                new CommonsArrayList <> (aProcess),
                                                                                null)).isSuccess ());
  }

  private long _getEndpointCount ()
  {
    long ret = 0;
    for (final ISMPServiceInformation aSI : m_aSIMgr.getAllSMPServiceInformation ())
      for (final ISMPProcess aProcess : aSI.getAllProcesses ())
        ret += aProcess.getEndpointCount ();
    return ret;
  }

  private ISMPEndpoint _getEndpoint (final IParticipantIdentifier aPI, final IDocumentTypeIdentifier aDT)
  {
    final ISMPServiceInformation aSI = m_aSIMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI, aDT);
    assertNotNull (aSI);
    final ISMPProcess aProcess = aSI.getProcessOfID (m_aProcID);
    assertNotNull (aProcess);
    return aProcess.getAllEndpoints ().get (0);
  }

  @Test
  public void testUpdateAllEndpointCertificatesOfDirectData ()
  {
    // The return value is the number of affected endpoints
    assertEquals (3, m_aSIMgr.updateAllEndpointCertificates (CERT1, CERT3));

    assertEquals (4, _getEndpointCount ());
    assertEquals (CERT3, _getEndpoint (m_aPI1, m_aDT1).getCertificate ());
    assertEquals (CERT3, _getEndpoint (m_aPI1, m_aDT2).getCertificate ());
    assertEquals (CERT3, _getEndpoint (m_aPI2, m_aDT1).getCertificate ());
    // The unrelated endpoint is untouched
    assertEquals (CERT2, _getEndpoint (m_aPI2, m_aDT2).getCertificate ());

    // Nothing left to change
    assertEquals (0, m_aSIMgr.updateAllEndpointCertificates (CERT1, CERT3));
  }

  @Test
  public void testUpdateAllEndpointCertificatesUnknownCert ()
  {
    assertEquals (0, m_aSIMgr.updateAllEndpointCertificates ("not-used-anywhere", CERT3));
    assertEquals (CERT1, _getEndpoint (m_aPI1, m_aDT1).getCertificate ());
  }

  @Test
  public void testUpdateAllEndpointCertificatesChangesAccessPointOnly ()
  {
    final ISMPAccessPoint aAP = m_aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);
    // Opt-in for all 3 endpoints using URL1/CERT1
    assertEquals (3, m_aSIMgr.useAccessPointForMatchingEndpoints (aAP.getID (), true));

    // 3 endpoints via the Access Point, none directly
    assertEquals (3, m_aSIMgr.updateAllEndpointCertificates (CERT1, CERT3));
    assertEquals (CERT3, m_aAPMgr.getAccessPointOfID (aAP.getID ()).getCertificate ());
    assertEquals (CERT3, _getEndpoint (m_aPI1, m_aDT1).getCertificate ());
    assertEquals (CERT2, _getEndpoint (m_aPI2, m_aDT2).getCertificate ());
  }

  @Test
  public void testUpdateAllEndpointURLsOfDirectData ()
  {
    assertEquals (3, m_aSIMgr.updateAllEndpointURLs (null, URL1, URL3));

    assertEquals (URL3, _getEndpoint (m_aPI1, m_aDT1).getEndpointReference ());
    assertEquals (URL3, _getEndpoint (m_aPI1, m_aDT2).getEndpointReference ());
    assertEquals (URL3, _getEndpoint (m_aPI2, m_aDT1).getEndpointReference ());
    // The certificate is not affected
    assertEquals (CERT1, _getEndpoint (m_aPI1, m_aDT1).getCertificate ());
    // The unrelated endpoint is untouched
    assertEquals (URL2, _getEndpoint (m_aPI2, m_aDT2).getEndpointReference ());
  }

  @Test
  public void testUpdateAllEndpointURLsServiceGroupFiltered ()
  {
    // Only the 2 endpoints of PI1 are affected
    assertEquals (2, m_aSIMgr.updateAllEndpointURLs (m_aPI1, URL1, URL3));

    assertEquals (URL3, _getEndpoint (m_aPI1, m_aDT1).getEndpointReference ());
    assertEquals (URL3, _getEndpoint (m_aPI1, m_aDT2).getEndpointReference ());
    // PI2 is untouched
    assertEquals (URL1, _getEndpoint (m_aPI2, m_aDT1).getEndpointReference ());
    assertEquals (URL2, _getEndpoint (m_aPI2, m_aDT2).getEndpointReference ());
  }

  @Test
  public void testUpdateAllEndpointURLsNoOps ()
  {
    // Same URL
    assertEquals (0, m_aSIMgr.updateAllEndpointURLs (null, URL1, URL1));
    // Unknown old URL
    assertEquals (0, m_aSIMgr.updateAllEndpointURLs (null, "http://localhost/unknown", URL3));
    // Known URL, but not used by that service group
    assertEquals (0, m_aSIMgr.updateAllEndpointURLs (m_aPI1, URL2, URL3));

    assertEquals (4, _getEndpointCount ());
  }

  @Test
  public void testUseAccessPointForMatchingEndpoints ()
  {
    final ISMPAccessPoint aAP = m_aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);
    assertEquals (0, m_aSIMgr.getEndpointCountUsingAccessPoint (aAP.getID ()));
    assertFalse (m_aSIMgr.containsAnyEndpointWithAccessPoint (aAP.getID ()));

    // Only the 3 endpoints with the same certificate AND the same URL
    assertEquals (3, m_aSIMgr.useAccessPointForMatchingEndpoints (aAP.getID (), true));
    assertEquals (3, m_aSIMgr.getEndpointCountUsingAccessPoint (aAP.getID ()));
    assertTrue (m_aSIMgr.containsAnyEndpointWithAccessPoint (aAP.getID ()));

    // The endpoint data is unchanged - it is just resolved differently now
    assertEquals (aAP.getID (), _getEndpoint (m_aPI1, m_aDT1).getAccessPointID ());
    assertEquals (NAME1, _getEndpoint (m_aPI1, m_aDT1).getAccessPointName ());
    assertEquals (URL1, _getEndpoint (m_aPI1, m_aDT1).getEndpointReference ());
    assertEquals (CERT1, _getEndpoint (m_aPI1, m_aDT1).getCertificate ());

    // The endpoint with a different certificate keeps its direct data
    assertNull (_getEndpoint (m_aPI2, m_aDT2).getAccessPointID ());
    assertEquals (URL2, _getEndpoint (m_aPI2, m_aDT2).getEndpointReference ());

    // Running it again changes nothing, because the endpoints already
    // reference an Access Point
    assertEquals (0, m_aSIMgr.useAccessPointForMatchingEndpoints (aAP.getID (), true));
  }

  @Test
  public void testUseAccessPointForMatchingEndpointsIgnoringURL ()
  {
    // Same certificate as the 3 endpoints, but a different URL
    final ISMPAccessPoint aAP = m_aAPMgr.createAccessPoint (NAME1, URL3, CERT1);
    assertNotNull (aAP);

    // Nothing matches, if the URL must be identical
    assertEquals (0, m_aSIMgr.useAccessPointForMatchingEndpoints (aAP.getID (), true));

    // All 3 endpoints match, if only the certificate is relevant - and they get
    // the URL of the Access Point
    assertEquals (3, m_aSIMgr.useAccessPointForMatchingEndpoints (aAP.getID (), false));
    assertEquals (URL3, _getEndpoint (m_aPI1, m_aDT1).getEndpointReference ());
    assertEquals (CERT1, _getEndpoint (m_aPI1, m_aDT1).getCertificate ());
  }

  @Test
  public void testUseAccessPointForMatchingEndpointsUnknownAccessPoint ()
  {
    assertEquals (0, m_aSIMgr.useAccessPointForMatchingEndpoints ("does-not-exist", true));
    assertEquals (0, m_aSIMgr.getEndpointCountUsingAccessPoint ("does-not-exist"));
    assertEquals (0, m_aSIMgr.getEndpointCountUsingAccessPoint (null));
  }

  @Test
  public void testAccessPointSurvivesServiceGroupDeletion ()
  {
    final ISMPAccessPoint aAP = m_aAPMgr.createAccessPoint (NAME1, URL1, CERT1);
    assertNotNull (aAP);
    assertEquals (3, m_aSIMgr.useAccessPointForMatchingEndpoints (aAP.getID (), true));

    assertTrue (m_aSGMgr.deleteSMPServiceGroupNoEx (m_aPI2, true).isChanged ());

    // Access Points are managed explicitly - they are never removed implicitly
    assertEquals (2, _getEndpointCount ());
    assertEquals (1, m_aAPMgr.getAccessPointCount ());
    assertEquals (2, m_aSIMgr.getEndpointCountUsingAccessPoint (aAP.getID ()));
  }
}
