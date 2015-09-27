/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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

import java.util.Collection;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsImmutableObject;
import com.helger.photon.basic.security.login.ELoginResult;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapLoginHTMLProvider;
import com.helger.photon.core.app.html.IHTMLProvider;
import com.helger.photon.core.login.LoginManager;

public final class AppLoginManager extends LoginManager
{
  @Override
  protected IHTMLProvider createLoginScreen (final boolean bLoginError, @Nonnull final ELoginResult eLoginResult)
  {
    return new BootstrapLoginHTMLProvider (bLoginError,
                                           eLoginResult,
                                           CApp.getApplicationTitle () + " Administration - Login");
  }

  @Override
  @Nonnull
  @ReturnsImmutableObject
  protected Collection <String> getAllRequiredRoleIDs ()
  {
    return CApp.REQUIRED_ROLE_IDS_CONFIG;
  }
}
