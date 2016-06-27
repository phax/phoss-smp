/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
  private static final String KEY_PEPPOL_DIRECTORY_INTEGRATION_ENABLED = "peppol.directory.integration.enabled";

  private boolean m_bPEPPOLDirectoryIntegrationEnabled;

  public SMPSettings ()
  {
    setToConfigurationValues ();
  }

  public void setToConfigurationValues ()
  {
    m_bPEPPOLDirectoryIntegrationEnabled = SMPServerConfiguration.isPEPPOLDirectoryIntegrationEnabled ();
  }

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
    ret.setValue (KEY_PEPPOL_DIRECTORY_INTEGRATION_ENABLED, m_bPEPPOLDirectoryIntegrationEnabled);
    return ret;
  }

  public void setFromSettings (@Nonnull final ISettings aSettings)
  {
    ValueEnforcer.notNull (aSettings, "settings");
    m_bPEPPOLDirectoryIntegrationEnabled = aSettings.getAsBoolean (KEY_PEPPOL_DIRECTORY_INTEGRATION_ENABLED);
  }
}
