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
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.commons.type.ObjectType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.settings.ISettings;
import com.helger.settings.SettingsWithDefault;

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
  private static final ObjectType OT = new ObjectType ("smp-settings");

  private SettingsWithDefault m_aSettings;

  public SMPSettings ()
  {
    setToConfigurationValues ();
  }

  @Nonnull
  public ObjectType getObjectType ()
  {
    return OT;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return "singleton";
  }

  public void setToConfigurationValues ()
  {
    m_aSettings = new SettingsWithDefault (SMPServerConfiguration.getConfigFile ().getSettings ());
  }

  public boolean isRESTWritableAPIDisabled ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SMP_REST_WRITABLE_API_DISABLED,
                                     SMPServerConfiguration.DEFAULT_SMP_REST_WRITABLE_API_DISABLED);
  }

  @Nonnull
  public EChange setRESTWritableAPIDisabled (final boolean bRESTWritableAPIDisabled)
  {
    return m_aSettings.setValue (SMPServerConfiguration.KEY_SMP_REST_WRITABLE_API_DISABLED, bRESTWritableAPIDisabled);
  }

  /**
   * Check if the PEPPOL Directory integration (offering the /businesscard API)
   * is enabled.
   *
   * @return <code>true</code> if it is enabled, <code>false</code> otherwise.
   *         By default it is disabled.
   */
  public boolean isPEPPOLDirectoryIntegrationEnabled ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED,
                                     SMPServerConfiguration.DEFAULT_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED);
  }

  @Nonnull
  public EChange setPEPPOLDirectoryIntegrationEnabled (final boolean bPEPPOLDirectoryIntegrationEnabled)
  {
    return m_aSettings.setValue (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED,
                                 bPEPPOLDirectoryIntegrationEnabled);
  }

  /**
   * @return The host name of the PEPPOL Directory server. Never
   *         <code>null</code>.
   */
  @Nonnull
  public String getPEPPOLDirectoryHostName ()
  {
    return m_aSettings.getAsString (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_HOSTNAME,
                                    SMPServerConfiguration.DEFAULT_SMP_PEPPOL_DIRECTORY_HOSTNAME);
  }

  @Nonnull
  public EChange setPEPPOLDirectoryHostName (@Nullable final String sPEPPOLDirectoryHostName)
  {
    return m_aSettings.setValue (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_HOSTNAME, sPEPPOLDirectoryHostName);
  }

  /**
   * @return <code>true</code> if the SML connection is active,
   *         <code>false</code> if not. Property <code>sml.active</code>.
   */
  public boolean isWriteToSML ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SML_ACTIVE, SMPServerConfiguration.DEFAULT_SML_ACTIVE);
  }

  @Nonnull
  public EChange setWriteToSML (final boolean bWriteToSML)
  {
    return m_aSettings.setValue (SMPServerConfiguration.KEY_SML_ACTIVE, bWriteToSML);
  }

  /**
   * @return The SML URL to use. Only relevant when {@link #isWriteToSML()} is
   *         <code>true</code>. Property <code>sml.url</code>.
   */
  @Nullable
  public String getSMLURL ()
  {
    return m_aSettings.getAsString (SMPServerConfiguration.KEY_SML_URL);
  }

  @Nonnull
  public EChange setSMLURL (@Nullable final String sSMLURL)
  {
    return m_aSettings.setValue (SMPServerConfiguration.KEY_SML_URL, sSMLURL);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ISettings getAsSettings ()
  {
    return m_aSettings;
  }

  public void setFromSettings (@Nonnull final ISettings aSettings)
  {
    ValueEnforcer.notNull (aSettings, "settings");
    m_aSettings.clear ();
    m_aSettings.setValues (aSettings);
  }
}
