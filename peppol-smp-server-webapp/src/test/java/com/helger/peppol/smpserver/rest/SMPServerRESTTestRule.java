/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.rest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;

import com.helger.peppol.smpserver.SMPServerTestRule;
import com.helger.web.servlet.server.StaticServerInfo;

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
    m_aServer = MockServer.startRegularServer ();

    final Client aClient = ClientBuilder.newClient ();
    m_aTarget = aClient.target (MockServer.BASE_URI_HTTP);
  }

  @Override
  public void after ()
  {
    m_aTarget = null;
    m_aServer.shutdownNow ();

    super.after ();
  }

  @Nonnull
  public WebTarget getWebTarget ()
  {
    return m_aTarget;
  }
}
