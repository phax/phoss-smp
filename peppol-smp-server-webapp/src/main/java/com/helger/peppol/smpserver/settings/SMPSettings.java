package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.settings.ISettings;
import com.helger.settings.Settings;

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

  @Nonnull
  @ReturnsMutableCopy
  public ISettings getAsSettings ()
  {
    final Settings ret = new Settings ("smp");
    ret.setValue ("peppol.directory.integration.enabled", m_bPEPPOLDirectoryIntegrationEnabled);
    return ret;
  }

  public void setFromSettings (@Nonnull final ISettings aSettings)
  {
    ValueEnforcer.notNull (aSettings, "settings");
    m_bPEPPOLDirectoryIntegrationEnabled = aSettings.getAsBoolean ("peppol.directory.integration.enabled");
  }
}
