/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
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

import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.phoss.smp.mock.SMPServerTestRule;

/**
 * Test class for class {@link SMLInfoManagerMongoDB}
 *
 * @author Philip Helger
 */
public final class SMLInfoManagerMongoDBTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testBasic ()
  {
    try (final SMLInfoManagerMongoDB aMgr = new SMLInfoManagerMongoDB ())
    {
      assertEquals (0, aMgr.getAllSMLInfos ().size ());
      final ICommonsList <ISMLInfo> aCreated = aMgr.getAllSMLInfos ();
      for (final ESML e : ESML.values ())
      {
        final ISMLInfo aCreate = aMgr.createSMLInfo (e.getDisplayName (),
                                                     e.getDNSZone (),
                                                     e.getManagementServiceURL (),
                                                     e.isClientCertificateRequired ());
        aCreated.add (aCreate);
      }
      final ICommonsList <ISMLInfo> aAll = aMgr.getAllSMLInfos ();
      assertEquals (ESML.values ().length, aAll.size ());
      for (final ISMLInfo aCreate : aCreated)
        assertTrue (aAll.contains (aCreate));
      for (final ISMLInfo aCreate : aCreated)
        assertTrue (aMgr.updateSMLInfo (aCreate.getID (),
                                        "bla " + aCreate.getDisplayName (),
                                        aCreate.getDNSZone (),
                                        aCreate.getManagementServiceURL (),
                                        aCreate.isClientCertificateRequired ())
                        .isChanged ());
      for (final ISMLInfo aCreate : aCreated)
      {
        final ISMLInfo aInfo = aMgr.getSMLInfoOfID (aCreate.getID ());
        assertNotNull (aInfo);
        assertTrue (aInfo.getDisplayName ().startsWith ("bla "));
      }
      for (final ISMLInfo aCreate : aCreated)
        assertTrue (aMgr.deleteSMLInfo (aCreate.getID ()).isChanged ());
      assertEquals (0, aMgr.getAllSMLInfos ().size ());
    }
  }

  @Test
  public void testConversion ()
  {
    final ISMLInfo aInfo = new SMLInfo ("displayName", "DNSZone", "https://url/url", true);
    final Document aSrc = SMLInfoManagerMongoDB.toBson (aInfo);
    assertNotNull (aSrc);

    final ISMLInfo aSrc2 = SMLInfoManagerMongoDB.toDomain (aSrc);
    assertNotNull (aSrc2);
    assertEquals (aInfo, aSrc2);
  }
}
