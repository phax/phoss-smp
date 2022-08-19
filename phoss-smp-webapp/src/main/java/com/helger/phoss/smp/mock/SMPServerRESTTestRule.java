/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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
package com.helger.phoss.smp.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.lang.ClassPathHelper;
import com.helger.config.Config;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.res.ConfigurationSourceProperties;
import com.helger.peppol.sml.ESML;
import com.helger.phoss.smp.SMPConfigProvider;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.photon.jetty.JettyRunner;

public class SMPServerRESTTestRule extends ExternalResource
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPServerRESTTestRule.class);

  private IConfig m_aOldConfig;
  private IConfig m_aNewConfig;
  private JettyRunner m_aServer;

  @Nonnull
  public static MultiConfigurationValueProvider createSMPClientValueProvider (@Nonnull final IReadableResource aRes)
  {
    // Start with default setup
    final MultiConfigurationValueProvider ret = ConfigFactory.createDefaultValueProvider ();

    // Lower priority than the standard files
    ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8),
                                ConfigFactory.APPLICATION_PROPERTIES_PRIORITY - 1);

    return ret;
  }

  public SMPServerRESTTestRule (@Nullable final IReadableResource aSMPServerProperties)
  {
    if (aSMPServerProperties != null && aSMPServerProperties.exists ())
    {
      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("Creating custom SMP configuration using " + aSMPServerProperties);

      // Remember the old config
      m_aOldConfig = SMPConfigProvider.getConfig ();
      // Create new config
      m_aNewConfig = Config.create (createSMPClientValueProvider (aSMPServerProperties));
    }
  }

  @Override
  public void before () throws Throwable
  {
    if (m_aNewConfig != null)
      SMPConfigProvider.setConfig (m_aNewConfig);

    if (false)
      ClassPathHelper.forAllClassPathEntries (LOGGER::info);

    // http only
    m_aServer = MockWebServer.startRegularServer ();

    // Ensure non-invasive setup
    // PD enabled but no auto-update
    // SML disabled
    SMPMetaManager.getSettingsMgr ()
                  .updateSettings (false, true, false, false, "dummy", false, false, ESML.DEVELOPMENT_LOCAL.getID ());

    LOGGER.info ("Finished SMPServerRESTTestRule before. Listening at '" + getFullURL () + "'");
  }

  @Override
  public void after ()
  {
    try
    {
      LOGGER.info ("Shutting down SMP server");
      if (m_aServer != null)
        m_aServer.shutDownServer ();

      // Reset for next run
      SMPMetaManager.setManagerProvider (null);
      LOGGER.info ("Finished shutting down SMP server");
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to shut down server", ex);
    }
    catch (final InterruptedException ex)
    {
      LOGGER.error ("Failed to shut down server", ex);
      Thread.currentThread ().interrupt ();
    }
    finally
    {
      // Restore old config
      if (m_aOldConfig != null)
        SMPConfigProvider.setConfig (m_aOldConfig);
    }
  }

  /**
   * @return The full URL with port and context path that is the basis for the
   *         running test instance.
   */
  @Nonnull
  @Nonempty
  public final String getFullURL ()
  {
    return "http://localhost:" + MockWebServer.PORT + MockWebServer.CONTEXT_PATH;
  }
}
