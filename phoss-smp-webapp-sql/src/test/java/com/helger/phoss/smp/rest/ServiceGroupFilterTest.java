/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.PagingSpec;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardName;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupFilter;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Test class for the server side Service Group filters of the SQL backend.
 *
 * @author Philip Helger
 */
public final class ServiceGroupFilterTest extends AbstractSMPWebAppSQLTest
{
  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (PROPERTIES_FILE);

  private static final String [] VALUES = { "9999:sgfilter1", "9999:sgfilter2", "9999:sgfilter3" };

  @Test
  public void testFilters () throws SMPServerException
  {
    if (SMPMetaManager.getInstance ().getBackendConnectionState ().isFalse ())
    {
      // Seems like the database is not running
      return;
    }

    final ISMPServiceGroupManager aMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    assertNotNull (aBusinessCardMgr);
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    assertNotNull (aParticipantMigrationMgr);

    try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      final ICommonsList <IParticipantIdentifier> aCreated = new CommonsArrayList <> ();
      try
      {
        for (final String sValue : VALUES)
        {
          final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (sValue);
          aMgr.deleteSMPServiceGroupNoEx (aPI, true);
          assertNotNull (aMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null, null, true));
          aCreated.add (aPI);
        }

        // Other Service Groups may exist in the database, so all queries are limited to the ones
        // created here by the search text
        assertEquals (VALUES.length, aMgr.getSMPServiceGroupCount (ESMPServiceGroupFilter.NO_BUSINESS_CARD, "sgfilter"));

        // The first one gets a Business Card - it needs at least one entity, because the SQL
        // backend stores one row per entity
        final IParticipantIdentifier aPIWithBC = aCreated.getFirstOrNull ();
        final SMPBusinessCardEntity aEntity = new SMPBusinessCardEntity ();
        aEntity.names ().add (new SMPBusinessCardName ("Entity 1", null));
        aEntity.setCountryCode ("AT");
        assertNotNull (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aPIWithBC,
                                                                       new CommonsArrayList <> (aEntity),
                                                                       false));

        assertEquals (VALUES.length - 1,
                      aMgr.getSMPServiceGroupCount (ESMPServiceGroupFilter.NO_BUSINESS_CARD, "sgfilter"));

        final ICommonsList <ISMPServiceGroup> aNoBC = aMgr.getAllSMPServiceGroups (ESMPServiceGroupFilter.NO_BUSINESS_CARD,
                                                                                   new PagingSpec (0, 100),
                                                                                   "sgfilter");
        assertEquals (VALUES.length - 1, aNoBC.size ());
        assertNull ("The Service Group with a Business Card must not be contained",
                    aNoBC.findFirst (x -> x.getParticipantIdentifier ().hasSameContent (aPIWithBC)));
        assertTrue (aMgr.containsAnySMPServiceGroup (ESMPServiceGroupFilter.NO_BUSINESS_CARD));

        // The ORDER BY must still be applied
        assertTrue (aNoBC.get (0).getID ().compareTo (aNoBC.get (1).getID ()) < 0);

        // Paging must be applied after the filtering
        assertEquals (1,
                      aMgr.getAllSMPServiceGroups (ESMPServiceGroupFilter.NO_BUSINESS_CARD,
                                                   new PagingSpec (1, 100),
                                                   "sgfilter")
                          .size ());

        // The last one gets an outbound migration
        final IParticipantIdentifier aPIMigrating = aCreated.getLastOrNull ();
        assertNotNull (aParticipantMigrationMgr.createOutboundParticipantMigration (aPIMigrating, "migration-key"));

        assertEquals (VALUES.length - 1,
                      aMgr.getSMPServiceGroupCount (ESMPServiceGroupFilter.NO_BLOCKING_MIGRATION, "sgfilter"));

        final ICommonsList <ISMPServiceGroup> aNoMig = aMgr.getAllSMPServiceGroups (ESMPServiceGroupFilter.NO_BLOCKING_MIGRATION,
                                                                                    new PagingSpec (0, 100),
                                                                                    "sgfilter");
        assertEquals (VALUES.length - 1, aNoMig.size ());
        assertNull ("The Service Group with a migration in progress must not be contained",
                    aNoMig.findFirst (x -> x.getParticipantIdentifier ().hasSameContent (aPIMigrating)));
        assertTrue (aMgr.containsAnySMPServiceGroup (ESMPServiceGroupFilter.NO_BLOCKING_MIGRATION));
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
}
