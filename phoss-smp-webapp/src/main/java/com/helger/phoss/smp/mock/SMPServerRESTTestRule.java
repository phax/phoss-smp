/**
 * Copyright (C) 2014-2021 Philip Helger and contributors
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

import javax.annotation.Nullable;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.lang.ClassPathHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.peppol.sml.ESML;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.photon.jetty.JettyRunner;

public class SMPServerRESTTestRule extends ExternalResource
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPServerRESTTestRule.class);

  private JettyRunner m_aServer;

  public SMPServerRESTTestRule (@Nullable final String sSMPServerPropertiesPath)
  {
    SystemProperties.setPropertyValue (SMPServerConfiguration.SYSTEM_PROPERTY_SMP_SERVER_PROPERTIES_PATH, sSMPServerPropertiesPath);
    SMPServerConfiguration.reloadConfiguration ();
  }

  @Override
  public void before () throws Throwable
  {
    super.before ();

    if (false)
      ClassPathHelper.forAllClassPathEntries (LOGGER::info);

    // http only
    m_aServer = MockWebServer.startRegularServer ();

    // Ensure non-invasive setup
    // PD enabled but no auto-update
    // SML disabled
    SMPMetaManager.getSettingsMgr ().updateSettings (false, true, false, false, "dummy", false, false, ESML.DEVELOPMENT_LOCAL);

    LOGGER.info ("Finished SMPServerRESTTestRule before");
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
      LOGGER.info ("super.after");
      super.after ();
    }
  }

  public String getFullURL ()
  {
    return "http://localhost:" + MockWebServer.PORT + MockWebServer.CONTEXT_PATH;
  }
}
