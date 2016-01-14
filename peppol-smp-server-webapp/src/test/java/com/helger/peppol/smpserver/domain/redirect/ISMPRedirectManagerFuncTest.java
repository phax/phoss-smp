/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
import com.helger.peppol.smpserver.SMPServerTestRule;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;

/**
 * Test class for class {@link ISMPRedirectManager}.
 *
 * @author Philip Helger
 */
public final class ISMPRedirectManagerFuncTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testRedirect ()
  {
    final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
    final String sUserID = "junitredir";
    aUserMgr.createUser (sUserID, "dummy");
    try
    {
      final SimpleParticipantIdentifier aPI1 = SimpleParticipantIdentifier.createWithDefaultScheme ("9999:junittest1");
      final SimpleParticipantIdentifier aPI2 = SimpleParticipantIdentifier.createWithDefaultScheme ("9999:junittest2");
      final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createWithDefaultScheme ("junit::testdoc#ext:1.0");

      final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aSG = aSGMgr.createSMPServiceGroup (sUserID, aPI1, null);
      try
      {
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();

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
        final int nCount = aRedirectMgr.getSMPRedirectCount ();

        // Update existing
        aRedirect = aRedirectMgr.createOrUpdateSMPRedirect (aSG, aDocTypeID, "target2", "suid2", "extredirect2");
        assertSame (aSG, aRedirect.getServiceGroup ());
        assertTrue (IdentifierHelper.areDocumentTypeIdentifiersEqual (aDocTypeID,
                                                                      aRedirect.getDocumentTypeIdentifier ()));
        assertEquals ("target2", aRedirect.getTargetHref ());
        assertEquals ("suid2", aRedirect.getSubjectUniqueIdentifier ());
        assertEquals ("extredirect2", aRedirect.getExtension ());
        assertEquals (nCount, aRedirectMgr.getSMPRedirectCount ());

        // Add second one
        final ISMPServiceGroup aSG2 = aSGMgr.createSMPServiceGroup (sUserID, aPI2, null);
        try
        {
          aRedirect = aRedirectMgr.createOrUpdateSMPRedirect (aSG2, aDocTypeID, "target2", "suid2", "extredirect2");
          assertSame (aSG2, aRedirect.getServiceGroup ());
          assertTrue (IdentifierHelper.areDocumentTypeIdentifiersEqual (aDocTypeID,
                                                                        aRedirect.getDocumentTypeIdentifier ()));
          assertEquals ("target2", aRedirect.getTargetHref ());
          assertEquals ("suid2", aRedirect.getSubjectUniqueIdentifier ());
          assertEquals ("extredirect2", aRedirect.getExtension ());
          assertEquals (nCount + 1, aRedirectMgr.getSMPRedirectCount ());

          // Cleanup
          assertTrue (aRedirectMgr.deleteAllSMPRedirectsOfServiceGroup (aSG2).isChanged ());
          assertEquals (nCount, aRedirectMgr.getSMPRedirectCount ());
          assertTrue (aRedirectMgr.deleteAllSMPRedirectsOfServiceGroup (aSG).isChanged ());
          assertEquals (nCount - 1, aRedirectMgr.getSMPRedirectCount ());
          assertTrue (aSGMgr.deleteSMPServiceGroup (aPI2).isChanged ());
          assertTrue (aSGMgr.deleteSMPServiceGroup (aPI1).isChanged ());
        }
        finally
        {
          aSGMgr.deleteSMPServiceGroup (aPI2);
        }
      }
      finally
      {
        aSGMgr.deleteSMPServiceGroup (aPI1);
      }
    }
    finally
    {
      aUserMgr.deleteUser (sUserID);
    }
  }
}
