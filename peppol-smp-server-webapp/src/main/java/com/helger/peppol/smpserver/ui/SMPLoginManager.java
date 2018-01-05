/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.ui;

import javax.annotation.Nonnull;

import com.helger.peppol.smpserver.app.CApp;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapLoginManager;
import com.helger.photon.core.app.html.IHTMLProvider;
import com.helger.security.authentication.credentials.ICredentialValidationResult;

/**
 * The login manager to be used. Manages login process incl. UI.
 *
 * @author Philip Helger
 */
public final class SMPLoginManager extends BootstrapLoginManager
{
  public SMPLoginManager ()
  {
    super (CApp.getApplicationTitle () + " Administration - Login");
    setRequiredRoleIDs (CApp.REQUIRED_ROLE_IDS_CONFIG);
  }

  @Override
  protected IHTMLProvider createLoginScreen (final boolean bLoginError,
                                             @Nonnull final ICredentialValidationResult aLoginResult)
  {
    return new SMPLoginHTMLProvider (bLoginError, aLoginResult, getPageTitle ());
  }
}
