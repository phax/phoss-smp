/*
 * Copyright (C) 2019-2022 Philip Helger and contributors
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
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.phoss.smp.settings.ISMPSettings;

/**
 * Test class for class {@link SMPSettingsManagerMongoDB}.
 *
 * @author Philip Helger
 */
public final class SMPSettingsManagerMongoDBTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testBasic ()
  {
    final ISMLInfoManager aSMLInfoMgr = SMPMetaManager.getSMLInfoMgr ();

    final ISMLInfo aSMLInfo = aSMLInfoMgr.createSMLInfo ("bla", "foo", "http://bar", true);
    assertNotNull (aSMLInfo);

    try (final SMPSettingsManagerMongoDB aMgr = new SMPSettingsManagerMongoDB ())
    {
      final ISMPSettings aSettings = aMgr.getSettings ();
      assertNotNull (aSettings);
      aMgr.updateSettings (true, true, true, true, "v1", true, true, aSMLInfo.getID ());
      assertTrue (aSettings.isRESTWritableAPIDisabled ());
      assertTrue (aSettings.isDirectoryIntegrationRequired ());
      assertTrue (aSettings.isDirectoryIntegrationEnabled ());
      assertTrue (aSettings.isDirectoryIntegrationAutoUpdate ());
      assertEquals ("v1", aSettings.getDirectoryHostName ());
      assertTrue (aSettings.isSMLRequired ());
      assertTrue (aSettings.isSMLEnabled ());
      assertEquals (aSMLInfo, aSettings.getSMLInfo ());

      aMgr.updateSettings (false, false, false, false, "v2", false, false, aSMLInfo.getID ());
      assertFalse (aSettings.isRESTWritableAPIDisabled ());
      assertFalse (aSettings.isDirectoryIntegrationRequired ());
      assertFalse (aSettings.isDirectoryIntegrationEnabled ());
      assertFalse (aSettings.isDirectoryIntegrationAutoUpdate ());
      assertEquals ("v2", aSettings.getDirectoryHostName ());
      assertFalse (aSettings.isSMLRequired ());
      assertFalse (aSettings.isSMLEnabled ());
      assertEquals (aSMLInfo, aSettings.getSMLInfo ());
    }
    finally
    {
      aSMLInfoMgr.deleteSMLInfo (aSMLInfo.getID ());
    }
  }
}
