/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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
package com.helger.phoss.smp.rest2;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.state.EHandled;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPInternalErrorException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPSMLException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.phoss.smp.exception.SMPUnknownUserException;
import com.helger.photon.api.AbstractAPIExceptionMapper;
import com.helger.photon.api.InvokableAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Special API exception mapper for the SMP REST API.
 *
 * @author Philip Helger
 */
public class Rest2ExceptionMapper extends AbstractAPIExceptionMapper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (Rest2ExceptionMapper.class);

  @Nonnull
  public EHandled applyExceptionOnResponse (final InvokableAPIDescriptor aInvokableDescriptor,
                                            final IRequestWebScopeWithoutResponse aRequestScope,
                                            final UnifiedResponse aUnifiedResponse,
                                            final Throwable aThrowable)
  {
    // From specific to general
    if (aThrowable instanceof SMPUnauthorizedException)
    {
      if (SMPServerConfiguration.isRESTLogExceptions ())
        LOGGER.error ("Unauthorized", aThrowable);
      setSimpleTextResponse (aUnifiedResponse,
                             HttpServletResponse.SC_FORBIDDEN,
                             getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPUnknownUserException)
    {
      if (SMPServerConfiguration.isRESTLogExceptions ())
        LOGGER.error ("Unknown user", aThrowable);
      setSimpleTextResponse (aUnifiedResponse,
                             HttpServletResponse.SC_FORBIDDEN,
                             getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPSMLException)
    {
      if (SMPServerConfiguration.isRESTLogExceptions ())
        LOGGER.error ("SMP SML error", aThrowable);
      setSimpleTextResponse (aUnifiedResponse,
                             HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                             GlobalDebug.isDebugMode () ? getResponseEntityWithStackTrace (aThrowable)
                                                        : getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPNotFoundException)
    {
      if (SMPServerConfiguration.isRESTLogExceptions ())
        LOGGER.error ("Not found", aThrowable);
      setSimpleTextResponse (aUnifiedResponse,
                             HttpServletResponse.SC_NOT_FOUND,
                             getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPInternalErrorException)
    {
      if (SMPServerConfiguration.isRESTLogExceptions ())
        LOGGER.error ("Internal error", aThrowable);
      setSimpleTextResponse (aUnifiedResponse,
                             HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                             GlobalDebug.isDebugMode () ? getResponseEntityWithStackTrace (aThrowable)
                                                        : getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPBadRequestException)
    {
      if (SMPServerConfiguration.isRESTLogExceptions ())
        LOGGER.error ("Bad request", aThrowable);
      setSimpleTextResponse (aUnifiedResponse,
                             HttpServletResponse.SC_BAD_REQUEST,
                             getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPServerException)
    {
      // Generic fallback only
      if (SMPServerConfiguration.isRESTLogExceptions ())
        LOGGER.error ("Generic SMP server", aThrowable);
      setSimpleTextResponse (aUnifiedResponse,
                             HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                             getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof RuntimeException)
    {
      if (SMPServerConfiguration.isRESTLogExceptions ())
        LOGGER.error ("Runtime exception - " + aThrowable.getClass ().getName (), aThrowable);
      setSimpleTextResponse (aUnifiedResponse,
                             HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                             GlobalDebug.isDebugMode () ? getResponseEntityWithStackTrace (aThrowable)
                                                        : getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }

    // We don't know that exception
    return EHandled.UNHANDLED;
  }
}
