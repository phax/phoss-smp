/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.mongodb.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.phoss.smp.backend.mongodb.SMPMongoDBTestRule;

/**
 * Test class for class {@link SMPTransportProfileManagerMongoDB}.
 *
 * @author Philip Helger
 */
public final class SMPTransportProfileManagerMongoDBTest
{
  @Rule
  public final SMPMongoDBTestRule m_aRule = new SMPMongoDBTestRule ();

  @Test
  public void testBasic ()
  {
    try (final SMPTransportProfileManagerMongoDB aMgr = new SMPTransportProfileManagerMongoDB ())
    {
      assertEquals (0, aMgr.getAllSMPTransportProfiles ().size ());
      final ICommonsList <ISMPTransportProfile> aCreated = aMgr.getAllSMPTransportProfiles ();
      for (final ESMPTransportProfile e : ESMPTransportProfile.values ())
      {
        final ISMPTransportProfile aCreate = aMgr.createSMPTransportProfile (e.getID (),
                                                                             e.getName (),
                                                                             e.isDeprecated ());
        aCreated.add (aCreate);
      }
      final ICommonsList <ISMPTransportProfile> aAll = aMgr.getAllSMPTransportProfiles ();
      assertEquals (ESMPTransportProfile.values ().length, aAll.size ());
      for (final ISMPTransportProfile aCreate : aCreated)
        assertTrue (aAll.contains (aCreate));
      for (final ISMPTransportProfile aCreate : aCreated)
        assertTrue (aMgr.updateSMPTransportProfile (aCreate.getID (),
                                                    "bla " + aCreate.getName (),
                                                    aCreate.isDeprecated ())
                        .isChanged ());
      for (final ISMPTransportProfile aCreate : aCreated)
      {
        final ISMPTransportProfile aInfo = aMgr.getSMPTransportProfileOfID (aCreate.getID ());
        assertNotNull (aInfo);
        assertTrue (aInfo.getName ().startsWith ("bla "));
      }
      for (final ISMPTransportProfile aCreate : aCreated)
        assertTrue (aMgr.removeSMPTransportProfile (aCreate.getID ()).isChanged ());
      assertEquals (0, aMgr.getAllSMPTransportProfiles ().size ());
    }
  }
}
