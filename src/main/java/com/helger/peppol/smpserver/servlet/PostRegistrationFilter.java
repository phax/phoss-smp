/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.servlet;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.state.ESuccess;
import com.helger.peppol.smpserver.smlhook.HookException;
import com.helger.peppol.smpserver.smlhook.IRegistrationHook;
import com.helger.peppol.smpserver.smlhook.RegistrationServiceRegistrationHook;

/**
 * Filter which handles post-registration hooks. If a registration was started
 * in <code>AbstractRegistrationHook</code>, this filter will make sure the
 * registration is ended by calling
 * <code>AbstractRegistrationHook.postUpdate(ESuccess)</code>.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class PostRegistrationFilter implements Filter
{
  /**
   * This response wrapper simply captures the status of the response in an
   * easily accessible way.
   *
   * @author Ravnholdt
   */
  private static final class HttpServletResponseWrapperWithStatus extends HttpServletResponseWrapper
  {
    private int m_nStatus = HttpServletResponse.SC_OK;

    public HttpServletResponseWrapperWithStatus (final HttpServletResponse aHttpResponse)
    {
      super (aHttpResponse);
    }

    @Override
    public void sendError (final int nStatusCode) throws IOException
    {
      super.sendError (nStatusCode);
      m_nStatus = nStatusCode;
    }

    @Override
    public void sendError (final int nStatusCode, final String msg) throws IOException
    {
      super.sendError (nStatusCode, msg);
      m_nStatus = nStatusCode;
    }

    @Override
    public void setStatus (final int nStatusCode)
    {
      super.setStatus (nStatusCode);
      m_nStatus = nStatusCode;
    }

    @Override
    @Deprecated
    public void setStatus (final int nStatusCode, final String sStatusMessage)
    {
      super.setStatus (nStatusCode, sStatusMessage);
      m_nStatus = nStatusCode;
    }

    @Override
    public int getStatus ()
    {
      return m_nStatus;
    }
  }

  private static final Logger s_aLogger = LoggerFactory.getLogger (PostRegistrationFilter.class);

  public void init (final FilterConfig aFilterConfig)
  {}

  private static void _notifyRegistrationHook (@Nonnull final ESuccess eSuccess) throws ServletException
  {
    final IRegistrationHook aCallback = RegistrationServiceRegistrationHook.getAndRemoveHook ();
    if (aCallback != null)
    {
      try
      {
        aCallback.postUpdate (eSuccess);
      }
      catch (final HookException ex)
      {
        throw new ServletException (ex);
      }
    }
  }

  public void doFilter (final ServletRequest aRequest, final ServletResponse aResponse, final FilterChain aFilterChain) throws IOException,
                                                                                                                       ServletException
  {
    // Wrap the response
    final HttpServletResponseWrapperWithStatus aResponseWrapper = new HttpServletResponseWrapperWithStatus ((HttpServletResponse) aResponse);
    try
    {
      aFilterChain.doFilter (aRequest, aResponseWrapper);

      // Success or failure?
      if (aResponseWrapper.getStatus () >= 400)
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Operation failed, status: " + aResponseWrapper.getStatus ());
        _notifyRegistrationHook (ESuccess.FAILURE);
      }
      else
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Operation ok, status: " + aResponseWrapper.getStatus ());
        _notifyRegistrationHook (ESuccess.SUCCESS);
      }
    }
    catch (final IOException e)
    {
      s_aLogger.warn ("Got IOException " + e.getMessage ());
      _notifyRegistrationHook (ESuccess.FAILURE);
      throw e;
    }
    catch (final ServletException e)
    {
      s_aLogger.warn ("Got ServletException " + e.getMessage ());
      _notifyRegistrationHook (ESuccess.FAILURE);
      throw e;
    }
    catch (final RuntimeException e)
    {
      s_aLogger.warn ("Got RuntimeException " + e.getMessage ());
      _notifyRegistrationHook (ESuccess.FAILURE);
      throw e;
    }
  }

  public void destroy ()
  {
    // empty
  }
}
