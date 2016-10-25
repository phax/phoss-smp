/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.domain.serviceinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.time.LocalDateTime;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.datetime.PDTFactory;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.identifier.generic.process.SimpleProcessIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.peppol.smpserver.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;

/**
 * Test class for class {@link SMPServiceInformation}.
 *
 * @author Philip Helger
 */
public final class SMPServiceInformationTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testBasic ()
  {
    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("0088:dummy");
    final ISMPServiceGroup aSG = new SMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null);

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
                                             "<extep/>");
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
    assertEquals ("[{\"Any\":\"<extep />\"}]", aEP.getExtensionAsString ());

    final IProcessIdentifier aProcessID = new SimpleProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                       "testproc");
    final SMPProcess aProcess = new SMPProcess (aProcessID, CollectionHelper.newList (aEP), "<extproc/>");
    assertEquals (aProcessID, aProcess.getProcessIdentifier ());
    assertEquals (1, aProcess.getAllEndpoints ().size ());
    assertEquals ("[{\"Any\":\"<extproc />\"}]", aProcess.getExtensionAsString ());

    final IDocumentTypeIdentifier aDocTypeID = new SimpleDocumentTypeIdentifier (PeppolIdentifierHelper.DEFAULT_DOCUMENT_TYPE_SCHEME,
                                                                                 "testdoctype");
    final SMPServiceInformation aSI = new SMPServiceInformation (aSG,
                                                                 aDocTypeID,
                                                                 CollectionHelper.newList (aProcess),
                                                                 "<extsi/>");
    assertSame (aSG, aSI.getServiceGroup ());
    assertEquals (aDocTypeID, aSI.getDocumentTypeIdentifier ());
    assertEquals (1, aSI.getAllProcesses ().size ());
    assertEquals ("[{\"Any\":\"<extsi />\"}]", aSI.getExtensionAsString ());
  }

  @Test
  public void testMinimal ()
  {
    final IParticipantIdentifier aPI = new SimpleParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                        "0088:dummy");
    final ISMPServiceGroup aSG = new SMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null);

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
    assertNull (aEP.getExtensionAsString ());

    final IProcessIdentifier aProcessID = new SimpleProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                       "testproc");
    final SMPProcess aProcess = new SMPProcess (aProcessID, CollectionHelper.newList (aEP), (String) null);
    assertEquals (aProcessID, aProcess.getProcessIdentifier ());
    assertEquals (1, aProcess.getAllEndpoints ().size ());
    assertNull (aProcess.getExtensionAsString ());

    final IDocumentTypeIdentifier aDocTypeID = new SimpleDocumentTypeIdentifier (PeppolIdentifierHelper.DEFAULT_DOCUMENT_TYPE_SCHEME,
                                                                                 "testdoctype");
    final SMPServiceInformation aSI = new SMPServiceInformation (aSG,
                                                                 aDocTypeID,
                                                                 CollectionHelper.newList (aProcess),
                                                                 (String) null);
    assertSame (aSG, aSI.getServiceGroup ());
    assertEquals (aDocTypeID, aSI.getDocumentTypeIdentifier ());
    assertEquals (1, aSI.getAllProcesses ().size ());
    assertNull (aSI.getExtensionAsString ());
  }
}
