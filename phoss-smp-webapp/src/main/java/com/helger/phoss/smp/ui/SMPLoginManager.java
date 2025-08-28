/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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

import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.photon.app.html.IHTMLProvider;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapLoginManager;
import com.helger.photon.core.servlet.AbstractSecureApplicationServlet;
import com.helger.security.authentication.credentials.ICredentialValidationResult;
import com.helger.servlet.StaticServerInfo;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nonnull;

/**
 * The login manager to be used. Manages login process incl. UI.
 *
 * @author Philip Helger
 */
public final class SMPLoginManager extends BootstrapLoginManager
{
  public SMPLoginManager ()
  {
    super (CSMP.getApplicationTitle () + " Administration - Login");
    setRequiredRoleIDs (CSMP.REQUIRED_ROLE_IDS_CONFIG);
    setFailedLoginWaitingTime (Duration.ofSeconds (1));
  }

  @Override
  protected IHTMLProvider createLoginScreen (final boolean bLoginError,
                                             @Nonnull final ICredentialValidationResult aLoginResult)
  {
    return new SMPLoginHTMLProvider (bLoginError, aLoginResult, getPageTitle ());
  }

  @Override
  protected String getPostLoginRedirectURL (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope)
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
