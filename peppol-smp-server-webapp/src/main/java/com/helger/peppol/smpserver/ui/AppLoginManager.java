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
package com.helger.peppol.smpserver.ui;

import java.util.Collection;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsImmutableObject;
import com.helger.css.ECSSUnit;
import com.helger.css.property.CCSSProperties;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.peppol.smpserver.app.CApp;
import com.helger.photon.bootstrap3.base.BootstrapContainer;
import com.helger.photon.bootstrap3.grid.BootstrapRow;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapLoginHTMLProvider;
import com.helger.photon.core.app.context.ISimpleWebExecutionContext;
import com.helger.photon.core.app.html.IHTMLProvider;
import com.helger.photon.core.login.LoginManager;
import com.helger.photon.security.login.ELoginResult;

public final class AppLoginManager extends LoginManager
{
  @Override
  protected IHTMLProvider createLoginScreen (final boolean bLoginError, @Nonnull final ELoginResult eLoginResult)
  {
    return new BootstrapLoginHTMLProvider (bLoginError, eLoginResult, CApp.getApplicationTitle () + " Administration - Login")
    {
      @Override
      protected void onAfterContainer (@Nonnull final ISimpleWebExecutionContext aSWEC,
                                       @Nonnull final BootstrapContainer aContainer,
                                       @Nonnull final BootstrapRow aRow,
                                       @Nonnull final HCDiv aContentCol)
      {
        aContentCol.addChild (new HCDiv ().addStyle (CCSSProperties.MARGIN_TOP.newValue (ECSSUnit.em (1)))
                                          .addChild (new HCSmall ().addChild (CApp.getApplicationTitleAndVersion ())));
      }
    };
  }

  @Override
  @Nonnull
  @ReturnsImmutableObject
  protected Collection <String> getAllRequiredRoleIDs ()
  {
    return CApp.REQUIRED_ROLE_IDS_CONFIG;
  }
}
