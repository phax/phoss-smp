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
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.backend.mongodb.SMPMongoDBTestRule;

/**
 * Test class for class {@link SMLInfoManagerMongoDB}
 *
 * @author Philip Helger
 */
public final class SMLInfoManagerMongoDBTest
{
  @Rule
  public final SMPMongoDBTestRule m_aRule = new SMPMongoDBTestRule ();

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
        assertTrue (aMgr.removeSMLInfo (aCreate.getID ()).isChanged ());
      assertEquals (0, aMgr.getAllSMLInfos ().size ());
    }
  }
}
