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
import javax.annotation.Nullable;

import com.helger.css.ECSSUnit;
import com.helger.css.property.CCSSProperties;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.sections.HCH2;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.peppol.smpserver.app.CApp;
import com.helger.peppol.smpserver.ui.pub.SMPRendererPublic;
import com.helger.photon.bootstrap3.base.BootstrapContainer;
import com.helger.photon.bootstrap3.grid.BootstrapRow;
import com.helger.photon.bootstrap3.pageheader.BootstrapPageHeader;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapLoginHTMLProvider;
import com.helger.photon.core.app.context.ISimpleWebExecutionContext;
import com.helger.security.authentication.credentials.ICredentialValidationResult;

/**
 * The login screen HTML provider.
 *
 * @author Philip Helger
 */
public final class SMPLoginHTMLProvider extends BootstrapLoginHTMLProvider
{
  public SMPLoginHTMLProvider (final boolean bLoginError,
                               @Nonnull final ICredentialValidationResult aLoginResult,
                               @Nullable final IHCNode aPageTitle)
  {
    super (bLoginError, aLoginResult, aPageTitle);
  }

  @Override
  @Nonnull
  protected IHCNode createPageHeader (@Nullable final IHCNode aPageTitle)
  {
    return new BootstrapPageHeader ().addChild (new HCH2 ().addChild ("Administration - Login"));
  }

  @Override
  protected void onAfterContainer (@Nonnull final ISimpleWebExecutionContext aSWEC,
                                   @Nonnull final BootstrapContainer aContainer,
                                   @Nonnull final BootstrapRow aRow,
                                   @Nonnull final HCDiv aContentCol)
  {
    // Add the logo on top
    aContentCol.addChildAt (0,
                            new HCDiv ().addStyle (CCSSProperties.MARGIN_TOP.newValue (ECSSUnit.em (1)))
                                        .addChild (SMPRendererPublic.createLogoBig (aSWEC)));

    // Add the version number in the login screen
    aContentCol.addChild (new HCDiv ().addStyle (CCSSProperties.MARGIN_TOP.newValue (ECSSUnit.em (1)))
                                      .addChild (new HCSmall ().addChild (CApp.getApplicationTitleAndVersion ())));
  }
}
