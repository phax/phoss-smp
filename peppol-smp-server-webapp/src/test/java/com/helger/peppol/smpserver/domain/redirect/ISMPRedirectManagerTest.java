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
package com.helger.peppol.smpserver.domain.redirect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.data.ISMPUserManagerSPI;
import com.helger.peppol.smpserver.data.SMPUserManagerFactory;
import com.helger.peppol.smpserver.domain.MetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;
import com.helger.photon.basic.security.CSecurity;

/**
 * Test class for class {@link ISMPRedirectManager}.
 *
 * @author Philip Helger
 */
public final class ISMPRedirectManagerTest
{
  @Rule
  public final TestRule m_aTestRule = new PhotonBasicWebTestRule ();

  @Test
  public void testRedirect ()
  {
    final ISMPUserManagerSPI aUserMgr = SMPUserManagerFactory.getInstance ();
    aUserMgr.createUser (CSecurity.USER_ADMINISTRATOR_ID, "dummy");
    try
    {
      final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:dummy");
      final SimpleParticipantIdentifier aPI2 = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:dummy2");
      final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createWithDefaultScheme ("testdoctype");

      final ISMPServiceGroupManager aSGMgr = MetaManager.getServiceGroupMgr ();
      // Ensure it is not present
      aSGMgr.deleteSMPServiceGroup (aPI);
      final ISMPServiceGroup aSG = aSGMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null);

      final ISMPRedirectManager aRedirectMgr = MetaManager.getRedirectMgr ();

      // Create new one
      ISMPRedirect aRedirect = aRedirectMgr.createOrUpdateSMPRedirect (aSG,
                                                                       aDocTypeID,
                                                                       "target",
                                                                       "suid",
                                                                       "extredirect");
      assertSame (aSG, aRedirect.getServiceGroup ());
      assertTrue (IdentifierHelper.areDocumentTypeIdentifiersEqual (aDocTypeID,
                                                                    aRedirect.getDocumentTypeIdentifier ()));
      assertEquals ("target", aRedirect.getTargetHref ());
      assertEquals ("suid", aRedirect.getSubjectUniqueIdentifier ());
      assertEquals ("extredirect", aRedirect.getExtension ());
      assertEquals (1, aRedirectMgr.getSMPRedirectCount ());
      assertSame (aRedirect, aRedirectMgr.getSMPRedirectOfID (aRedirect.getID ()));

      // Update existing
      aRedirect = aRedirectMgr.createOrUpdateSMPRedirect (aSG, aDocTypeID, "target2", "suid2", "extredirect2");
      assertSame (aSG, aRedirect.getServiceGroup ());
      assertTrue (IdentifierHelper.areDocumentTypeIdentifiersEqual (aDocTypeID,
                                                                    aRedirect.getDocumentTypeIdentifier ()));
      assertEquals ("target2", aRedirect.getTargetHref ());
      assertEquals ("suid2", aRedirect.getSubjectUniqueIdentifier ());
      assertEquals ("extredirect2", aRedirect.getExtension ());
      assertEquals (1, aRedirectMgr.getSMPRedirectCount ());
      assertSame (aRedirect, aRedirectMgr.getSMPRedirectOfID (aRedirect.getID ()));

      // Add second one
      final ISMPServiceGroup aSG2 = aSGMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI2, null);
      aRedirect = aRedirectMgr.createOrUpdateSMPRedirect (aSG2, aDocTypeID, "target2", "suid2", "extredirect2");
      assertSame (aSG2, aRedirect.getServiceGroup ());
      assertTrue (IdentifierHelper.areDocumentTypeIdentifiersEqual (aDocTypeID,
                                                                    aRedirect.getDocumentTypeIdentifier ()));
      assertEquals ("target2", aRedirect.getTargetHref ());
      assertEquals ("suid2", aRedirect.getSubjectUniqueIdentifier ());
      assertEquals ("extredirect2", aRedirect.getExtension ());
      assertEquals (2, aRedirectMgr.getSMPRedirectCount ());
      assertSame (aRedirect, aRedirectMgr.getSMPRedirectOfID (aRedirect.getID ()));

      // Cleanup
      assertTrue (aRedirectMgr.deleteAllSMPRedirectsOfServiceGroup (aSG2.getID ()).isChanged ());
      assertEquals (1, aRedirectMgr.getSMPRedirectCount ());
      assertTrue (aRedirectMgr.deleteAllSMPRedirectsOfServiceGroup (aSG.getID ()).isChanged ());
      assertEquals (0, aRedirectMgr.getSMPRedirectCount ());
      assertTrue (aSGMgr.deleteSMPServiceGroup (aPI2).isChanged ());
      assertTrue (aSGMgr.deleteSMPServiceGroup (aPI).isChanged ());
    }
    finally
    {
      aUserMgr.deleteUser (CSecurity.USER_ADMINISTRATOR_ID);
    }
  }
}
