package com.helger.phoss.smp.backend.mongodb.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.peppol.sml.ESML;
import com.helger.phoss.smp.backend.mongodb.SMPMongoDBTestRule;
import com.helger.phoss.smp.settings.ISMPSettings;

/**
 * Test class for class {@link SMPSettingsManagerMongoDB}.
 *
 * @author Philip Helger
 */
public final class SMPSettingsManagerMongoDBTest
{
  @Rule
  public final SMPMongoDBTestRule m_aRule = new SMPMongoDBTestRule ();

  @Test
  public void testBasic ()
  {
    try (final SMPSettingsManagerMongoDB aMgr = new SMPSettingsManagerMongoDB ())
    {
      final ISMPSettings aSettings = aMgr.getSettings ();
      assertNotNull (aSettings);
      aMgr.updateSettings (true, true, true, true, "v1", true, true, ESML.DEVELOPMENT_LOCAL);
      assertTrue (aSettings.isRESTWritableAPIDisabled ());
      assertTrue (aSettings.isDirectoryIntegrationRequired ());
      assertTrue (aSettings.isDirectoryIntegrationEnabled ());
      assertTrue (aSettings.isDirectoryIntegrationAutoUpdate ());
      assertEquals ("v1", aSettings.getDirectoryHostName ());
      assertTrue (aSettings.isSMLRequired ());
      assertTrue (aSettings.isSMLEnabled ());
      assertEquals (ESML.DEVELOPMENT_LOCAL, aSettings.getSMLInfo ());

      aMgr.updateSettings (false, false, false, false, "v2", false, false, ESML.DIGIT_TEST);
      assertFalse (aSettings.isRESTWritableAPIDisabled ());
      assertFalse (aSettings.isDirectoryIntegrationRequired ());
      assertFalse (aSettings.isDirectoryIntegrationEnabled ());
      assertFalse (aSettings.isDirectoryIntegrationAutoUpdate ());
      assertEquals ("v2", aSettings.getDirectoryHostName ());
      assertFalse (aSettings.isSMLRequired ());
      assertFalse (aSettings.isSMLEnabled ());
      assertEquals (ESML.DIGIT_TEST, aSettings.getSMLInfo ());
    }
  }
}
