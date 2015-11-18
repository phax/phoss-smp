package com.helger.peppol.smpserver.rest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;

import com.helger.peppol.smpserver.SMPServerTestRule;

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
