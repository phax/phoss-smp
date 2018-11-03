/**
 * Copyright (C) 2014-2018 Philip Helger and contributors
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
package com.helger.peppol.smpserver.exceptionmapper;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.mime.CMimeType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.exception.SMPBadRequestException;

/**
 * @author Philip Helger
 * @since 5.1.0
 */
@Provider
public class SMPBadRequestExceptionMapper extends AbstractExceptionMapper <SMPBadRequestException>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPBadRequestExceptionMapper.class);

  @Nonnull
  public Response toResponse (@Nonnull final SMPBadRequestException ex)
  {
    if (SMPServerConfiguration.isRESTLogExceptions ())
      LOGGER.error ("Bad Request", ex);

    return Response.status (Status.BAD_REQUEST)
                   .entity (getResponseEntityWithoutStackTrace (ex))
                   .type (CMimeType.TEXT_PLAIN.getAsString ())
                   .build ();
  }
}
