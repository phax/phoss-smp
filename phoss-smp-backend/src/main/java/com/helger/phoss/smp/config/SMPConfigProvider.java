/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.debug.GlobalDebug;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.exception.InitializationException;
import com.helger.base.string.StringHelper;
import com.helger.base.system.SystemProperties;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.source.MultiConfigurationValueProvider;

/**
 * The global configuration provider for SMP V6.
 *
 * @author Philip Helger
 * @since 6.0.0
 */
@ThreadSafe
public final class SMPConfigProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPConfigProvider.class);

  static
  {
    // TODO remove block in v7
    // smp-server.properties stuff
    if (StringHelper.isNotEmpty (SystemProperties.getPropertyValueOrNull ("peppol.smp.server.properties.path")))
      throw new InitializationException ("The system property 'peppol.smp.server.properties.path' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.isNotEmpty (SystemProperties.getPropertyValueOrNull ("smp.server.properties.path")))
      throw new InitializationException ("The system property 'smp.server.properties.path' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.isNotEmpty (System.getenv ().get ("SMP_SERVER_CONFIG")))
      throw new InitializationException ("The environment variable 'SMP_SERVER_CONFIG' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the environment variable 'CONFIG_FILE' instead.");

    // webapp.properties stuff
    if (StringHelper.isNotEmpty (SystemProperties.getPropertyValueOrNull ("peppol.smp.webapp.properties.path")))
      throw new InitializationException ("The system property 'peppol.smp.webapp.properties.path' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.isNotEmpty (SystemProperties.getPropertyValueOrNull ("smp.webapp.properties.path")))
      throw new InitializationException ("The system property 'smp.webapp.properties.path' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.isNotEmpty (System.getenv ().get ("SMP_WEBAPP_CONFIG")))
      throw new InitializationException ("The environment variable 'SMP_WEBAPP_CONFIG' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the environment variable 'CONFIG_FILE' instead.");
  }

  /**
   * @return The configuration value provider for SMP client that contains backward compatibility
   *         support.
   */
  @NonNull
  public static MultiConfigurationValueProvider createSMPClientValueProvider ()
  {
    // Start with default setup
    final MultiConfigurationValueProvider ret = ConfigFactory.createDefaultValueProvider ();

    return ret;
  }

  private static final IConfigWithFallback DEFAULT_CONFIG = new SMPConfig (createSMPClientValueProvider ());

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static IConfigWithFallback s_aConfig = DEFAULT_CONFIG;

  private SMPConfigProvider ()
  {}

  /**
   * @return The current global configuration. Never <code>null</code>.
   */
  @NonNull
  public static IConfigWithFallback getConfig ()
  {
    // Inline for performance
    RW_LOCK.readLock ().lock ();
    try
    {
      return s_aConfig;
    }
    finally
    {
      RW_LOCK.readLock ().unlock ();
    }
  }

  /**
   * Overwrite the global configuration. This is only needed for testing.
   *
   * @param aNewConfig
   *        The configuration to use globally. May not be <code>null</code>.
   * @return The old value of {@link IConfig}. Never <code>null</code>.
   */
  @NonNull
  public static IConfigWithFallback setConfig (@NonNull final IConfigWithFallback aNewConfig)
  {
    ValueEnforcer.notNull (aNewConfig, "NewConfig");
    final IConfigWithFallback aOld;
    RW_LOCK.writeLock ().lock ();
    try
    {
      aOld = s_aConfig;
      s_aConfig = aNewConfig;
    }
    finally
    {
      RW_LOCK.writeLock ().unlock ();
    }
    if (!EqualsHelper.identityEqual (aOld, aNewConfig))
      if (!GlobalDebug.isProductionMode ())
        LOGGER.info ("The SMP configuration provider was changed to " + aNewConfig);
    return aOld;
  }
}
