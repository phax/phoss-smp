/*
 * Copyright (C) 2015-2024 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.settings;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.value.ConfiguredValue;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.config.SMPConfigProvider;
import com.helger.phoss.smp.config.SMPServerConfiguration;
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
  private static final String KEY_SMP_REST_WRITABLE_API_DISABLED = "smp.rest.writableapi.disabled";

  private static final String KEY_SML_REQUIRED = "sml.required";
  private static final String KEY_SML_REQUIRED_OLD = "sml.needed";
  private static final String KEY_SML_ENABLED = "sml.enabled";
  private static final String KEY_SML_ENABLED_OLD = "sml.active";
  // No matching configuration item
  private static final String KEY_SML_INFO_ID = "smlinfo.id";

  private static final String KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED = "smp.directory.integration.required";
  private static final String KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED_OLD = "smp.peppol.directory.integration.required";
  private static final String KEY_SMP_DIRECTORY_INTEGRATION_ENABLED = "smp.directory.integration.enabled";
  private static final String KEY_SMP_DIRECTORY_INTEGRATION_ENABLED_OLD = "smp.peppol.directory.integration.enabled";
  private static final String KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE = "smp.directory.integration.autoupdate";
  private static final String KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE_OLD = "smp.peppol.directory.integration.autoupdate";
  private static final String KEY_SMP_DIRECTORY_HOSTNAME = "smp.directory.hostname";
  private static final String KEY_SMP_DIRECTORY_HOSTNAME_OLD = "smp.peppol.directory.hostname";

  public static final boolean DEFAULT_SMP_REST_WRITABLE_API_DISABLED = false;

  public static final boolean DEFAULT_SML_REQUIRED = true;
  public static final boolean DEFAULT_SML_ENABLED = false;

  public static final boolean DEFAULT_SMP_DIRECTORY_INTEGRATION_REQUIRED = true;
  public static final boolean DEFAULT_SMP_DIRECTORY_INTEGRATION_ENABLED = true;
  public static final boolean DEFAULT_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE = true;
  public static final String DEFAULT_SMP_DIRECTORY_HOSTNAME_PROD = "https://directory.peppol.eu";
  public static final String DEFAULT_SMP_DIRECTORY_HOSTNAME_TEST = "https://test-directory.peppol.eu";

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPSettings.class);
  private static final ObjectType OT = new ObjectType ("smp-settings");

  private final Settings m_aSettings = new Settings ("smp-settings");

  public SMPSettings (final boolean bInitFromConfiguration)
  {
    if (bInitFromConfiguration)
    {
      // Create settings and use the values from the configuration file as the
      // default values
      final IConfigWithFallback aConfig = SMPConfigProvider.getConfig ();

      final Consumer <String> aGetSet = sKey -> {
        final ConfiguredValue aCV = aConfig.getConfiguredValue (sKey);
        if (aCV != null)
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Initializing settings property '" + sKey + "' with Configuration property " + aCV);
          m_aSettings.putIn (sKey, aCV.getValue ());
        }
      };
      final BiConsumer <String, String []> aGetSetMulti = (sKey, aOldOnes) -> {
        final ConfiguredValue aCV = aConfig.getConfiguredValueOrFallback (sKey, aOldOnes);
        if (aCV != null)
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Initializing settings property '" + sKey + "' with Configuration property " + aCV);
          m_aSettings.putIn (sKey, aCV.getValue ());
        }
      };

      // SML Info ID is not taken from Config!
      aGetSet.accept (KEY_SMP_REST_WRITABLE_API_DISABLED);
      aGetSetMulti.accept (KEY_SML_REQUIRED, new String [] { KEY_SML_REQUIRED_OLD });
      aGetSetMulti.accept (KEY_SML_ENABLED, new String [] { KEY_SML_ENABLED_OLD });
      aGetSetMulti.accept (KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED,
                           new String [] { KEY_SMP_DIRECTORY_INTEGRATION_REQUIRED_OLD });
      aGetSetMulti.accept (KEY_SMP_DIRECTORY_INTEGRATION_ENABLED,
                           new String [] { KEY_SMP_DIRECTORY_INTEGRATION_ENABLED_OLD });
      aGetSetMulti.accept (KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                           new String [] { KEY_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE_OLD });
      aGetSetMulti.accept (KEY_SMP_DIRECTORY_HOSTNAME, new String [] { KEY_SMP_DIRECTORY_HOSTNAME_OLD });
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
    return m_aSettings.getAsBoolean (KEY_SMP_DIRECTORY_INTEGRATION_ENABLED,
                                     DEFAULT_SMP_DIRECTORY_INTEGRATION_ENABLED);
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

  @Nonnull
  @ReturnsMutableCopy
  final Settings internalGetAsMutableSettings ()
  {
    return m_aSettings;
  }

  @Nonnull
  final EChange internalSetFromSettings (@Nonnull final ISettings aSettings)
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
