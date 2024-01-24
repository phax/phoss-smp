/*
 * Copyright (C) 2014-2024 Philip Helger and contributors
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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.photon.jetty.JettyRunner;

/**
 * WebServer based on Jetty for standalone SMP server testing. It starts a
 * server on Port 9090 using the context path "/unittest".
 *
 * @author Philip Helger
 */
@Immutable
final class MockWebServer
{
  public static final String CONTEXT_PATH = "/unittest";
  public static final int PORT = 9090;
  public static final int STOP_PORT = PORT - 1;

  private MockWebServer ()
  {}

  /**
   * Starts HTTP server exposing JAX-RS resources defined in this application.
   *
   * @return The Jetty runner
   */
  @Nonnull
  public static JettyRunner startRegularServer ()
  {
    final JettyRunner ret = new JettyRunner ();
    ret.setContextPath (CONTEXT_PATH).setPort (PORT).setStopPort (STOP_PORT);
    try
    {
      ret.startServer ();
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to start server", ex);
    }
    return ret;
  }
}
