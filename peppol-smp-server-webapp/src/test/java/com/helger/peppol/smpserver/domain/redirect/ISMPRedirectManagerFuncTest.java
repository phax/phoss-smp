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

import javax.persistence.PersistenceException;

import org.eclipse.persistence.exceptions.DatabaseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.peppol.doctype.PeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.peppol.participant.PeppolParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.mock.SMPServerTestRule;

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
    try
    {
      aUserMgr.createUser (sUserID, "dummy");
    }
    catch (final PersistenceException ex)
    {
      assertTrue (ex.getCause () instanceof DatabaseException);
      // MySQL is not configured correctly!
      return;
    }
    try
    {
      final IParticipantIdentifier aPI1 = PeppolParticipantIdentifier.createWithDefaultScheme ("9999:junittest1");
      final IParticipantIdentifier aPI2 = PeppolParticipantIdentifier.createWithDefaultScheme ("9999:junittest2");
      final IDocumentTypeIdentifier aDocTypeID = PeppolDocumentTypeIdentifier.createWithDefaultScheme ("junit::testdoc#ext:1.0");

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
        assertEquals ("extredirect", aRedirect.getExtensionAsString ());
        final int nCount = aRedirectMgr.getSMPRedirectCount ();

        // Update existing
        aRedirect = aRedirectMgr.createOrUpdateSMPRedirect (aSG, aDocTypeID, "target2", "suid2", "extredirect2");
        assertSame (aSG, aRedirect.getServiceGroup ());
        assertTrue (IdentifierHelper.areDocumentTypeIdentifiersEqual (aDocTypeID,
                                                                      aRedirect.getDocumentTypeIdentifier ()));
        assertEquals ("target2", aRedirect.getTargetHref ());
        assertEquals ("suid2", aRedirect.getSubjectUniqueIdentifier ());
        assertEquals ("extredirect2", aRedirect.getExtensionAsString ());
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
          assertEquals ("extredirect2", aRedirect.getExtensionAsString ());
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
