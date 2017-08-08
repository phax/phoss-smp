/**
 * Copyright (C) 2014-2017 Philip Helger (www.helger.com)
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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;

import com.helger.servlet.StaticServerInfo;

public class SMPServerRESTTestRule extends SMPServerTestRule
{
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
      aClient.register (new LoggingFeature (Logger.getLogger ("SMPServerRESTTestRule"),
                                            Level.INFO,
                                            Verbosity.PAYLOAD_ANY,
                                            null));

    m_aTarget = aClient.target (MockWebServer.BASE_URI_HTTP);
  }

  @Override
  public void after ()
  {
    try
    {
      m_aTarget = null;
      m_aServer.shutdownNow ();
    }
    finally
    {
      super.after ();
    }
  }

  @Nonnull
  public WebTarget getWebTarget ()
  {
    return m_aTarget;
  }
}
