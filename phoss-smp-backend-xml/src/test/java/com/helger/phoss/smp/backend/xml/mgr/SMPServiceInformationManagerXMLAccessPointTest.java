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
package com.helger.phoss.smp.backend.xml.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;
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

/**
 * Test class for the Access Point related bulk operations of
 * {@link SMPServiceInformationManagerXML}.
 *
 * @author Philip Helger
 */
public final class SMPServiceInformationManagerXMLAccessPointTest
{
  private static final String URL1 = "http://localhost/ap1";
  private static final String URL2 = "http://localhost/ap2";
  private static final String URL3 = "http://localhost/ap3";
  private static final String CERT1 = "cert1";
  private static final String CERT2 = "cert2";
  private static final String CERT3 = "cert3";

  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

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
    m_aSGMgr.createSMPServiceGroup (aTestUser.getID (), m_aPI1, null, null, true);
    m_aSGMgr.createSMPServiceGroup (aTestUser.getID (), m_aPI2, null, null, true);

    // 3 endpoints share URL1/CERT1, 1 endpoint uses URL2/CERT2
    _createSI (m_aPI1, m_aDT1, URL1, CERT1);
    _createSI (m_aPI1, m_aDT2, URL1, CERT1);
    _createSI (m_aPI2, m_aDT1, URL1, CERT1);
    _createSI (m_aPI2, m_aDT2, URL2, CERT2);

    // The whole point of the normalization: 4 endpoints, but only 2 Access
    // Points
    assertEquals (4, _getEndpointCount ());
    assertEquals (2, m_aAPMgr.getAccessPointCount ());
  }

  @After
  public void after ()
  {
    m_aSGMgr.deleteSMPServiceGroupNoEx (m_aPI1, true);
    m_aSGMgr.deleteSMPServiceGroupNoEx (m_aPI2, true);
  }

  private void _createSI (final IParticipantIdentifier aPI,
                          final IDocumentTypeIdentifier aDT,
                          final String sURL,
                          final String sCert)
  {
    final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
    final SMPEndpoint aEP = new SMPEndpoint ("epid",
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

  private ICommonsSet <String> _getUsedAccessPointIDs ()
  {
    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    for (final ISMPServiceInformation aSI : m_aSIMgr.getAllSMPServiceInformation ())
      for (final ISMPProcess aProcess : aSI.getAllProcesses ())
        for (final ISMPEndpoint aEP : aProcess.getAllEndpoints ())
          ret.add (aEP.getAccessPointID ());
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
  public void testUpdateAllEndpointCertificatesTouchesAccessPointsOnly ()
  {
    final ISMPAccessPoint aAP1 = m_aAPMgr.findAccessPoint (URL1);
    final String sAPID1 = aAP1.getID ();
    final ICommonsSet <String> aUsedBefore = _getUsedAccessPointIDs ();

    // The return value is the number of affected ENDPOINTS, because that is
    // what the GUI reports to the user
    assertEquals (3, m_aSIMgr.updateAllEndpointCertificates (CERT1, CERT3));

    // Exactly one Access Point row was written, no endpoint was touched
    assertEquals (2, m_aAPMgr.getAccessPointCount ());
    assertEquals (sAPID1, m_aAPMgr.findAccessPoint (URL1).getID ());
    assertEquals (aUsedBefore, _getUsedAccessPointIDs ());
    assertEquals (4, _getEndpointCount ());

    // ... yet all 3 endpoints report the new certificate
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
    assertEquals (2, m_aAPMgr.getAccessPointCount ());
    assertEquals (CERT1, _getEndpoint (m_aPI1, m_aDT1).getCertificate ());
  }

  @Test
  public void testUpdateAllEndpointURLsFastPath ()
  {
    final String sAPID1 = m_aAPMgr.findAccessPoint (URL1).getID ();
    final ICommonsSet <String> aUsedBefore = _getUsedAccessPointIDs ();

    // No service group filter and URL3 is not in use yet -> the Access Point is
    // simply renamed, no endpoint is touched
    assertEquals (3, m_aSIMgr.updateAllEndpointURLs (null, URL1, URL3));

    assertEquals (2, m_aAPMgr.getAccessPointCount ());
    assertNull (m_aAPMgr.findAccessPoint (URL1));
    assertEquals (sAPID1, m_aAPMgr.findAccessPoint (URL3).getID ());
    assertEquals (aUsedBefore, _getUsedAccessPointIDs ());

    assertEquals (URL3, _getEndpoint (m_aPI1, m_aDT1).getEndpointReference ());
    assertEquals (URL3, _getEndpoint (m_aPI2, m_aDT1).getEndpointReference ());
    // The certificate travels with the Access Point
    assertEquals (CERT1, _getEndpoint (m_aPI1, m_aDT1).getCertificate ());
    // The unrelated endpoint is untouched
    assertEquals (URL2, _getEndpoint (m_aPI2, m_aDT2).getEndpointReference ());
  }

  @Test
  public void testUpdateAllEndpointURLsMergeIntoExisting ()
  {
    // URL2 already exists -> the endpoints must be re-pointed and the now
    // orphaned Access Point of URL1 must be garbage collected
    assertEquals (3, m_aSIMgr.updateAllEndpointURLs (null, URL1, URL2));

    assertEquals (1, m_aAPMgr.getAccessPointCount ());
    assertNull (m_aAPMgr.findAccessPoint (URL1));
    final ISMPAccessPoint aAP2 = m_aAPMgr.findAccessPoint (URL2);
    assertNotNull (aAP2);
    assertEquals (new CommonsHashSet <> (aAP2.getID ()), _getUsedAccessPointIDs ());

    // All 4 endpoints now use URL2 and therefore also its certificate, because
    // a physical Access Point can only have one certificate
    assertEquals (4, _getEndpointCount ());
    assertEquals (URL2, _getEndpoint (m_aPI1, m_aDT1).getEndpointReference ());
    assertEquals (CERT2, _getEndpoint (m_aPI1, m_aDT1).getCertificate ());
    assertEquals (CERT2, _getEndpoint (m_aPI2, m_aDT2).getCertificate ());
  }

  @Test
  public void testUpdateAllEndpointURLsServiceGroupFiltered ()
  {
    final String sAPID1 = m_aAPMgr.findAccessPoint (URL1).getID ();

    // Only the 2 endpoints of PI1 are affected, so the Access Point of URL1
    // must NOT be renamed - it is still used by PI2
    assertEquals (2, m_aSIMgr.updateAllEndpointURLs (m_aPI1, URL1, URL3));

    assertEquals (3, m_aAPMgr.getAccessPointCount ());
    assertEquals (sAPID1, m_aAPMgr.findAccessPoint (URL1).getID ());
    final ISMPAccessPoint aAP3 = m_aAPMgr.findAccessPoint (URL3);
    assertNotNull (aAP3);
    // The new Access Point inherits the certificate of the old one
    assertEquals (CERT1, aAP3.getCertificate ());

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

    assertEquals (2, m_aAPMgr.getAccessPointCount ());
    assertEquals (4, _getEndpointCount ());
  }

  @Test
  public void testUnusedAccessPointsAreCollected ()
  {
    // Deleting the only service group that uses URL2 must free its Access Point
    assertTrue (m_aSGMgr.deleteSMPServiceGroupNoEx (m_aPI2, true).isChanged ());

    assertEquals (2, _getEndpointCount ());
    assertEquals (1, m_aAPMgr.getAccessPointCount ());
    assertNotNull (m_aAPMgr.findAccessPoint (URL1));
    assertNull (m_aAPMgr.findAccessPoint (URL2));
  }
}
