/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.mime.CMimeType;

/**
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Provider
public final class RuntimeExceptionMapper implements ExceptionMapper <RuntimeException>
{
  public Response toResponse (final RuntimeException ex)
  {
    final String sText = StackTraceHelper.getStackAsString (ex);

    return Response.status (Status.INTERNAL_SERVER_ERROR)
                   .entity (sText)
                   .type (CMimeType.TEXT_PLAIN.getAsString ())
                   .build ();
  }
}
