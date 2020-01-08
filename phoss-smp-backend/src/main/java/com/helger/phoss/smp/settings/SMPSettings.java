/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
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
  private static final String KEY_SML_INFO_ID = "smlinfo.id";

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

  public boolean isDirectoryIntegrationRequired ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED,
                                     SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_REQUIRED);
  }

  @Nonnull
  public EChange setDirectoryIntegrationRequired (final boolean bPEPPOLDirectoryIntegrationRequired)
  {
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED,
                              bPEPPOLDirectoryIntegrationRequired);
  }

  public boolean isDirectoryIntegrationEnabled ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_ENABLED,
                                     SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_ENABLED);
  }

  @Nonnull
  public EChange setDirectoryIntegrationEnabled (final boolean bPEPPOLDirectoryIntegrationEnabled)
  {
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_ENABLED,
                              bPEPPOLDirectoryIntegrationEnabled);
  }

  public boolean isDirectoryIntegrationAutoUpdate ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                     SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE);
  }

  @Nonnull
  public EChange setDirectoryIntegrationAutoUpdate (final boolean bPEPPOLDirectoryIntegrationAutoUpdate)
  {
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                              bPEPPOLDirectoryIntegrationAutoUpdate);
  }

  @Nonnull
  public String getDirectoryHostName ()
  {
    return m_aSettings.getAsString (SMPServerConfiguration.KEY_SMP_DIRECTORY_HOSTNAME,
                                    SMPServerConfiguration.DEFAULT_SMP_DIRECTORY_HOSTNAME);
  }

  @Nonnull
  public EChange setDirectoryHostName (@Nullable final String sDirectoryHostName)
  {
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SMP_DIRECTORY_HOSTNAME, sDirectoryHostName);
  }

  public boolean isSMLRequired ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SML_REQUIRED,
                                     SMPServerConfiguration.DEFAULT_SML_REQUIRED);
  }

  @Nonnull
  public EChange setSMLRequired (final boolean bSMLRequired)
  {
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SML_REQUIRED, bSMLRequired);
  }

  public boolean isSMLEnabled ()
  {
    return m_aSettings.getAsBoolean (SMPServerConfiguration.KEY_SML_ENABLED,
                                     SMPServerConfiguration.DEFAULT_SML_ENABLED);
  }

  @Nonnull
  public EChange setSMLEnabled (final boolean bSMLEnabled)
  {
    return m_aSettings.putIn (SMPServerConfiguration.KEY_SML_ENABLED, bSMLEnabled);
  }

  @Nullable
  private String _getSMLInfoID ()
  {
    return m_aSettings.getAsString (KEY_SML_INFO_ID);
  }

  @Nullable
  public ISMLInfo getSMLInfo ()
  {
    final String sID = _getSMLInfoID ();
    return SMPMetaManager.getSMLInfoMgr ().getSMLInfoOfID (sID);
  }

  @Nullable
  @Override
  public String getSMLInfoID ()
  {
    return _getSMLInfoID ();
  }

  @Nonnull
  public EChange setSMLInfoID (@Nullable final String sSMLInfoID)
  {
    return m_aSettings.putIn (KEY_SML_INFO_ID, sSMLInfoID);
  }

  @Nonnull
  @ReturnsMutableCopy
  public SettingsWithDefault getAsSettings ()
  {
    return m_aSettings;
  }

  @Nonnull
  EChange initFromSettings (@Nonnull final ISettings aSettings)
  {
    ValueEnforcer.notNull (aSettings, "settings");
    final EChange ret = m_aSettings.setAll (aSettings);
    // Soft migration 5.0.7
    if (!m_aSettings.containsKey (KEY_SML_INFO_ID))
    {
      // Get old String (includes "/manageparticipantidentifier")
      final String sOldSMLURL = m_aSettings.getAsString ("sml.url");
      if (StringHelper.hasText (sOldSMLURL))
      {
        // Check if any SML item matches
        final ISMLInfo aSMLInfo = SMPMetaManager.getSMLInfoMgr ()
                                                .findFirstWithManageParticipantIdentifierEndpointAddress (sOldSMLURL);
        if (aSMLInfo != null)
          m_aSettings.put (KEY_SML_INFO_ID, aSMLInfo.getID ());
      }
    }
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Settings", m_aSettings).getToString ();
  }
}
