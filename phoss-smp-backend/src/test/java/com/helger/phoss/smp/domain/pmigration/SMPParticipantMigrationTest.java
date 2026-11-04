/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.pmigration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.base.string.StringHelper;
import com.helger.datetime.helper.PDTFactory;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.xml.mock.XMLTestHelper;

/**
 * Test class for class {@link SMPParticipantMigration}
 *
 * @author Philip Helger
 */
public final class SMPParticipantMigrationTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testOutbound ()
  {
    final IParticipantIdentifier aPI = SimpleIdentifierFactory.INSTANCE.createParticipantIdentifier ("foo", "bar");
    final SMPParticipantMigration pm = SMPParticipantMigration.createOutbound (aPI,
                                                                               ManageParticipantIdentifierServiceCaller.createRandomMigrationKey ());
    assertNotNull (pm);
    assertTrue (StringHelper.isNotEmpty (pm.getID ()));
    assertEquals (EParticipantMigrationDirection.OUTBOUND, pm.getDirection ());
    assertSame (aPI, pm.getParticipantIdentifier ());
    assertTrue (PDTFactory.getCurrentLocalDateTime ().compareTo (pm.getInitiationDateTime ()) >= 0);
    assertTrue (StringHelper.isNotEmpty (pm.getMigrationKey ()));
    XMLTestHelper.testMicroTypeConversion (pm);
  }

  @Test
  public void testReplaceOutboundInProgress () throws Exception
  {
    final ISMPParticipantMigrationManager aMgr = new SMPParticipantMigrationManagerXML ("test-migrations-" +
                                                                                        UUID.randomUUID () +
                                                                                        ".xml");
    final IParticipantIdentifier aPI = SimpleIdentifierFactory.INSTANCE.createParticipantIdentifier ("foo", "replace");
    final ISMPParticipantMigration aFirst = aMgr.createOutboundParticipantMigration (aPI, "key1");
    assertNotNull (aFirst);
    assertTrue (aMgr.containsOutboundMigrationInProgress (aPI));

    // Replace the key, like a second prepareToMigrate call does
    assertTrue (aMgr.deleteParticipantMigrationOfID (aFirst.getID ()).isChanged ());
    final ISMPParticipantMigration aSecond = aMgr.createOutboundParticipantMigration (aPI, "key2");
    assertNotNull (aSecond);

    assertTrue (aMgr.containsOutboundMigrationInProgress (aPI));
    final ISMPParticipantMigration aCurrent = aMgr.getParticipantMigrationOfParticipantID (EParticipantMigrationDirection.OUTBOUND,
                                                                                           EParticipantMigrationState.IN_PROGRESS,
                                                                                           aPI);
    assertNotNull (aCurrent);
    assertEquals ("key2", aCurrent.getMigrationKey ());
    assertEquals (1, aMgr.getAllOutboundParticipantMigrations (EParticipantMigrationState.IN_PROGRESS)
                         .getCount (x -> x.getParticipantIdentifier ().hasSameContent (aPI)));

    aMgr.deleteParticipantMigrationOfID (aSecond.getID ());
  }

  @Test
  public void testInbound ()
  {
    final IParticipantIdentifier aPI = SimpleIdentifierFactory.INSTANCE.createParticipantIdentifier ("foo", "bar");
    final SMPParticipantMigration pm = SMPParticipantMigration.createInbound (aPI, "11AAbb$$");
    assertNotNull (pm);
    assertTrue (StringHelper.isNotEmpty (pm.getID ()));
    assertEquals (EParticipantMigrationDirection.INBOUND, pm.getDirection ());
    assertSame (aPI, pm.getParticipantIdentifier ());
    assertTrue (PDTFactory.getCurrentLocalDateTime ().compareTo (pm.getInitiationDateTime ()) >= 0);
    assertEquals ("11AAbb$$", pm.getMigrationKey ());
    XMLTestHelper.testMicroTypeConversion (pm);
  }
}
