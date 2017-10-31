/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
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

  public final void setToConfigurationValues ()
  {
    // Create settings and use the values from the configuration file as the
    // default values
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
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SMP_REST_WRITABLE_API_DISABLED, bRESTWritableAPIDisabled);
  }

  public boolean isPEPPOLDirectoryIntegrationEnabled ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED,
                                     SMPServerConfiguration.DEFAULT_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED);
  }

  @Nonnull
  public EChange setPEPPOLDirectoryIntegrationEnabled (final boolean bPEPPOLDirectoryIntegrationEnabled)
  {
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED,
                              bPEPPOLDirectoryIntegrationEnabled);
  }

  public boolean isPEPPOLDirectoryIntegrationAutoUpdate ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                     SMPServerConfiguration.DEFAULT_SMP_PEPPOL_DIRECTORY_INTEGRATION_AUTO_UPDATE);
  }

  @Nonnull
  public EChange setPEPPOLDirectoryIntegrationAutoUpdate (final boolean bPEPPOLDirectoryIntegrationAutoUpdate)
  {
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                              bPEPPOLDirectoryIntegrationAutoUpdate);
  }

  @Nonnull
  public String getPEPPOLDirectoryHostName ()
  {
    return m_aSettings.getAsString (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_HOSTNAME,
                                    SMPServerConfiguration.DEFAULT_SMP_PEPPOL_DIRECTORY_HOSTNAME);
  }

  @Nonnull
  public EChange setPEPPOLDirectoryHostName (@Nullable final String sPEPPOLDirectoryHostName)
  {
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_HOSTNAME, sPEPPOLDirectoryHostName);
  }

  public boolean isWriteToSML ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SML_ACTIVE, SMPServerConfiguration.DEFAULT_SML_ACTIVE);
  }

  @Nonnull
  public EChange setWriteToSML (final boolean bWriteToSML)
  {
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SML_ACTIVE, bWriteToSML);
  }

  @Nullable
  public String getSMLURL ()
  {
    return m_aSettings.getAsString (SMPServerConfiguration.KEY_SML_URL);
  }

  @Nonnull
  public EChange setSMLURL (@Nullable final String sSMLURL)
  {
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SML_URL, sSMLURL);
  }

  @Nonnull
  @ReturnsMutableCopy
  public SettingsWithDefault getAsSettings ()
  {
    return m_aSettings;
  }

  public void setFromSettings (@Nonnull final ISettings aSettings)
  {
    ValueEnforcer.notNull (aSettings, "settings");
    m_aSettings.clear ();
    m_aSettings.putAllIn (aSettings);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Settings", m_aSettings).getToString ();
  }
}
