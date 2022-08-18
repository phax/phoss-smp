/*
 * Copyright (C) 2015-2022 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.config.IConfig;
import com.helger.config.value.ConfiguredValue;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.SMPConfigSource;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
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
  public static final String KEY_SMP_REST_WRITABLE_API_DISABLED = "smp.rest.writableapi.disabled";
  /* legacy name - should not contain "peppol" */
  public static final String KEY_SMP_DIRECTORY_INTEGRATION_ENABLED = "smp.peppol.directory.integration.enabled";
  /* legacy name - should not contain "peppol" */
  public static final String KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED = "smp.peppol.directory.integration.required";
  /* legacy name - should not contain "peppol" */
  public static final String KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE = "smp.peppol.directory.integration.autoupdate";
  /* legacy name - should not contain "peppol" */
  public static final String KEY_SMP_DIRECTORY_HOSTNAME = "smp.peppol.directory.hostname";
  /* legacy name - should be called "required" instead of "needed" */
  public static final String KEY_SML_REQUIRED = "sml.needed";
  /* legacy name - should be called "enabled" instead of "active" */
  public static final String KEY_SML_ENABLED = "sml.active";
  private static final String KEY_SML_INFO_ID = "smlinfo.id";

  public static final boolean DEFAULT_SMP_REST_WRITABLE_API_DISABLED = false;
  public static final boolean DEFAULT_SMP_DIRECTORY_INTEGRATION_REQUIRED = true;
  public static final boolean DEFAULT_SMP_DIRECTORY_INTEGRATION_ENABLED = true;
  public static final boolean DEFAULT_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE = true;
  public static final String DEFAULT_SMP_DIRECTORY_HOSTNAME_PROD = "https://directory.peppol.eu";
  public static final String DEFAULT_SMP_DIRECTORY_HOSTNAME_TEST = "https://test-directory.peppol.eu";
  public static final boolean DEFAULT_SML_REQUIRED = true;
  public static final boolean DEFAULT_SML_ENABLED = false;

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPSettings.class);
  private static final ObjectType OT = new ObjectType ("smp-settings");

  private final Settings m_aSettings = new Settings ("smp-settings");

  public SMPSettings (final boolean bInitFromConfiguration)
  {
    if (bInitFromConfiguration)
    {
      // Create settings and use the values from the configuration file as the
      // default values
      final IConfig aConfig = SMPConfigSource.getConfig ();
      // SML Info ID is not taken from Config!
      for (final String sKey : new String [] { KEY_SMP_REST_WRITABLE_API_DISABLED,
                                               KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED,
                                               KEY_SMP_DIRECTORY_INTEGRATION_ENABLED,
                                               KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                               KEY_SMP_DIRECTORY_HOSTNAME,
                                               KEY_SML_REQUIRED,
                                               KEY_SML_ENABLED })
      {
        final ConfiguredValue aCV = aConfig.getConfiguredValue (sKey);
        if (aCV != null)
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Initializing settings property '" + sKey + "' with Configuration property " + aCV);
          m_aSettings.putIn (sKey, aCV.getValue ());
        }
      }
    }
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

  public boolean isRESTWritableAPIDisabled ()
  {
    return m_aSettings.getAsBoolean (KEY_SMP_REST_WRITABLE_API_DISABLED, DEFAULT_SMP_REST_WRITABLE_API_DISABLED);
  }

  @Nonnull
  public EChange setRESTWritableAPIDisabled (final boolean bRESTWritableAPIDisabled)
  {
    return m_aSettings.putIn (KEY_SMP_REST_WRITABLE_API_DISABLED, bRESTWritableAPIDisabled);
  }

  public boolean isDirectoryIntegrationRequired ()
  {
    return m_aSettings.getAsBoolean (KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED,
                                     DEFAULT_SMP_DIRECTORY_INTEGRATION_REQUIRED);
  }

  @Nonnull
  public EChange setDirectoryIntegrationRequired (final boolean bPeppolDirectoryIntegrationRequired)
  {
    return m_aSettings.putIn (KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED, bPeppolDirectoryIntegrationRequired);
  }

  public boolean isDirectoryIntegrationEnabled ()
  {
    return m_aSettings.getAsBoolean (KEY_SMP_DIRECTORY_INTEGRATION_ENABLED, DEFAULT_SMP_DIRECTORY_INTEGRATION_ENABLED);
  }

  @Nonnull
  public EChange setDirectoryIntegrationEnabled (final boolean bPeppolDirectoryIntegrationEnabled)
  {
    return m_aSettings.putIn (KEY_SMP_DIRECTORY_INTEGRATION_ENABLED, bPeppolDirectoryIntegrationEnabled);
  }

  public boolean isDirectoryIntegrationAutoUpdate ()
  {
    return m_aSettings.getAsBoolean (KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                     DEFAULT_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE);
  }

  @Nonnull
  public EChange setDirectoryIntegrationAutoUpdate (final boolean bPeppolDirectoryIntegrationAutoUpdate)
  {
    return m_aSettings.putIn (KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE, bPeppolDirectoryIntegrationAutoUpdate);
  }

  @Nonnull
  public String getDirectoryHostName ()
  {
    // TODO select between test and prod in default
    return m_aSettings.getAsString (KEY_SMP_DIRECTORY_HOSTNAME,
                                    true ? DEFAULT_SMP_DIRECTORY_HOSTNAME_PROD : DEFAULT_SMP_DIRECTORY_HOSTNAME_TEST);
  }

  @Nonnull
  public EChange setDirectoryHostName (@Nullable final String sDirectoryHostName)
  {
    return m_aSettings.putIn (KEY_SMP_DIRECTORY_HOSTNAME, sDirectoryHostName);
  }

  public boolean isSMLRequired ()
  {
    return m_aSettings.getAsBoolean (KEY_SML_REQUIRED, DEFAULT_SML_REQUIRED);
  }

  @Nonnull
  public EChange setSMLRequired (final boolean bSMLRequired)
  {
    return m_aSettings.putIn (KEY_SML_REQUIRED, bSMLRequired);
  }

  public boolean isSMLEnabled ()
  {
    return m_aSettings.getAsBoolean (KEY_SML_ENABLED, DEFAULT_SML_ENABLED);
  }

  @Nonnull
  public EChange setSMLEnabled (final boolean bSMLEnabled)
  {
    return m_aSettings.putIn (KEY_SML_ENABLED, bSMLEnabled);
  }

  @Nullable
  @Override
  public String getSMLInfoID ()
  {
    return m_aSettings.getAsString (KEY_SML_INFO_ID);
  }

  @Nullable
  public ISMLInfo getSMLInfo ()
  {
    final String sID = getSMLInfoID ();
    return SMPMetaManager.getSMLInfoMgr ().getSMLInfoOfID (sID);
  }

  @Nonnull
  public EChange setSMLInfoID (@Nullable final String sSMLInfoID)
  {
    return m_aSettings.putIn (KEY_SML_INFO_ID, sSMLInfoID);
  }

  @Nonnull
  @ReturnsMutableCopy
  final Settings getAsMutableSettings ()
  {
    return m_aSettings;
  }

  @Nonnull
  final EChange initFromSettings (@Nonnull final ISettings aSettings)
  {
    ValueEnforcer.notNull (aSettings, "settings");
    return m_aSettings.setAll (aSettings);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Settings", m_aSettings).getToString ();
  }
}
