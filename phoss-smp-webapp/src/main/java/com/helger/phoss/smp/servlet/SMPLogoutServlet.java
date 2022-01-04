/*
 * Copyright (C) 2014-2022 Philip Helger (www.helger.com)
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
package com.helger.phoss.smp.servlet;

import java.util.EnumSet;

import com.helger.commons.http.EHttpMethod;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.photon.core.servlet.LogoutXServletHandler;
import com.helger.servlet.StaticServerInfo;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.AbstractXServlet;

/**
 * Handles the log-out of a user. Can be called with a user context and without.
 *
 * @author Philip Helger
 */
public final class SMPLogoutServlet extends AbstractXServlet
{
  public static final String SERVLET_DEFAULT_NAME = "logout";
  public static final String SERVLET_DEFAULT_PATH = "/" + SERVLET_DEFAULT_NAME;

  public SMPLogoutServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.GET, new LogoutXServletHandler ()
    {
      @Override
      protected ISimpleURL getRedirectURL (final IRequestWebScopeWithoutResponse aRequestScope)
      {
        final boolean bIsForceRoot = SMPServerConfiguration.isForceRoot ();
        String sRedirectURL;
        if (bIsForceRoot)
          sRedirectURL = StaticServerInfo.getInstance ().getFullServerPath ();
        else
          sRedirectURL = StaticServerInfo.getInstance ().getFullContextPath ();

        return new SimpleURL (sRedirectURL);
      }
    });
    handlerRegistry ().copyHandler (EHttpMethod.GET, EnumSet.of (EHttpMethod.POST));
  }
}
