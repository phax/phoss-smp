/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.mock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.servlet.StaticServerInfo;

public class SMPServerRESTTestRule extends SMPServerTestRule
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPServerRESTTestRule.class);

  private HttpServer m_aServer;
  private WebTarget m_aTarget;

  public SMPServerRESTTestRule (@Nullable final String sSMPServerPropertiesPath)
  {
    super (sSMPServerPropertiesPath);
  }

  @Override
  public void before ()
  {
    super.before ();

    // Init once
    if (!StaticServerInfo.isSet ())
      StaticServerInfo.init ("http", "localhost", 80, "");

    // http only
    m_aServer = MockWebServer.startRegularServer ();

    final Client aClient = ClientBuilder.newClient ();

    // Enable the feature to activate logging of HTTP requests
    if (false)
      aClient.register (new LoggingFeature (java.util.logging.Logger.getLogger ("SMPServerRESTTestRule"),
                                            java.util.logging.Level.INFO,
                                            Verbosity.PAYLOAD_ANY,
                                            null));

    m_aTarget = aClient.target (MockWebServer.BASE_URI_HTTP);
    LOGGER.info ("Finished before");
  }

  @Override
  public void after ()
  {
    try
    {
      LOGGER.info ("Shutting down server");
      m_aTarget = null;
      if (m_aServer != null)
        m_aServer.shutdownNow ();
      LOGGER.info ("Finished shutting down server");
    }
    finally
    {
      LOGGER.info ("super.after");
      super.after ();
    }
  }

  @Nonnull
  public WebTarget getWebTarget ()
  {
    return m_aTarget;
  }
}
