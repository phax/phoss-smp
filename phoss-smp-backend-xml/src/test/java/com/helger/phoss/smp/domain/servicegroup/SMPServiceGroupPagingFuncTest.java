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
package com.helger.phoss.smp.domain.servicegroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.PagingSpec;
import com.helger.collection.paging.SortField;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;

/**
 * Functional test for the server side paging, sorting and searching of
 * {@link ISMPServiceGroupManager}, driven through the column mapping of
 * {@link ESMPServiceGroupColumn}.
 *
 * @author Philip Helger
 */
public final class SMPServiceGroupPagingFuncTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  private static final String [] VALUES = { "0088:paging1", "0088:paging2", "0088:paging3", "0088:other1" };

  @Test
  public void testPagingSortingSearching () throws SMPServerException
  {
    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);

    final IIdentifierFactory aIF = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aMgr = SMPMetaManager.getServiceGroupMgr ();

    final ICommonsList <IParticipantIdentifier> aCreated = new CommonsArrayList <> ();
    try
    {
      for (final String sValue : VALUES)
      {
        final IParticipantIdentifier aPI = aIF.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                            sValue);
        aMgr.deleteSMPServiceGroupNoEx (aPI, true);
        assertNotNull (aMgr.createSMPServiceGroup (aTestUser.getID (), aPI, null, null, true));
        aCreated.add (aPI);
      }

      final long nTotal = aMgr.getSMPServiceGroupCount ();
      assertEquals (VALUES.length, nTotal);

      // Ascending by participant ID - the default order
      final ICommonsList <ISMPServiceGroup> aPage1 = aMgr.getAllSMPServiceGroups (new PagingSpec (0, 2), null);
      assertEquals (2, aPage1.size ());
      assertTrue (aPage1.get (0).getID ().compareTo (aPage1.get (1).getID ()) < 0);

      // The second page must not overlap with the first one
      final ICommonsList <ISMPServiceGroup> aPage2 = aMgr.getAllSMPServiceGroups (new PagingSpec (2, 2), null);
      assertEquals (2, aPage2.size ());
      for (final ISMPServiceGroup aSG : aPage2)
        assertTrue ("Page 2 must not contain an entry of page 1",
                    aPage1.findFirst (x -> x.getID ().equals (aSG.getID ())) == null);

      // Descending must return the reverse order
      final ICommonsList <ISMPServiceGroup> aDesc = aMgr.getAllSMPServiceGroups (new PagingSpec (0,
                                                                                                 4,
                                                                                                 SortField.descending ("participantid")),
                                                                                 null);
      assertEquals (4, aDesc.size ());
      assertEquals (aPage1.get (0).getID (), aDesc.get (3).getID ());

      // Searching
      assertEquals (3, aMgr.getSMPServiceGroupCount ("paging"));
      assertEquals (3, aMgr.getAllSMPServiceGroups (new PagingSpec (0, 10), "paging").size ());
      assertEquals (1, aMgr.getAllSMPServiceGroups (new PagingSpec (2, 10), "paging").size ());
      assertEquals (0, aMgr.getSMPServiceGroupCount ("does-not-exist"));

      // Search is case insensitive
      assertEquals (3, aMgr.getSMPServiceGroupCount ("PAGING"));

      // An unknown sort field must be ignored, not fail
      assertEquals (4,
                    aMgr.getAllSMPServiceGroups (new PagingSpec (0, 10, SortField.ascending ("no-such-field")), null)
                        .size ());

      // No filtering at all
      assertEquals (4, aMgr.getSMPServiceGroupCount (ESMPServiceGroupFilter.ALL, null));
      assertEquals (4, aMgr.getAllSMPServiceGroups (ESMPServiceGroupFilter.ALL, new PagingSpec (0, 10), null).size ());
    }
    finally
    {
      for (final IParticipantIdentifier aPI : aCreated)
        aMgr.deleteSMPServiceGroupNoEx (aPI, true);
    }
  }

  @Test
  public void testFilters () throws SMPServerException
  {
    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);

    final IIdentifierFactory aIF = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    assertNotNull (aBusinessCardMgr);
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    assertNotNull (aParticipantMigrationMgr);

    final ICommonsList <IParticipantIdentifier> aCreated = new CommonsArrayList <> ();
    try
    {
      for (final String sValue : VALUES)
      {
        final IParticipantIdentifier aPI = aIF.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                            sValue);
        aMgr.deleteSMPServiceGroupNoEx (aPI, true);
        assertNotNull (aMgr.createSMPServiceGroup (aTestUser.getID (), aPI, null, null, true));
        aCreated.add (aPI);
      }

      // No Business Card exists yet, so all of them match
      assertEquals (VALUES.length, aMgr.getSMPServiceGroupCount (ESMPServiceGroupFilter.NO_BUSINESS_CARD, null));
      assertTrue (aMgr.containsAnySMPServiceGroup (ESMPServiceGroupFilter.NO_BUSINESS_CARD));

      // The first one gets a Business Card
      final IParticipantIdentifier aPIWithBC = aCreated.getFirstOrNull ();
      assertNotNull (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aPIWithBC, new CommonsArrayList <> (), false));

      assertEquals (VALUES.length - 1, aMgr.getSMPServiceGroupCount (ESMPServiceGroupFilter.NO_BUSINESS_CARD, null));

      final ICommonsList <ISMPServiceGroup> aNoBC = aMgr.getAllSMPServiceGroups (ESMPServiceGroupFilter.NO_BUSINESS_CARD,
                                                                                 new PagingSpec (0, 10),
                                                                                 null);
      assertEquals (VALUES.length - 1, aNoBC.size ());
      assertNull ("The Service Group with a Business Card must not be contained",
                  aNoBC.findFirst (x -> x.getParticipantIdentifier ().hasSameContent (aPIWithBC)));

      // The search text must be applied on top of the filter - "0088:paging1"
      // has a Business Card, so only 2 of the 3 remain
      assertEquals (2, aMgr.getSMPServiceGroupCount (ESMPServiceGroupFilter.NO_BUSINESS_CARD, "paging"));

      // No Participant Migration exists yet, so all of them match
      assertEquals (VALUES.length, aMgr.getSMPServiceGroupCount (ESMPServiceGroupFilter.NO_BLOCKING_MIGRATION, null));
      assertTrue (aMgr.containsAnySMPServiceGroup (ESMPServiceGroupFilter.NO_BLOCKING_MIGRATION));

      // The last one gets an outbound migration
      final IParticipantIdentifier aPIMigrating = aCreated.getLastOrNull ();
      assertNotNull (aParticipantMigrationMgr.createOutboundParticipantMigration (aPIMigrating, "migration-key"));

      assertEquals (VALUES.length - 1,
                    aMgr.getSMPServiceGroupCount (ESMPServiceGroupFilter.NO_BLOCKING_MIGRATION, null));

      final ICommonsList <ISMPServiceGroup> aNoMig = aMgr.getAllSMPServiceGroups (ESMPServiceGroupFilter.NO_BLOCKING_MIGRATION,
                                                                                  new PagingSpec (0, 10),
                                                                                  null);
      assertEquals (VALUES.length - 1, aNoMig.size ());
      assertNull ("The Service Group with a migration in progress must not be contained",
                  aNoMig.findFirst (x -> x.getParticipantIdentifier ().hasSameContent (aPIMigrating)));
    }
    finally
    {
      for (final IParticipantIdentifier aPI : aCreated)
      {
        aParticipantMigrationMgr.deleteAllParticipantMigrationsOfParticipant (aPI);
        aMgr.deleteSMPServiceGroupNoEx (aPI, true);
      }
    }
  }
}
