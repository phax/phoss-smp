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
import java.net.URISyntaxException;

import javax.annotation.Nonnull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.helger.commons.exception.InitializationException;

/**
 * REST Web Service for redirection. It is called if the server root ("/") is
 * invoked - e.g. from a browser. It redirects the application to the UI start
 * page.
 *
 * @author Jerry Dimitriou
 */
@Path ("/")
public final class RedirectInterface
{
  private static final URI INDEX_HTML;

  static
  {
    try
    {
      INDEX_HTML = new URI ("/web/");
    }
    catch (final URISyntaxException e)
    {
      throw new InitializationException ("Failed to build index URI");
    }
  }

  public RedirectInterface ()
  {}

  @GET
  @Produces (MediaType.TEXT_HTML)
  @Nonnull
  public Response displayHomeURI ()
  {
    return Response.seeOther (INDEX_HTML).build ();
  }
}
