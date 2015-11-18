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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.uri.UriComponent;

/**
 * Main class.
 */
final class MockServer
{
  // Base URI the Grizzly HTTP server will listen on
  public static final String BASE_URI_HTTP = "http://localhost:9090/unittest/";

  @Nonnull
  private static WebappContext _createContext (final URI u,
                                               final Class <? extends Servlet> aServletClass,
                                               final Servlet aServlet,
                                               final Map <String, String> aInitParams,
                                               final Map <String, String> aContextInitParams)
  {
    String path = u.getPath ();
    if (path == null)
      throw new IllegalArgumentException ("The URI path, of the URI " + u + ", must be non-null");
    if (path.isEmpty ())
      throw new IllegalArgumentException ("The URI path, of the URI " + u + ", must be present");
    if (path.charAt (0) != '/')
      throw new IllegalArgumentException ("The URI path, of the URI " + u + ". must start with a '/'");
    path = String.format ("/%s", UriComponent.decodePath (u.getPath (), true).get (1).toString ());

    final WebappContext aContext = new WebappContext ("GrizzlyContext", path);
    ServletRegistration aRegistration;
    if (aServletClass != null)
      aRegistration = aContext.addServlet (aServletClass.getName (), aServletClass);
    else
      aRegistration = aContext.addServlet (aServlet.getClass ().getName (), aServlet);
    aRegistration.addMapping ("/*");

    if (aContextInitParams != null)
      for (final Map.Entry <String, String> e : aContextInitParams.entrySet ())
        aContext.setInitParameter (e.getKey (), e.getValue ());

    if (aInitParams != null)
      aRegistration.setInitParameters (aInitParams);

    return aContext;
  }

  @Nonnull
  private static WebappContext _createContext (final String sURI)
  {
    final Map <String, String> aInitParams = new HashMap <String, String> ();
    aInitParams.put ("jersey.config.server.provider.packages",
                     com.helger.peppol.smpserver.rest.ServiceGroupInterface.class.getPackage ().getName () +
                                                               "," +
                                                               com.helger.peppol.smpserver.exceptionmapper.RuntimeExceptionMapper.class.getPackage ()
                                                                                                                                       .getName ());
    return _createContext (URI.create (sURI), ServletContainer.class, null, aInitParams, null);
  }

  /**
   * Starts Grizzly HTTP server exposing JAX-RS resources defined in this
   * application.
   *
   * @return Grizzly HTTP server.
   */
  @Nonnull
  public static HttpServer startRegularServer ()
  {
    final WebappContext aContext = _createContext (BASE_URI_HTTP);

    // create and start a new instance of grizzly http server
    // exposing the Jersey application at BASE_URI
    final HttpServer ret = GrizzlyHttpServerFactory.createHttpServer (URI.create (BASE_URI_HTTP));
    aContext.deploy (ret);
    return ret;
  }
}
