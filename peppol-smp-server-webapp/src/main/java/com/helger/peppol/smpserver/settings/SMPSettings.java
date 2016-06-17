package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.state.EChange;
import com.helger.peppol.smpserver.SMPServerConfiguration;

/**
 * This class contains the settings to be applied for the current SMP instance.
 * It is based on the {@link SMPServerConfiguration} but allows for changes at
 * runtime!
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPSettings implements ISMPSettings
{
  private boolean m_bPEPPOLDirectoryIntegrationEnabled = SMPServerConfiguration.isPEPPOLDirectoryIntegrationEnabled ();

  public SMPSettings ()
  {}

  public boolean isPEPPOLDirectoryIntegrationEnabled ()
  {
    return m_bPEPPOLDirectoryIntegrationEnabled;
  }

  @Nonnull
  public EChange setPEPPOLDirectoryIntegrationEnabled (final boolean bPEPPOLDirectoryIntegrationEnabled)
  {
    if (bPEPPOLDirectoryIntegrationEnabled == m_bPEPPOLDirectoryIntegrationEnabled)
      return EChange.UNCHANGED;
    m_bPEPPOLDirectoryIntegrationEnabled = bPEPPOLDirectoryIntegrationEnabled;
    return EChange.CHANGED;
  }
}
