package com.helger.peppol.smpserver.settings;

/**
 * Runtime settings for this SMP server instance.
 * 
 * @author Philip Helger
 */
public interface ISMPSettings
{
  /**
   * @return <code>true</code> if PEPPOL Directory integration is enabled,
   *         <code>false</code> otherwise.
   */
  boolean isPEPPOLDirectoryIntegrationEnabled ();
}
