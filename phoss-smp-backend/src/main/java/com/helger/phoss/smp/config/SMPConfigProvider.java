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
package com.helger.phoss.smp.config;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.resourceprovider.ReadableResourceProviderChain;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.res.ConfigurationSourceProperties;

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
    if (StringHelper.hasText (SystemProperties.getPropertyValueOrNull ("peppol.smp.server.properties.path")))
      throw new InitializationException ("The system property 'peppol.smp.server.properties.path' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.hasText (SystemProperties.getPropertyValueOrNull ("smp.server.properties.path")))
      throw new InitializationException ("The system property 'smp.server.properties.path' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.hasText (System.getenv ().get ("SMP_SERVER_CONFIG")))
      throw new InitializationException ("The environment variable 'SMP_SERVER_CONFIG' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the environment variable 'CONFIG_FILE' instead.");

    // webapp.properties stuff
    if (StringHelper.hasText (SystemProperties.getPropertyValueOrNull ("peppol.smp.webapp.properties.path")))
      throw new InitializationException ("The system property 'peppol.smp.webapp.properties.path' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.hasText (SystemProperties.getPropertyValueOrNull ("smp.webapp.properties.path")))
      throw new InitializationException ("The system property 'smp.webapp.properties.path' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.hasText (System.getenv ().get ("SMP_WEBAPP_CONFIG")))
      throw new InitializationException ("The environment variable 'SMP_WEBAPP_CONFIG' is no longer supported." +
                                         " All configuration properties are in 'application.properties' since v6.0.0." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the environment variable 'CONFIG_FILE' instead.");
  }

  /**
   * @return The configuration value provider for SMP client that contains
   *         backward compatibility support.
   */
  @Nonnull
  public static MultiConfigurationValueProvider createSMPClientValueProvider ()
  {
    // Start with default setup
    final MultiConfigurationValueProvider ret = ConfigFactory.createDefaultValueProvider ();

    final ReadableResourceProviderChain aResourceProvider = ConfigFactory.createDefaultResourceProviderChain ();

    IReadableResource aRes;
    final int nBasePrio = ConfigFactory.APPLICATION_PROPERTIES_PRIORITY;

    final String sDefaultSuffix = "\n  Place all properties in 'application.properties' instead. !This fallback will be removed in version 7 of phoss SMP!";

    // TODO remove block in v7
    // Lower priority than the standard files
    aRes = aResourceProvider.getReadableResourceIf ("private-smp-server.properties", IReadableResource::exists);
    if (aRes != null)
    {
      LOGGER.warn ("The support for the properties file '" + aRes.getAsURL () + "' is deprecated." + sDefaultSuffix);
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nBasePrio - 1);
    }
    aRes = aResourceProvider.getReadableResourceIf ("smp-server.properties", IReadableResource::exists);
    if (aRes != null)
    {
      LOGGER.warn ("The support for the properties file '" + aRes.getAsURL () + "' is deprecated." + sDefaultSuffix);
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nBasePrio - 2);
    }
    aRes = aResourceProvider.getReadableResourceIf ("private-webapp.properties", IReadableResource::exists);
    if (aRes != null)
    {
      LOGGER.warn ("The support for the properties file '" + aRes.getAsURL () + "' is deprecated." + sDefaultSuffix);
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nBasePrio - 1);
    }
    aRes = aResourceProvider.getReadableResourceIf ("webapp.properties", IReadableResource::exists);
    if (aRes != null)
    {
      LOGGER.warn ("The support for the properties file '" + aRes.getAsURL () + "' is deprecated." + sDefaultSuffix);
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nBasePrio - 2);
    }
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
  @Nonnull
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
  @Nonnull
  public static IConfigWithFallback setConfig (@Nonnull final IConfigWithFallback aNewConfig)
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
