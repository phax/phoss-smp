/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.serviceinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.simple.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;
import com.helger.phoss.smp.mock.SMPServerTestRule;

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

    final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
    final XMLOffsetDateTime aEndDT = aStartDT.plusYears (1);
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
    assertEquals ("[{\"Any\":\"<extep />\"}]", aEP.getExtensions ().getExtensionsAsJsonString ());

    final IProcessIdentifier aProcessID = new SimpleProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                       "testproc");
    final SMPProcess aProcess = new SMPProcess (aProcessID, new CommonsArrayList <> (aEP), "<extproc/>");
    assertEquals (aProcessID, aProcess.getProcessIdentifier ());
    assertEquals (1, aProcess.getAllEndpoints ().size ());
    assertEquals ("[{\"Any\":\"<extproc />\"}]", aProcess.getExtensions ().getExtensionsAsJsonString ());

    final IDocumentTypeIdentifier aDocTypeID = new SimpleDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                 "testdoctype");
    final SMPServiceInformation aSI = new SMPServiceInformation (aPI,
                                                                 aDocTypeID,
                                                                 new CommonsArrayList <> (aProcess),
                                                                 "<extsi/>");
    assertSame (aPI, aSI.getServiceGroupParticipantIdentifier ());
    assertEquals (aDocTypeID, aSI.getDocumentTypeIdentifier ());
    assertEquals (1, aSI.getAllProcesses ().size ());
    assertEquals ("[{\"Any\":\"<extsi />\"}]", aSI.getExtensions ().getExtensionsAsJsonString ());
  }

  @Test
  public void testMinimal ()
  {
    final IParticipantIdentifier aPI = new SimpleParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                        "0088:dummy");

    final SMPEndpoint aEP = new SMPEndpoint ("tp",
                                             "http://localhost/as2",
                                             false,
                                             (String) null,
                                             (XMLOffsetDateTime) null,
                                             (XMLOffsetDateTime) null,
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
    assertNull (aEP.getExtensions ().getExtensionsAsJsonString ());

    final IProcessIdentifier aProcessID = new SimpleProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                       "testproc");
    final SMPProcess aProcess = new SMPProcess (aProcessID, new CommonsArrayList <> (aEP), (String) null);
    assertEquals (aProcessID, aProcess.getProcessIdentifier ());
    assertEquals (1, aProcess.getAllEndpoints ().size ());
    assertNull (aProcess.getExtensions ().getExtensionsAsJsonString ());

    final IDocumentTypeIdentifier aDocTypeID = new SimpleDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                 "testdoctype");
    final SMPServiceInformation aSI = new SMPServiceInformation (aPI,
                                                                 aDocTypeID,
                                                                 new CommonsArrayList <> (aProcess),
                                                                 (String) null);
    assertSame (aPI, aSI.getServiceGroupParticipantIdentifier ());
    assertEquals (aDocTypeID, aSI.getDocumentTypeIdentifier ());
    assertEquals (1, aSI.getAllProcesses ().size ());
    assertNull (aSI.getExtensions ().getExtensionsAsJsonString ());
  }
}
