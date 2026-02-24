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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ESMPTransportProfileState;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.phoss.smp.mock.SMPServerTestRule;

/**
 * Test class for class {@link SMPTransportProfileManagerMongoDB}.
 *
 * @author Philip Helger
 */
public final class SMPTransportProfileManagerMongoDBTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

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
                                                                             e.getState () == ESMPTransportProfileState.DEPRECATED);
        aCreated.add (aCreate);
      }
      final ICommonsList <ISMPTransportProfile> aAll = aMgr.getAllSMPTransportProfiles ();
      assertEquals (ESMPTransportProfile.values ().length, aAll.size ());
      for (final ISMPTransportProfile aCreate : aCreated)
        assertTrue (aAll.contains (aCreate));
      for (final ISMPTransportProfile aCreate : aCreated)
        assertTrue (aMgr.updateSMPTransportProfile (aCreate.getID (),
                                                    "bla " + aCreate.getName (),
                                                    aCreate.getState () == ESMPTransportProfileState.DEPRECATED)
                        .isChanged ());
      for (final ISMPTransportProfile aCreate : aCreated)
      {
        final ISMPTransportProfile aInfo = aMgr.getSMPTransportProfileOfID (aCreate.getID ());
        assertNotNull (aInfo);
        assertTrue (aInfo.getName ().startsWith ("bla "));
      }
      for (final ISMPTransportProfile aCreate : aCreated)
        assertTrue (aMgr.deleteSMPTransportProfile (aCreate.getID ()).isChanged ());
      assertEquals (0, aMgr.getAllSMPTransportProfiles ().size ());
    }
  }
}
