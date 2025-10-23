/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.standalone;

import java.io.File;

import com.helger.photon.jetty.JettyStarter;

/**
 * Run this as an application and your SMP will be up and running on port 90 (to not interfere with
 * anything already running on port 80) of your local machine. Please ensure that you have adopted
 * the DB configuration file.<br>
 * To stop the running Jetty simply invoke the {@link JettyStopSMPSERVER_XML} application in this
 * package. It performs a graceful shutdown of the App Server.
 *
 * @author Philip Helger
 */
public final class RunInJettySMPSERVER_XML
{
  public static void main (final String... args) throws Exception
  {
    if (!new File ("pom.xml").exists ())
      throw new IllegalStateException ("Please make sure your working directory is the directory containing 'pom.xml'");

    new JettyStarter (RunInJettySMPSERVER_XML.class).setPort (90)
                                                    .setStopPort (x -> x + 1000)
                                                    .setSessionCookieName ("SMPSESSION")
                                                    // .setContextPath ("/smp")
                                                    .run ();
  }
}
