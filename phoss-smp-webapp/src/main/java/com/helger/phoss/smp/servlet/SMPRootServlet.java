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
package com.helger.phoss.smp.servlet;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.http.EHttpMethod;
import com.helger.commons.string.StringHelper;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.photon.core.servlet.AbstractPublicApplicationServlet;
import com.helger.servlet.StaticServerInfo;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.AbstractXServlet;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

/**
 * The servlet handling the "/" URL to redirect to "/public"
 *
 * @author Philip Helger
 */
public class SMPRootServlet extends AbstractXServlet
{
  private static final class RootHandler implements IXServletSimpleHandler
  {
    private static final Logger LOGGER = LoggerFactory.getLogger (SMPRootServlet.RootHandler.class);

    public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                               @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
    {
      String sRedirectURL;
      if (StaticServerInfo.isSet ())
      {
        final boolean bIsForceRoot = SMPServerConfiguration.isForceRoot ();
        if (bIsForceRoot)
          sRedirectURL = StaticServerInfo.getInstance ().getFullServerPath ();
        else
          sRedirectURL = StaticServerInfo.getInstance ().getFullContextPath ();
        sRedirectURL += AbstractPublicApplicationServlet.SERVLET_DEFAULT_PATH;
      }
      else
      {
        sRedirectURL = aRequestScope.getContextPath () + AbstractPublicApplicationServlet.SERVLET_DEFAULT_PATH;
      }

      final String sQueryString = aRequestScope.getQueryString ();
      if (StringHelper.hasText (sQueryString))
        sRedirectURL += "?" + sQueryString;

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Sending redirect to '" + sRedirectURL + "'");

      aUnifiedResponse.setRedirect (sRedirectURL);
    }
  }

  public SMPRootServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.GET, new RootHandler ());
    handlerRegistry ().copyHandlerToAll (EHttpMethod.GET);
    if (SMPWebAppConfiguration.isHttpOptionsDisabled ())
      handlerRegistry ().unregisterHandler (EHttpMethod.OPTIONS);
  }
}
