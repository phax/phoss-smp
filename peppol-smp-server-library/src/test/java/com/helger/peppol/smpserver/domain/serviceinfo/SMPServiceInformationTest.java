/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
