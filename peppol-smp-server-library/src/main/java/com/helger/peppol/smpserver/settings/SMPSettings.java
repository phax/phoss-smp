/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
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
    return m_aSettings.setValue (SMPServerConfiguration.KEY_SMP_REST_WRITABLE_API_DISABLED, bRESTWritableAPIDisabled);
  }

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

  public boolean isPEPPOLDirectoryIntegrationAutoUpdate ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                     SMPServerConfiguration.DEFAULT_SMP_PEPPOL_DIRECTORY_INTEGRATION_AUTO_UPDATE);
  }

  @Nonnull
  public EChange setPEPPOLDirectoryIntegrationAutoUpdate (final boolean bPEPPOLDirectoryIntegrationAutoUpdate)
  {
    return m_aSettings.setValue (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_INTEGRATION_AUTO_UPDATE,
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
    return m_aSettings.setValue (SMPServerConfiguration.KEY_SMP_PEPPOL_DIRECTORY_HOSTNAME, sPEPPOLDirectoryHostName);
  }

  public boolean isWriteToSML ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SML_ACTIVE, SMPServerConfiguration.DEFAULT_SML_ACTIVE);
  }

  @Nonnull
  public EChange setWriteToSML (final boolean bWriteToSML)
  {
    return m_aSettings.setValue (SMPServerConfiguration.KEY_SML_ACTIVE, bWriteToSML);
  }

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

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Settings", m_aSettings).getToString ();
  }
}
