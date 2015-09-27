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
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * This class adds a XML DSIG to successful GET's for SignedServiceMetadata
 * objects.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class SignatureFilter implements ContainerResponseFilter
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SignatureFilter.class);

  public SignatureFilter ()
  {}

  @Nonnull
  public ContainerResponse filter (@Nonnull final ContainerRequest aRequest, @Nonnull final ContainerResponse aResponse)
  {
    // Make sure that the signature is only added to GET/OK on service metadata.
    if (aRequest.getMethod ().equals ("GET") && aResponse.getResponse ().getStatus () == Status.OK.getStatusCode ())
    {
      final String sRequestPath = aRequest.getPath (false);
      // Only handle requests that contain "/services/" but don't end with it
      if (sRequestPath.contains ("/services/") && !sRequestPath.endsWith ("/services/"))
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Will sign response to " + sRequestPath);

        aResponse.setContainerResponseWriter (new SigningContainerResponseWriter (aResponse.getContainerResponseWriter ()));
      }
    }

    return aResponse;
  }
}
