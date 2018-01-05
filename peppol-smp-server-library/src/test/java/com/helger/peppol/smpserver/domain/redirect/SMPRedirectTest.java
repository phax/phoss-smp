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
package com.helger.peppol.smpserver.domain.redirect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.peppol.smpserver.mock.SMPServerTestRule;
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
  public void testRedirect ()
  {
    final IParticipantIdentifier aPI = new SimpleParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                        "0088:dummy");
    final IDocumentTypeIdentifier aDocTypeID = new SimpleDocumentTypeIdentifier (PeppolIdentifierHelper.DEFAULT_DOCUMENT_TYPE_SCHEME,
                                                                                 "testdoctype");

    final SMPServiceGroup aSG = new SMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null);

    // Create new one
    final ISMPRedirect aRedirect = new SMPRedirect (aSG, aDocTypeID, "target", "suid", "<extredirect/>");
    assertSame (aSG, aRedirect.getServiceGroup ());
    assertEquals (aDocTypeID, aRedirect.getDocumentTypeIdentifier ());
    assertEquals ("target", aRedirect.getTargetHref ());
    assertEquals ("suid", aRedirect.getSubjectUniqueIdentifier ());
    assertEquals ("[{\"Any\":\"<extredirect />\"}]", aRedirect.getExtensionAsString ());
  }
}
