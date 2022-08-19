/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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
package com.helger.phoss.smp.rest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.http.CHttp;
import com.helger.commons.state.EHandled;
import com.helger.commons.string.StringHelper;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPInternalErrorException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
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
public class SMPRestExceptionMapper extends AbstractAPIExceptionMapper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPRestExceptionMapper.class);

  private static void _logRestException (@Nonnull final String sMsg, @Nonnull final Throwable t)
  {
    _logRestException (sMsg, t, false);
  }

  private static void _logRestException (@Nonnull final String sMsg, @Nonnull final Throwable t, final boolean bForceNoStackTrace)
  {
    final boolean bConfiguredToLog = SMPServerConfiguration.isRESTLogExceptions ();
    if (bConfiguredToLog)
    {
      if (bForceNoStackTrace)
        LOGGER.error (SMPRestFilter.LOG_PREFIX + sMsg + " - " + getResponseEntityWithoutStackTrace (t));
      else
        LOGGER.error (SMPRestFilter.LOG_PREFIX + sMsg, t);
    }
    else
      LOGGER.error (SMPRestFilter.LOG_PREFIX +
                    sMsg +
                    " - " +
                    getResponseEntityWithoutStackTrace (t) +
                    " (turn on REST exception logging to see all details)");
  }

  private static void _setSimpleTextResponse (@Nonnull final UnifiedResponse aUnifiedResponse,
                                              final int nStatusCode,
                                              @Nullable final String sContent)
  {
    if (SMPServerConfiguration.isRESTPayloadOnError ())
    {
      // With payload
      setSimpleTextResponse (aUnifiedResponse, nStatusCode, sContent);
      if (StringHelper.hasText (sContent))
        aUnifiedResponse.disableCaching ();
    }
    else
    {
      // No payload
      aUnifiedResponse.setStatus (nStatusCode);
    }
  }

  @Nonnull
  public EHandled applyExceptionOnResponse (@Nonnull final InvokableAPIDescriptor aInvokableDescriptor,
                                            @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                            @Nonnull final UnifiedResponse aUnifiedResponse,
                                            @Nonnull final Throwable aThrowable)
  {
    // From specific to general
    if (aThrowable instanceof SMPUnauthorizedException)
    {
      _logRestException ("Unauthorized", aThrowable);
      _setSimpleTextResponse (aUnifiedResponse, CHttp.HTTP_FORBIDDEN, getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPUnknownUserException)
    {
      _logRestException ("Unknown user", aThrowable);
      _setSimpleTextResponse (aUnifiedResponse, CHttp.HTTP_FORBIDDEN, getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPSMLException)
    {
      _logRestException ("SMP SML error", aThrowable);
      _setSimpleTextResponse (aUnifiedResponse,
                              CHttp.HTTP_INTERNAL_SERVER_ERROR,
                              GlobalDebug.isDebugMode () ? getResponseEntityWithStackTrace (aThrowable)
                                                         : getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPNotFoundException)
    {
      _logRestException ("Not found", aThrowable);
      _setSimpleTextResponse (aUnifiedResponse, CHttp.HTTP_NOT_FOUND, getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPInternalErrorException)
    {
      _logRestException ("Internal error", aThrowable);
      _setSimpleTextResponse (aUnifiedResponse,
                              CHttp.HTTP_INTERNAL_SERVER_ERROR,
                              GlobalDebug.isDebugMode () ? getResponseEntityWithStackTrace (aThrowable)
                                                         : getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPBadRequestException)
    {
      // Forcing no stack trace, because the context should be self-explanatory
      _logRestException ("Bad request", aThrowable, true);
      _setSimpleTextResponse (aUnifiedResponse, CHttp.HTTP_BAD_REQUEST, getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPPreconditionFailedException)
    {
      // Forcing no stack trace, because the context should be self-explanatory
      _logRestException ("Precondition failed", aThrowable, true);
      _setSimpleTextResponse (aUnifiedResponse, CHttp.HTTP_PRECONDITION_FAILED, getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof SMPServerException)
    {
      // Generic fallback only
      _logRestException ("Generic SMP error", aThrowable);
      _setSimpleTextResponse (aUnifiedResponse, CHttp.HTTP_INTERNAL_SERVER_ERROR, getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof RuntimeException)
    {
      _logRestException ("Runtime exception - " + aThrowable.getClass ().getName (), aThrowable);
      _setSimpleTextResponse (aUnifiedResponse,
                              CHttp.HTTP_INTERNAL_SERVER_ERROR,
                              GlobalDebug.isDebugMode () ? getResponseEntityWithStackTrace (aThrowable)
                                                         : getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }

    // We don't know that exception
    return EHandled.UNHANDLED;
  }
}
