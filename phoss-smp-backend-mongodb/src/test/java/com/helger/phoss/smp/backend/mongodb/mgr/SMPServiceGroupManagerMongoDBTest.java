/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.PagingSpec;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.phoss.smp.backend.mongodb.SMPServerMongoDBTestRule;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupFilter;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exception.SMPServerException;

/**
 * Test class for class {@link SMPServiceGroupManagerMongoDB}.
 *
 * @author vinit-thummar
 */
public final class SMPServiceGroupManagerMongoDBTest
{
  @Rule
  public final TestRule m_aRule = new SMPServerMongoDBTestRule ();

  @Test
  public void testGetAllServiceGroupIDs () throws SMPServerException
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aParticipantID = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                                  "0088:mongodb-service-group-ids");
    assertNotNull (aParticipantID);

    final ISMPServiceGroupManager aMgr = SMPMetaManager.getServiceGroupMgr ();
    final String sOwnerID = "mongodb-service-group-owner";
    aMgr.deleteSMPServiceGroupNoEx (aParticipantID, false);
    final ISMPServiceGroup aServiceGroup = aMgr.createSMPServiceGroup (sOwnerID, aParticipantID, null, null, false);
    try
    {
      assertTrue (aMgr.getAllSMPServiceGroupIDs ().contains (aServiceGroup.getID ()));
      assertFalse (aMgr.getAllSMPServiceGroupIDs ().contains (sOwnerID));
    }
    finally
    {
      aMgr.deleteSMPServiceGroupNoEx (aParticipantID, false);
    }
  }

  @Test
  public void testFilters () throws SMPServerException
  {
    final IIdentifierFactory aIF = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    assertNotNull (aBusinessCardMgr);
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    assertNotNull (aParticipantMigrationMgr);

    final String [] aValues = { "0088:mongofilter1", "0088:mongofilter2", "0088:mongofilter3" };
    final ICommonsList <IParticipantIdentifier> aCreated = new CommonsArrayList <> ();
    try
    {
      for (final String sValue : aValues)
      {
        final IParticipantIdentifier aPI = aIF.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                            sValue);
        aMgr.deleteSMPServiceGroupNoEx (aPI, false);
        aBusinessCardMgr.deleteSMPBusinessCard (aBusinessCardMgr.getSMPBusinessCardOfID (aPI), false);
        aParticipantMigrationMgr.deleteAllParticipantMigrationsOfParticipant (aPI);
        assertNotNull (aMgr.createSMPServiceGroup ("mongodb-filter-owner", aPI, null, null, false));
        aCreated.add (aPI);
      }

      // The first one gets a Business Card
      final IParticipantIdentifier aPIWithBC = aCreated.getFirstOrNull ();
      assertNotNull (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aPIWithBC, new CommonsArrayList <> (), false));

      // Other Service Groups may exist in the database, so only the created ones are checked
      final ICommonsList <ISMPServiceGroup> aNoBC = aMgr.getAllSMPServiceGroups (ESMPServiceGroupFilter.NO_BUSINESS_CARD,
                                                                                 new PagingSpec (0, 100),
                                                                                 "mongofilter");
      assertEquals (aValues.length - 1, aNoBC.size ());
      assertNull ("The Service Group with a Business Card must not be contained",
                  aNoBC.findFirst (x -> x.getParticipantIdentifier ().hasSameContent (aPIWithBC)));
      assertEquals (aValues.length - 1,
                    aMgr.getSMPServiceGroupCount (ESMPServiceGroupFilter.NO_BUSINESS_CARD, "mongofilter"));
      assertTrue (aMgr.containsAnySMPServiceGroup (ESMPServiceGroupFilter.NO_BUSINESS_CARD));

      // The sorting must survive the $lookup
      assertTrue (aNoBC.get (0).getID ().compareTo (aNoBC.get (1).getID ()) < 0);

      // The last one gets an outbound migration
      final IParticipantIdentifier aPIMigrating = aCreated.getLastOrNull ();
      assertNotNull (aParticipantMigrationMgr.createOutboundParticipantMigration (aPIMigrating, "migration-key"));

      final ICommonsList <ISMPServiceGroup> aNoMig = aMgr.getAllSMPServiceGroups (ESMPServiceGroupFilter.NO_BLOCKING_MIGRATION,
                                                                                  new PagingSpec (0, 100),
                                                                                  "mongofilter");
      assertEquals (aValues.length - 1, aNoMig.size ());
      assertNull ("The Service Group with a migration in progress must not be contained",
                  aNoMig.findFirst (x -> x.getParticipantIdentifier ().hasSameContent (aPIMigrating)));
      assertEquals (aValues.length - 1,
                    aMgr.getSMPServiceGroupCount (ESMPServiceGroupFilter.NO_BLOCKING_MIGRATION, "mongofilter"));
      assertTrue (aMgr.containsAnySMPServiceGroup (ESMPServiceGroupFilter.NO_BLOCKING_MIGRATION));

      // Paging must be applied after the filtering
      assertEquals (1,
                    aMgr.getAllSMPServiceGroups (ESMPServiceGroupFilter.NO_BUSINESS_CARD,
                                                 new PagingSpec (1, 100),
                                                 "mongofilter")
                        .size ());
    }
    finally
    {
      for (final IParticipantIdentifier aPI : aCreated)
      {
        aParticipantMigrationMgr.deleteAllParticipantMigrationsOfParticipant (aPI);
        aBusinessCardMgr.deleteSMPBusinessCard (aBusinessCardMgr.getSMPBusinessCardOfID (aPI), false);
        aMgr.deleteSMPServiceGroupNoEx (aPI, false);
      }
    }
  }
}
