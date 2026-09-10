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
package com.helger.phoss.smp.backend.xml.mgr;

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
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.totp.ISMPUserTotp;
import com.helger.phoss.smp.domain.totp.ISMPUserTotpManager;
import com.helger.phoss.smp.domain.totp.SMPTotpHelper;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;

/**
 * Test class for class {@link com.helger.phoss.smp.domain.totp.SMPUserTotpManagerXML}.
 *
 * @author Philip Helger
 */
public final class SMPUserTotpManagerXMLTest
{
  private static final String USER_ID = CSecurity.USER_ADMINISTRATOR_ID;

  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testBasic ()
  {
    final ISMPUserTotpManager aMgr = SMPMetaManager.getUserTotpMgr ();
    assertNotNull (aMgr);
    aMgr.deleteTotp (USER_ID);

    assertNull (aMgr.getTotpOfUserID (USER_ID));
    assertTrue (aMgr.getAllTotps ().isEmpty ());

    final String sSecret = SMPTotpHelper.createNewSecret ();
    final ISMPUserTotp aTotp = aMgr.createOrReplaceTotp (USER_ID, sSecret);
    assertNotNull (aTotp);
    assertEquals (USER_ID, aTotp.getID ());
    assertEquals (sSecret, aTotp.getSecret ());
    assertFalse (aTotp.isEnabled ());
    assertNull (aTotp.getLastUsedTimeSlot ());
    assertEquals (1, aMgr.getAllTotps ().size ());

    try
    {
      assertTrue (aMgr.setTotpEnabled (USER_ID, true).isChanged ());
      assertTrue (aMgr.getTotpOfUserID (USER_ID).isEnabled ());
      assertTrue (aMgr.setTotpEnabled (USER_ID, true).isUnchanged ());
    }
    finally
    {
      assertTrue (aMgr.deleteTotp (USER_ID).isChanged ());
    }
    assertNull (aMgr.getTotpOfUserID (USER_ID));
    assertTrue (aMgr.deleteTotp (USER_ID).isUnchanged ());
  }

  @Test
  public void testLastUsedTimeSlotIsMonotonic ()
  {
    final ISMPUserTotpManager aMgr = SMPMetaManager.getUserTotpMgr ();
    assertNotNull (aMgr);
    aMgr.deleteTotp (USER_ID);
    aMgr.createOrReplaceTotp (USER_ID, SMPTotpHelper.createNewSecret ());
    try
    {
      // The first value is always accepted
      assertTrue (aMgr.setTotpLastUsedTimeSlot (USER_ID, 1000).isChanged ());
      assertEquals (Long.valueOf (1000), aMgr.getTotpOfUserID (USER_ID).getLastUsedTimeSlot ());

      // Replay of the same slot must be rejected
      assertTrue (aMgr.setTotpLastUsedTimeSlot (USER_ID, 1000).isUnchanged ());
      // Replay of an older slot must be rejected
      assertTrue (aMgr.setTotpLastUsedTimeSlot (USER_ID, 999).isUnchanged ());
      assertEquals (Long.valueOf (1000), aMgr.getTotpOfUserID (USER_ID).getLastUsedTimeSlot ());

      // A newer slot is accepted
      assertTrue (aMgr.setTotpLastUsedTimeSlot (USER_ID, 1001).isChanged ());
      assertEquals (Long.valueOf (1001), aMgr.getTotpOfUserID (USER_ID).getLastUsedTimeSlot ());

      // Unknown user
      assertTrue (aMgr.setTotpLastUsedTimeSlot ("unknown-user", 1001).isUnchanged ());
    }
    finally
    {
      aMgr.deleteTotp (USER_ID);
    }
  }

  @Test
  public void testRecoveryCodes ()
  {
    final ISMPUserTotpManager aMgr = SMPMetaManager.getUserTotpMgr ();
    assertNotNull (aMgr);
    aMgr.deleteTotp (USER_ID);
    aMgr.createOrReplaceTotp (USER_ID, SMPTotpHelper.createNewSecret ());
    try
    {
      assertEquals (0, aMgr.getTotpOfUserID (USER_ID).getRecoveryCodeCount ());

      final ICommonsList <String> aCodes = SMPTotpHelper.createNewRecoveryCodes ();
      final ICommonsList <String> aHashes = new CommonsArrayList <> ();
      for (final String sCode : aCodes)
        aHashes.add (SMPTotpHelper.getRecoveryCodeHash (sCode));

      assertTrue (aMgr.setRecoveryCodeHashes (USER_ID, aHashes).isChanged ());
      assertEquals (aHashes.size (), aMgr.getTotpOfUserID (USER_ID).getRecoveryCodeCount ());

      // Consume the first code
      final String sHash0 = aHashes.getFirstOrNull ();
      assertTrue (aMgr.consumeRecoveryCodeHash (USER_ID, sHash0).isChanged ());
      assertEquals (aHashes.size () - 1, aMgr.getTotpOfUserID (USER_ID).getRecoveryCodeCount ());

      // Each code can be used exactly once
      assertTrue (aMgr.consumeRecoveryCodeHash (USER_ID, sHash0).isUnchanged ());
      assertEquals (aHashes.size () - 1, aMgr.getTotpOfUserID (USER_ID).getRecoveryCodeCount ());

      // Unknown code and unknown user
      assertTrue (aMgr.consumeRecoveryCodeHash (USER_ID, "whatever").isUnchanged ());
      assertTrue (aMgr.consumeRecoveryCodeHash (USER_ID, null).isUnchanged ());
      assertTrue (aMgr.consumeRecoveryCodeHash ("unknown-user", sHash0).isUnchanged ());

      // New codes replace the old ones
      assertTrue (aMgr.setRecoveryCodeHashes (USER_ID, new CommonsArrayList <> ("h1", "h2")).isChanged ());
      assertEquals (2, aMgr.getTotpOfUserID (USER_ID).getRecoveryCodeCount ());
      assertTrue (aMgr.consumeRecoveryCodeHash (USER_ID, aHashes.get (1)).isUnchanged ());
      assertTrue (aMgr.consumeRecoveryCodeHash (USER_ID, "h1").isChanged ());
    }
    finally
    {
      aMgr.deleteTotp (USER_ID);
    }
  }
}
