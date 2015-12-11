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
package com.helger.peppol.smpserver.standalone;

import java.io.File;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.system.SystemProperties;

/**
 * Run this as an application and your SML will be up and running on port 8080
 * of your local machine. Please ensure that you have adopted the Hibernate
 * configuration file.<br>
 * To stop the running Jetty simply invoke the {@link JettyStopSMPSERVER_SQLMIN}
 * application in this package. It performs a graceful shutdown of the App
 * Server.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class RunInJettySMPSERVER_SQLMIN
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (RunInJettySMPSERVER_SQLMIN.class);
  private static final String RESOURCE_PREFIX = "target/webapp-classes";

  public static void main (final String... args) throws Exception
  {
    if (System.getSecurityManager () != null)
      throw new IllegalStateException ("Security Manager is set but not supported - aborting!");

    // Create main server
    final Server aServer = new Server ();
    // Create connector on Port
    final ServerConnector aConnector = new ServerConnector (aServer);
    aConnector.setPort (90);
    aConnector.setIdleTimeout (30000);
    // aConnector.setStatsOn (true);
    aServer.setConnectors (new Connector [] { aConnector });

    final WebAppContext aWebAppCtx = new WebAppContext ();
    aWebAppCtx.setDescriptor (RESOURCE_PREFIX + "/WEB-INF/web.xml");
    aWebAppCtx.setResourceBase (RESOURCE_PREFIX);
    aWebAppCtx.setContextPath ("/");
    aWebAppCtx.setTempDirectory (new File (SystemProperties.getTmpDir () + '/' + RunInJettySMPSERVER_SQLMIN.class.getName ()));
    aWebAppCtx.setParentLoaderPriority (true);
    // Important to add the AnnotationConfiguration!
    aWebAppCtx.setConfigurations (new Configuration [] { new WebInfConfiguration (),
                                                         new WebXmlConfiguration (),
                                                         new MetaInfConfiguration (),
                                                         new FragmentConfiguration (),
                                                         new JettyWebXmlConfiguration (),
                                                         new AnnotationConfiguration () });
    aServer.setHandler (aWebAppCtx);
    final ServletContextHandler aCtx = aWebAppCtx;

    // Setting final properties
    // Stops the server when ctrl+c is pressed (registers to
    // Runtime.addShutdownHook)
    aServer.setStopAtShutdown (true);
    // Starting shutdown listener thread
    new JettyMonitor ().start ();
    try
    {
      // Starting the engines:
      aServer.start ();
      if (aCtx.isFailed ())
      {
        s_aLogger.error ("Failed to start server - stopping server!");
        aServer.stop ();
        s_aLogger.error ("Failed to start server - stopped server!");
      }
      else
      {
        // Running the server!
        aServer.join ();
      }
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to run server!", ex);
    }
  }
}
