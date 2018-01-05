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
package com.helger.peppol.smpserver.standalone;

import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.photon.jetty.JettyStarter;

/**
 * Run this as an application and your SMP will be up and running on port 90 (to
 * not interfere with anything already running on port 80) of your local
 * machine. Please ensure that you have adopted the DB configuration file.<br>
 * To stop the running Jetty simply invoke the {@link JettyStopSMPSERVER_SQL}
 * application in this package. It performs a graceful shutdown of the App
 * Server.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class RunInJettySMPSERVER_SQL
{
  public static void main (final String... args) throws Exception
  {
    SMPServerConfiguration.getConfigFile ().applyAllNetworkSystemProperties ();
    new JettyStarter (RunInJettySMPSERVER_SQL.class).setPort (90).setStopPort (8078).run ();
  }
}
