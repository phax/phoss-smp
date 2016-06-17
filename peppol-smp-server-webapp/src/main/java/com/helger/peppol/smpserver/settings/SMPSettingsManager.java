package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.state.EChange;

@ThreadSafe
public class SMPSettingsManager
{
  private static SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  private static SMPSettings s_aSettings = new SMPSettings ();

  public SMPSettingsManager ()
  {}

  @Nonnull
  public static ISMPSettings getSettings ()
  {
    return s_aSettings;
  }

  public static EChange updateSettings (final boolean bPEPPOLDirectoryIntegrationEnabled)
  {
    EChange eChange = EChange.UNCHANGED;
    eChange = eChange.or (s_aSettings.setPEPPOLDirectoryIntegrationEnabled (bPEPPOLDirectoryIntegrationEnabled));
    if (eChange.isChanged ())
    {

    }
    return eChange;
  }
}
