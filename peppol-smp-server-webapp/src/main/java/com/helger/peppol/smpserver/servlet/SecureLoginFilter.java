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
package com.helger.peppol.smpserver.servlet;

import javax.annotation.Nonnull;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.state.EContinue;
import com.helger.peppol.smpserver.app.CApp;
import com.helger.peppol.smpserver.ui.AppLoginManager;
import com.helger.photon.core.app.CApplication;
import com.helger.photon.core.servlet.AbstractUnifiedResponseFilter;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.servlet.response.UnifiedResponse;

/**
 * A special servlet filter that checks that a user can only access the config
 * application after authenticating.
 *
 * @author Philip Helger
 */
public final class SecureLoginFilter extends AbstractUnifiedResponseFilter
{
  private AppLoginManager m_aLogin;

  @Override
  @Nonnull
  @Nonempty
  protected String getApplicationID (@Nonnull final FilterConfig aFilterConfig)
  {
    return CApplication.APP_ID_SECURE;
  }

  @Override
  protected void onInit (@Nonnull final FilterConfig aFilterConfig) throws ServletException
  {
    // Make the application login configurable if you like
    m_aLogin = new AppLoginManager ();
  }

  @Override
  @Nonnull
  protected EContinue handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                     @Nonnull final UnifiedResponse aUnifiedResponse) throws ServletException
  {
    if (m_aLogin.checkUserAndShowLogin (aRequestScope, aUnifiedResponse).isBreak ())
    {
      // Show login screen
      return EContinue.BREAK;
    }

    // Check if the currently logged in user has the required roles
    final String sCurrentUserID = LoggedInUserManager.getInstance ().getCurrentUserID ();
    if (!SecurityHelper.hasUserAllRoles (sCurrentUserID, CApp.REQUIRED_ROLE_IDS_CONFIG))
    {
      aUnifiedResponse.setStatus (HttpServletResponse.SC_FORBIDDEN);
      return EContinue.BREAK;
    }

    return EContinue.CONTINUE;
  }
}
