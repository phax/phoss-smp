/**
 * Copyright (C) 2019-2021 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import javax.annotation.Nonnull;

import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.redirect.SMPRedirect;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;

/**
 * Test class for class {@link SMPRedirectManagerMongoDB}.
 *
 * @author Philip Helger
 */
public final class SMPRedirectManagerMongoDBTest
{
  @Rule
  public final SMPServerTestRule m_aTestRule = new SMPServerTestRule ();

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

    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI, null);
    assertNotNull (aSG);
    try
    {
      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                  "doctype4711");
      final ISMPRedirect aRedirect = aRedirectMgr.createOrUpdateSMPRedirect (aSG, aDocTypeID, "bla", "foo", null, "<ext/>");
      assertNotNull (aRedirect);
      assertSame (aSG, aRedirect.getServiceGroup ());
      assertEquals (aDocTypeID, aRedirect.getDocumentTypeIdentifier ());
      assertEquals ("bla", aRedirect.getTargetHref ());
      assertEquals ("foo", aRedirect.getSubjectUniqueIdentifier ());
      assertNull (aRedirect.getCertificate ());
      assertEquals ("<ext />", aRedirect.getFirstExtensionXML ().trim ());
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

    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI, null);
    assertNotNull (aSG);
    try
    {
      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                  "DocType4711");
      final ISMPRedirect aRedirect = aRedirectMgr.createOrUpdateSMPRedirect (aSG, aDocTypeID, "bla", "foo", null, "<ext/>");
      assertNotNull (aRedirect);
      assertSame (aSG, aRedirect.getServiceGroup ());
      assertEquals (aDocTypeID, aRedirect.getDocumentTypeIdentifier ());
      assertEquals ("bla", aRedirect.getTargetHref ());
      assertEquals ("foo", aRedirect.getSubjectUniqueIdentifier ());
      assertNull (aRedirect.getCertificate ());
      assertEquals ("<ext />", aRedirect.getFirstExtensionXML ().trim ());
    }
    finally
    {
      aServiceGroupMgr.deleteSMPServiceGroup (aPI, true);
    }
  }

  private static void _testConversion (@Nonnull final ISMPRedirect aSrc)
  {
    final Document aDoc = SMPRedirectManagerMongoDB.toBson (aSrc);
    assertNotNull (aDoc);

    final ISMPRedirect aSrc2 = SMPRedirectManagerMongoDB.toDomain (SMPMetaManager.getIdentifierFactory (),
                                                                   SMPMetaManager.getServiceGroupMgr (),
                                                                   aDoc);
    assertNotNull (aSrc2);
    assertEquals (aSrc, aSrc2);
  }

  @Test
  public void testConversion () throws SMPServerException
  {
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();

    final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                "doctype4711");

    // Delete existing service group
    final IParticipantIdentifier aPI = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                       "0088:dummy");
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);

    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup ("blub", aPI, null);
    assertNotNull (aSG);
    try
    {
      _testConversion (new SMPRedirect (aSG, aDocTypeID, "target href", "what ever", null, null));
      _testConversion (new SMPRedirect (aSG, aDocTypeID, "target href", "what ever", null, "<ext/>"));
    }
    finally
    {
      aServiceGroupMgr.deleteSMPServiceGroup (aPI, true);
    }
  }
}
