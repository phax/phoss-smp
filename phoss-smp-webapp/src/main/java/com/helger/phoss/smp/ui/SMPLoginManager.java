/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.ui;

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.photon.app.html.IHTMLProvider;
import com.helger.photon.bootstrap5.uictrls.ext.BootstrapLoginManager;
import com.helger.photon.core.servlet.AbstractSecureApplicationServlet;
import com.helger.photon.security.login.LoginInfo;
import com.helger.security.authentication.credentials.ICredentialValidationResult;
import com.helger.servlet.StaticServerInfo;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * The login manager to be used. Manages login process incl. UI.
 *
 * @author Philip Helger
 */
public final class SMPLoginManager extends BootstrapLoginManager
{
  public static final Duration FAILED_LOGIN_WAITING_TIME = Duration.ofSeconds (1);

  public SMPLoginManager ()
  {
    super (CSMP.getApplicationTitle () + " Administration - Login");
    setRequiredRoleIDs (CSMP.REQUIRED_ROLE_IDS_CONFIG);
    setFailedLoginWaitingTime (FAILED_LOGIN_WAITING_TIME);
  }

  @Override
  protected IHTMLProvider createLoginScreen (final boolean bLoginError,
                                             @NonNull final ICredentialValidationResult aLoginResult)
  {
    return new SMPLoginHTMLProvider (bLoginError, aLoginResult, getPageTitle ());
  }

  /**
   * Widened to <code>public</code>, so that {@code SecureLoginFilter} can throttle the failed
   * validations of the second authentication factor per IP address the very same way - including
   * the evaluation of the <code>X-Forwarded-For</code> and <code>Forwarded</code> headers.
   *
   * @since 8.4.3
   */
  @Override
  @Nullable
  public String getRemoteAddressForThrottling (@NonNull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    return super.getRemoteAddressForThrottling (aRequestScope);
  }

  @Override
  protected void modifyLoginInfo (@NonNull final LoginInfo aLoginInfo,
                                  @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                  final boolean bLoggedInInThisRequest)
  {
    super.modifyLoginInfo (aLoginInfo, aRequestScope, bLoggedInInThisRequest);

    if (bLoggedInInThisRequest)
    {
      // Every new login requires a new second factor, even if the same user logs in again in the
      // same session
      SMPSecondFactorHelper.resetSecondFactorProvided ();
    }
  }

  @Override
  protected String getPostLoginRedirectURL (@NonNull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    if (!StaticServerInfo.isSet ())
      return super.getPostLoginRedirectURL (aRequestScope);

    // Ensure URL is absolute
    final boolean bIsForceRoot = SMPServerConfiguration.isForceRoot ();
    final String ret;
    if (bIsForceRoot)
      ret = StaticServerInfo.getInstance ().getFullServerPath ();
    else
      ret = StaticServerInfo.getInstance ().getFullContextPath ();

    return ret + AbstractSecureApplicationServlet.SERVLET_DEFAULT_PATH;
  }
}
