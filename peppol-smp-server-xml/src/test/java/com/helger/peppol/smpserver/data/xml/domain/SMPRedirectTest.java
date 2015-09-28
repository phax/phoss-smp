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
package com.helger.peppol.smpserver.data.xml.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.mock.CommonsTestHelper;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.data.xml.MetaManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirect;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;
import com.helger.photon.basic.security.AccessManager;
import com.helger.photon.basic.security.CSecurity;
import com.helger.photon.basic.security.user.IUser;

/**
 * Test class for class {@link SMPRedirect}.
 *
 * @author Philip Helger
 */
public final class SMPRedirectTest
{
  @Rule
  public final TestRule m_aTestRule = new PhotonBasicWebTestRule ();

  @Test
  public void testRedirect ()
  {
    final IUser aTestUser = AccessManager.getInstance ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:dummy");
    final SimpleParticipantIdentifier aPI2 = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:dummy2");
    final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createWithDefaultScheme ("testdoctype");

    final ISMPServiceGroupManager aSGMgr = MetaManager.getServiceGroupMgr ();
    // Ensure it is not present
    aSGMgr.deleteSMPServiceGroup (aPI);
    final ISMPServiceGroup aSG = aSGMgr.createSMPServiceGroup (aTestUser, aPI, null);

    final ISMPRedirectManager aRedirectMgr = MetaManager.getRedirectMgr ();

    // Create new one
    ISMPRedirect aRedirect = aRedirectMgr.createSMPRedirect (aSG, aDocTypeID, "target", "suid", "extredirect");
    assertSame (aSG, aRedirect.getServiceGroup ());
    assertTrue (IdentifierHelper.areDocumentTypeIdentifiersEqual (aDocTypeID, aRedirect.getDocumentTypeIdentifier ()));
    assertEquals ("target", aRedirect.getTargetHref ());
    assertEquals ("suid", aRedirect.getSubjectUniqueIdentifier ());
    assertEquals ("extredirect", aRedirect.getExtension ());
    assertEquals (1, aRedirectMgr.getSMPRedirectCount ());
    assertSame (aRedirect, aRedirectMgr.getSMPRedirectOfID (aRedirect.getID ()));
    CommonsTestHelper.testMicroTypeConversion (aRedirect);

    // Update existing
    aRedirect = aRedirectMgr.createSMPRedirect (aSG, aDocTypeID, "target2", "suid2", "extredirect2");
    assertSame (aSG, aRedirect.getServiceGroup ());
    assertTrue (IdentifierHelper.areDocumentTypeIdentifiersEqual (aDocTypeID, aRedirect.getDocumentTypeIdentifier ()));
    assertEquals ("target2", aRedirect.getTargetHref ());
    assertEquals ("suid2", aRedirect.getSubjectUniqueIdentifier ());
    assertEquals ("extredirect2", aRedirect.getExtension ());
    assertEquals (1, aRedirectMgr.getSMPRedirectCount ());
    assertSame (aRedirect, aRedirectMgr.getSMPRedirectOfID (aRedirect.getID ()));
    CommonsTestHelper.testMicroTypeConversion (aRedirect);

    // Add second one
    final ISMPServiceGroup aSG2 = aSGMgr.createSMPServiceGroup (aTestUser, aPI2, null);
    aRedirect = aRedirectMgr.createSMPRedirect (aSG2, aDocTypeID, "target2", "suid2", "extredirect2");
    assertSame (aSG2, aRedirect.getServiceGroup ());
    assertTrue (IdentifierHelper.areDocumentTypeIdentifiersEqual (aDocTypeID, aRedirect.getDocumentTypeIdentifier ()));
    assertEquals ("target2", aRedirect.getTargetHref ());
    assertEquals ("suid2", aRedirect.getSubjectUniqueIdentifier ());
    assertEquals ("extredirect2", aRedirect.getExtension ());
    assertEquals (2, aRedirectMgr.getSMPRedirectCount ());
    assertSame (aRedirect, aRedirectMgr.getSMPRedirectOfID (aRedirect.getID ()));
    CommonsTestHelper.testMicroTypeConversion (aRedirect);

    // Cleanup
    assertTrue (aRedirectMgr.deleteAllSMPRedirectsOfServiceGroup (aSG2).isChanged ());
    assertEquals (1, aRedirectMgr.getSMPRedirectCount ());
    assertTrue (aRedirectMgr.deleteAllSMPRedirectsOfServiceGroup (aSG).isChanged ());
    assertEquals (0, aRedirectMgr.getSMPRedirectCount ());
    assertTrue (aSGMgr.deleteSMPServiceGroup (aSG2).isChanged ());
    assertTrue (aSGMgr.deleteSMPServiceGroup (aSG).isChanged ());
  }
}
