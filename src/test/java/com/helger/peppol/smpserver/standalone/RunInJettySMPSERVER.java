/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.standalone;

import java.io.File;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.SystemProperties;

/**
 * Run this as an application and your SML will be up and running on port 8080
 * of your local machine. Please ensure that you have adopted the Hibernate
 * configuration file.<br>
 * To stop the running Jetty simply invoke the {@link JettyStopSMPSERVER}
 * application in this package. It performs a graceful shutdown of the App
 * Server.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class RunInJettySMPSERVER
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (RunInJettySMPSERVER.class);
  private static final String RESOURCE_PREFIX = "target/webapp-classes";

  public static void main (final String... args) throws Exception
  {
    if (System.getSecurityManager () != null)
      throw new IllegalStateException ("Security Manager is set but not supported - aborting!");

    // Create main server
    final Server aServer = new Server ();
    // Create connector on Port 80 - must be 80 according to PEPPOL specs!
    final Connector aConnector = new SelectChannelConnector ();
    aConnector.setPort (80);
    aConnector.setMaxIdleTime (30000);
    aConnector.setStatsOn (true);
    aServer.setConnectors (new Connector [] { aConnector });

    final WebAppContext aWebAppCtx = new WebAppContext ();
    aWebAppCtx.setDescriptor (RESOURCE_PREFIX + "/WEB-INF/web.xml");
    aWebAppCtx.setResourceBase (RESOURCE_PREFIX);
    aWebAppCtx.setContextPath ("/");
    aWebAppCtx.setTempDirectory (new File (SystemProperties.getTmpDir () + '/' + RunInJettySMPSERVER.class.getName ()));
    aWebAppCtx.setParentLoaderPriority (true);
    aServer.setHandler (aWebAppCtx);
    final ServletContextHandler aCtx = aWebAppCtx;

    // Setting final properties
    // Stops the server when ctrl+c is pressed (registers to
    // Runtime.addShutdownHook)
    aServer.setStopAtShutdown (true);
    // Send the server version in the response header?
    aServer.setSendServerVersion (true);
    // Send the date header in the response header?
    aServer.setSendDateHeader (true);
    // Allows requests (prior to shutdown) to finish gracefully
    aServer.setGracefulShutdown (1000);
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
