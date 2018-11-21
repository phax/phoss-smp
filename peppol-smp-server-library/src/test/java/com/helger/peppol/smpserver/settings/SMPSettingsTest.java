package com.helger.peppol.smpserver.settings;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for class {@link SMPSettings}.
 *
 * @author Philip Helger
 */
public class SMPSettingsTest
{
  @Test
  public void testBasic ()
  {
    final SMPSettings aSettings = new SMPSettings ();
    assertTrue (aSettings.isPEPPOLDirectoryIntegrationRequired ());
  }
}
