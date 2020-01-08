/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.redirect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.simple.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;

/**
 * Test class for class {@link SMPRedirect}.
 *
 * @author Philip Helger
 */
public final class SMPRedirectTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testBasic ()
  {
    final IParticipantIdentifier aPI = new SimpleParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                        "0088:dummy");
    final IDocumentTypeIdentifier aDocTypeID = new SimpleDocumentTypeIdentifier (PeppolIdentifierHelper.DEFAULT_DOCUMENT_TYPE_SCHEME,
                                                                                 "testdoctype");

    final SMPServiceGroup aSG = new SMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null);

    // Create new one
    final ISMPRedirect aRedirect = new SMPRedirect (aSG, aDocTypeID, "target", "suid", null, "<extredirect/>");
    assertSame (aSG, aRedirect.getServiceGroup ());
    assertEquals (aDocTypeID, aRedirect.getDocumentTypeIdentifier ());
    assertEquals ("target", aRedirect.getTargetHref ());
    assertEquals ("suid", aRedirect.getSubjectUniqueIdentifier ());
    assertNull (aRedirect.getCertificate ());
    assertFalse (aRedirect.hasCertificate ());
    assertEquals ("[{\"Any\":\"<extredirect />\"}]", aRedirect.getExtensionsAsString ());
  }

  @Test
  public void testCaseSensitivity ()
  {
    final IParticipantIdentifier aPI = new SimpleParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                        "0088:UpperCase");
    final IDocumentTypeIdentifier aDocTypeID = new SimpleDocumentTypeIdentifier (PeppolIdentifierHelper.DEFAULT_DOCUMENT_TYPE_SCHEME,
                                                                                 "testDocType");

    final SMPServiceGroup aSG = new SMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null);

    // Create new one
    final ISMPRedirect aRedirect = new SMPRedirect (aSG, aDocTypeID, "target", "suid", null, "<extredirect/>");
    assertSame (aSG, aRedirect.getServiceGroup ());
    assertEquals (aDocTypeID, aRedirect.getDocumentTypeIdentifier ());
    assertEquals ("target", aRedirect.getTargetHref ());
    assertEquals ("suid", aRedirect.getSubjectUniqueIdentifier ());
    assertNull (aRedirect.getCertificate ());
    assertFalse (aRedirect.hasCertificate ());
    assertEquals ("[{\"Any\":\"<extredirect />\"}]", aRedirect.getExtensionsAsString ());
  }
}
