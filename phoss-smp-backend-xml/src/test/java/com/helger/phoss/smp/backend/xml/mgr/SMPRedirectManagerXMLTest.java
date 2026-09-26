/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.xml.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.xml.mock.XMLTestHelper;

/**
 * Test class for class {@link SMPRedirectManagerXML}.
 *
 * @author Philip Helger
 */
public final class SMPRedirectManagerXMLTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testRedirectBasic () throws SMPServerException
  {
    // Ensure the user is present
    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    assertEquals (0, aRedirectMgr.getSMPRedirectCount ());

    // Delete existing service group
    final IParticipantIdentifier aPI = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                       "0088:dummy");
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);

    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI, null, null, true);
    assertNotNull (aSG);
    try
    {
      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                  "xml::xml##doctype4711::1");
      assertNotNull (aDocTypeID);

      final ISMPRedirect aRedirect = aRedirectMgr.createOrUpdateSMPRedirect (aPI,
                                                                             aDocTypeID,
                                                                             "bla",
                                                                             "foo",
                                                                             null,
                                                                             "<ext/>");
      assertNotNull (aRedirect);
      assertSame (aPI, aRedirect.getServiceGroupParticipantIdentifier ());
      assertEquals (aDocTypeID, aRedirect.getDocumentTypeIdentifier ());
      assertEquals ("bla", aRedirect.getTargetHref ());
      assertEquals ("foo", aRedirect.getSubjectUniqueIdentifier ());
      assertNull (aRedirect.getCertificate ());
      assertEquals ("<ext />", aRedirect.getExtensions ().getFirstExtensionXMLString ().trim ());

      XMLTestHelper.testMicroTypeConversion (aRedirect);
    }
    finally
    {
      aServiceGroupMgr.deleteSMPServiceGroup (aPI, true);
    }
  }

  @Test
  public void testRedirectUpperCaseSG () throws SMPServerException
  {
    // Ensure the user is present
    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    assertFalse (aIdentifierFactory.isParticipantIdentifierCaseInsensitive ("bla-sch-eme"));

    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    assertEquals (0, aRedirectMgr.getSMPRedirectCount ());

    // Delete existing service group
    final IParticipantIdentifier aPI = aIdentifierFactory.createParticipantIdentifier ("bla-sch-eme", "0088:UpperCase");
    assertNotNull (aPI);
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);

    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI, null, null, true);
    assertNotNull (aSG);
    try
    {
      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                  "xml::xml##DocType4711::1");
      assertNotNull (aDocTypeID);

      final ISMPRedirect aRedirect = aRedirectMgr.createOrUpdateSMPRedirect (aPI,
                                                                             aDocTypeID,
                                                                             "bla",
                                                                             "foo",
                                                                             null,
                                                                             "<ext/>");
      assertNotNull (aRedirect);
      assertSame (aPI, aRedirect.getServiceGroupParticipantIdentifier ());
      assertEquals (aDocTypeID, aRedirect.getDocumentTypeIdentifier ());
      assertEquals ("bla", aRedirect.getTargetHref ());
      assertEquals ("foo", aRedirect.getSubjectUniqueIdentifier ());
      assertNull (aRedirect.getCertificate ());
      assertEquals ("<ext />", aRedirect.getExtensions ().getFirstExtensionXMLString ().trim ());

      XMLTestHelper.testMicroTypeConversion (aRedirect);
    }
    finally
    {
      aServiceGroupMgr.deleteSMPServiceGroup (aPI, true);
    }
  }

  @Test
  public void testGetRedirectOfServiceGroupAndDocumentType () throws SMPServerException
  {
    // Ensure the user is present
    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    assertEquals (0, aRedirectMgr.getSMPRedirectCount ());

    final IParticipantIdentifier aPI1 = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                        "0088:xml-redirect-1");
    assertNotNull (aPI1);
    final IParticipantIdentifier aPI2 = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                        "0088:xml-redirect-2");
    assertNotNull (aPI2);
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI1, true);
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI2, true);

    assertNotNull (aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI1, null, null, true));
    try
    {
      assertNotNull (aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI2, null, null, true));
      try
      {
        // Create a bunch of unrelated registrations
        final int nRedirects = 20;
        for (int i = 0; i < nRedirects; ++i)
        {
          final IDocumentTypeIdentifier aCurDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                         "xml::xml##redirect" +
                                                                                                                                                                     i +
                                                                                                                                                                     "::1");
          assertNotNull (aCurDocTypeID);
          assertNotNull (aRedirectMgr.createOrUpdateSMPRedirect (aPI1,
                                                                 aCurDocTypeID,
                                                                 "target" + i,
                                                                 "suid" + i,
                                                                 null,
                                                                 null));
        }
        assertEquals (nRedirects, aRedirectMgr.getSMPRedirectCount ());

        // Exact hit in the middle of many unrelated registrations
        final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                    "xml::xml##redirect7::1");
        assertNotNull (aDocTypeID);
        final ISMPRedirect aRedirect = aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI1, aDocTypeID);
        assertNotNull (aRedirect);
        assertEquals ("target7", aRedirect.getTargetHref ());

        // Unknown document type
        final IDocumentTypeIdentifier aUnknownDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                           "xml::xml##redirect-unknown::1");
        assertNotNull (aUnknownDocTypeID);
        assertNull (aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI1, aUnknownDocTypeID));

        // Same document type, but a different participant
        assertNull (aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI2, aDocTypeID));

        // A participant ID that only matches after the unification
        final IParticipantIdentifier aNonUnifiedPI = new SimpleParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                      "0088:XML-Redirect-1");
        assertNotNull (aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aNonUnifiedPI, aDocTypeID));

        // Undefined parameters
        assertNull (aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (null, aDocTypeID));
        assertNull (aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI1, null));
      }
      finally
      {
        aServiceGroupMgr.deleteSMPServiceGroup (aPI2, true);
      }
    }
    finally
    {
      aServiceGroupMgr.deleteSMPServiceGroup (aPI1, true);
    }
  }
}
