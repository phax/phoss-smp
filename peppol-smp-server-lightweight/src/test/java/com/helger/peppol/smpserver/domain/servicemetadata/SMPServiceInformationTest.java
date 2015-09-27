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
package com.helger.peppol.smpserver.domain.servicemetadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.joda.time.LocalDateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.mock.CommonsTestHelper;
import com.helger.datetime.PDTFactory;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.process.SimpleProcessIdentifier;
import com.helger.peppol.smpserver.data.dao.MetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroupManager;
import com.helger.photon.basic.security.AccessManager;
import com.helger.photon.basic.security.CSecurity;
import com.helger.photon.basic.security.user.IUser;
import com.helger.photon.core.mock.PhotonCoreTestRule;

/**
 * Test class for class {@link SMPServiceInformation}.
 *
 * @author Philip Helger
 */
public final class SMPServiceInformationTest
{
  @Rule
  public final TestRule m_aTestRule = new PhotonCoreTestRule ();

  @Test
  public void testBasic ()
  {
    final IUser aTestUser = AccessManager.getInstance ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:dummy");
    final SMPServiceGroupManager aServiceGroupMgr = MetaManager.getServiceGroupMgr ();
    aServiceGroupMgr.deleteSMPServiceGroup (aPI);
    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (aTestUser, aPI, null);

    final LocalDateTime aStartDT = PDTFactory.getCurrentLocalDateTime ();
    final LocalDateTime aEndDT = aStartDT.plusYears (1);
    final SMPEndpoint aEP = new SMPEndpoint ("tp",
                                             "http://localhost/as2",
                                             false,
                                             "minauth",
                                             aStartDT,
                                             aEndDT,
                                             "cert",
                                             "sd",
                                             "tc",
                                             "ti",
                                             "extep");
    assertEquals ("tp", aEP.getTransportProfile ());
    assertEquals ("http://localhost/as2", aEP.getEndpointReference ());
    assertFalse (aEP.isRequireBusinessLevelSignature ());
    assertEquals ("minauth", aEP.getMinimumAuthenticationLevel ());
    assertEquals (aStartDT, aEP.getServiceActivationDateTime ());
    assertEquals (aEndDT, aEP.getServiceExpirationDateTime ());
    assertEquals ("cert", aEP.getCertificate ());
    assertEquals ("sd", aEP.getServiceDescription ());
    assertEquals ("tc", aEP.getTechnicalContactUrl ());
    assertEquals ("ti", aEP.getTechnicalInformationUrl ());
    assertEquals ("extep", aEP.getExtension ());

    final SimpleProcessIdentifier aProcessID = SimpleProcessIdentifier.createWithDefaultScheme ("testproc");
    final SMPProcess aProcess = new SMPProcess (aProcessID, CollectionHelper.newList (aEP), "extproc");
    assertEquals (aProcessID, aProcess.getProcessIdentifier ());
    assertEquals (1, aProcess.getAllEndpoints ().size ());
    assertEquals ("extproc", aProcess.getExtension ());

    final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createWithDefaultScheme ("testdoctype");
    final SMPServiceInformation aSI = new SMPServiceInformation (aSG,
                                                                 aDocTypeID,
                                                                 CollectionHelper.newList (aProcess),
                                                                 "extsi");
    assertSame (aSG, aSI.getServiceGroup ());
    assertEquals (aDocTypeID, aSI.getDocumentTypeIdentifier ());
    assertEquals (1, aSI.getAllProcesses ().size ());
    assertEquals ("extsi", aSI.getExtension ());

    CommonsTestHelper.testMicroTypeConversion (aSI);
  }

  @Test
  public void testMinimal ()
  {
    final IUser aTestUser = AccessManager.getInstance ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:dummy");
    final SMPServiceGroupManager aServiceGroupMgr = MetaManager.getServiceGroupMgr ();
    aServiceGroupMgr.deleteSMPServiceGroup (aPI);
    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (aTestUser, aPI, null);

    final SMPEndpoint aEP = new SMPEndpoint ("tp",
                                             "http://localhost/as2",
                                             false,
                                             (String) null,
                                             (LocalDateTime) null,
                                             (LocalDateTime) null,
                                             "cert",
                                             "sd",
                                             "tc",
                                             (String) null,
                                             (String) null);
    assertEquals ("tp", aEP.getTransportProfile ());
    assertEquals ("http://localhost/as2", aEP.getEndpointReference ());
    assertFalse (aEP.isRequireBusinessLevelSignature ());
    assertNull (aEP.getMinimumAuthenticationLevel ());
    assertNull (aEP.getServiceActivationDateTime ());
    assertNull (aEP.getServiceExpirationDateTime ());
    assertEquals ("cert", aEP.getCertificate ());
    assertEquals ("sd", aEP.getServiceDescription ());
    assertEquals ("tc", aEP.getTechnicalContactUrl ());
    assertNull (aEP.getTechnicalInformationUrl ());
    assertNull (aEP.getExtension ());

    final SimpleProcessIdentifier aProcessID = SimpleProcessIdentifier.createWithDefaultScheme ("testproc");
    final SMPProcess aProcess = new SMPProcess (aProcessID, CollectionHelper.newList (aEP), (String) null);
    assertEquals (aProcessID, aProcess.getProcessIdentifier ());
    assertEquals (1, aProcess.getAllEndpoints ().size ());
    assertNull (aProcess.getExtension ());

    final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createWithDefaultScheme ("testdoctype");
    final SMPServiceInformation aSI = new SMPServiceInformation (aSG,
                                                                 aDocTypeID,
                                                                 CollectionHelper.newList (aProcess),
                                                                 (String) null);
    assertSame (aSG, aSI.getServiceGroup ());
    assertEquals (aDocTypeID, aSI.getDocumentTypeIdentifier ());
    assertEquals (1, aSI.getAllProcesses ().size ());
    assertNull (aSI.getExtension ());

    CommonsTestHelper.testMicroTypeConversion (aSI);
  }
}
