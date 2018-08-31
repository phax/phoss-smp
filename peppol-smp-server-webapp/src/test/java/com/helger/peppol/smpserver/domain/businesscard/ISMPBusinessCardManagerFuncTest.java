/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.domain.businesscard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.persistence.PersistenceException;

import org.eclipse.persistence.exceptions.DatabaseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.mock.SMPServerTestRule;

/**
 * Test class for class {@link ISMPBusinessCardManager}.
 *
 * @author Philip Helger
 */
public final class ISMPBusinessCardManagerFuncTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testBusinessCard ()
  {
    final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

    final String sUserID = "junitredir";
    try
    {
      // May fail
      aUserMgr.createUser (sUserID, "dummy");
    }
    catch (final PersistenceException ex)
    {
      assertTrue (ex.getCause () instanceof DatabaseException);
      // MySQL is not configured correctly!
      return;
    }

    final SMPBusinessCardEntity aEntity1 = new SMPBusinessCardEntity ();
    aEntity1.setName ("Test entity");
    aEntity1.setCountryCode ("AT");
    final SMPBusinessCardEntity aEntity2 = new SMPBusinessCardEntity ();
    aEntity2.setName ("Test entity2");
    aEntity2.setCountryCode ("AT");
    aEntity2.setGeographicalInformation ("Address here");
    aEntity2.addIdentifier (new SMPBusinessCardIdentifier ("gln", "1234567890123"));
    aEntity2.addWebsiteURI ("https://www.peppol-directory.org/fake");
    aEntity2.addContact (new SMPBusinessCardContact ("support", "Unit test support", null, "support@peppol.eu"));
    aEntity2.setAdditionalInformation ("Bla foo fasel");
    aEntity2.setRegistrationDate (PDTFactory.getCurrentLocalDate ());
    final SMPBusinessCardEntity aEntity3 = new SMPBusinessCardEntity ();
    aEntity3.setName ("Test entity3");
    aEntity3.setCountryCode ("AT");
    aEntity3.setAdditionalInformation ("Entity 3");

    try
    {
      final IParticipantIdentifier aPI1 = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:junittest1");
      final IParticipantIdentifier aPI2 = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:junittest2");

      final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aSG = aSGMgr.createSMPServiceGroup (sUserID, aPI1, null);
      assertNotNull (aSG);
      ISMPBusinessCard aBusinessCard = null;
      try
      {
        final int nBCCount = aBusinessCardMgr.getSMPBusinessCardCount ();

        // Create new one
        aBusinessCard = aBusinessCardMgr.createOrUpdateSMPBusinessCard (aSG, new CommonsArrayList <> (aEntity1));
        assertSame (aSG, aBusinessCard.getServiceGroup ());
        assertEquals (aPI1.getScheme (), aBusinessCard.getServiceGroup ().getParticpantIdentifier ().getScheme ());
        assertEquals (aPI1.getValue (), aBusinessCard.getServiceGroup ().getParticpantIdentifier ().getValue ());
        assertEquals (1, aBusinessCard.getEntityCount ());

        assertEquals (nBCCount + 1, aBusinessCardMgr.getSMPBusinessCardCount ());
        assertEquals (aBusinessCard, aBusinessCardMgr.getSMPBusinessCardOfServiceGroup (aSG));
        assertEquals (aBusinessCard, aBusinessCardMgr.getSMPBusinessCardOfID (aSG.getID ()));

        // Update existing
        aBusinessCard = aBusinessCardMgr.createOrUpdateSMPBusinessCard (aSG,
                                                                        new CommonsArrayList <> (aEntity1, aEntity2));
        assertSame (aSG, aBusinessCard.getServiceGroup ());
        assertEquals (aPI1.getScheme (), aBusinessCard.getServiceGroup ().getParticpantIdentifier ().getScheme ());
        assertEquals (aPI1.getValue (), aBusinessCard.getServiceGroup ().getParticpantIdentifier ().getValue ());
        assertEquals (2, aBusinessCard.getEntityCount ());

        // Must not have changed
        assertEquals (nBCCount + 1, aBusinessCardMgr.getSMPBusinessCardCount ());
        assertEquals (aBusinessCard, aBusinessCardMgr.getSMPBusinessCardOfServiceGroup (aSG));
        assertEquals (aBusinessCard, aBusinessCardMgr.getSMPBusinessCardOfID (aSG.getID ()));

        // Add second one
        final ISMPServiceGroup aSG2 = aSGMgr.createSMPServiceGroup (sUserID, aPI2, null);
        assertNotNull (aSG2);
        ISMPBusinessCard aBusinessCard2 = null;
        try
        {
          aBusinessCard2 = aBusinessCardMgr.createOrUpdateSMPBusinessCard (aSG2, new CommonsArrayList <> (aEntity3));
          assertSame (aSG2, aBusinessCard2.getServiceGroup ());
          assertEquals (1, aBusinessCard2.getEntityCount ());
          assertEquals (nBCCount + 2, aBusinessCardMgr.getSMPBusinessCardCount ());

          assertEquals (aBusinessCard2, aBusinessCardMgr.getSMPBusinessCardOfServiceGroup (aSG2));
          assertEquals (aBusinessCard2, aBusinessCardMgr.getSMPBusinessCardOfID (aSG2.getID ()));

          // Cleanup
          assertTrue (aBusinessCardMgr.deleteSMPBusinessCard (aBusinessCard2).isChanged ());
          assertEquals (nBCCount + 1, aBusinessCardMgr.getSMPBusinessCardCount ());
          assertTrue (aBusinessCardMgr.deleteSMPBusinessCard (aBusinessCard).isChanged ());
          assertEquals (nBCCount + 0, aBusinessCardMgr.getSMPBusinessCardCount ());
          assertTrue (aSGMgr.deleteSMPServiceGroup (aPI2).isChanged ());
          assertTrue (aSGMgr.deleteSMPServiceGroup (aPI1).isChanged ());
        }
        finally
        {
          // Real cleanup
          aBusinessCardMgr.deleteSMPBusinessCard (aBusinessCard2);
          aSGMgr.deleteSMPServiceGroup (aPI2);
        }
      }
      finally
      {
        // Real cleanup
        aBusinessCardMgr.deleteSMPBusinessCard (aBusinessCard);
        aSGMgr.deleteSMPServiceGroup (aPI1);
      }
    }
    finally
    {
      // Don't care about the result
      aUserMgr.deleteUser (sUserID);
    }
  }
}
