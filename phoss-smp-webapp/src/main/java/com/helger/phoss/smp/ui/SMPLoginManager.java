/*
 * Copyright (C) 2014-2021 Philip Helger and contributors
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

import javax.annotation.Nonnull;

import com.helger.phoss.smp.app.CSMP;
import com.helger.photon.app.html.IHTMLProvider;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapLoginManager;
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
    super (CSMP.getApplicationTitle () + " Administration - Login");
    setRequiredRoleIDs (CSMP.REQUIRED_ROLE_IDS_CONFIG);
  }

  @Override
  protected IHTMLProvider createLoginScreen (final boolean bLoginError, @Nonnull final ICredentialValidationResult aLoginResult)
  {
    return new SMPLoginHTMLProvider (bLoginError, aLoginResult, getPageTitle ());
  }
}
