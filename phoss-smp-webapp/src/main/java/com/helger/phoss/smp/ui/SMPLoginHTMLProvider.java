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

import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.ui.pub.SMPRendererPublic;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapLoginHTMLProvider;
import com.helger.photon.bootstrap4.utils.BootstrapPageHeader;
import com.helger.photon.core.execcontext.ISimpleWebExecutionContext;
import com.helger.security.authentication.credentials.ICredentialValidationResult;
import com.helger.url.SimpleURL;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
    setShowLoginErrorDetails (SMPWebAppConfiguration.isSecurityLoginShowErrorDetails ());
  }

  @Override
  protected void onBeforeForm (@Nonnull final ISimpleWebExecutionContext aSWEC, @Nonnull final BootstrapForm aForm)
  {
    // Change the URL to relative (fixed in ph-oton 8.2.1)
    aForm.setAction (new SimpleURL (aSWEC.getRequestScope ().getURIDecoded ()));
  }

  @Override
  @Nonnull
  protected IHCNode createPageHeader (@Nonnull final ISimpleWebExecutionContext aSWEC,
                                      @Nullable final IHCNode aPageTitle)
  {
    final HCNodeList ret = new HCNodeList ();
    ret.addChild (new HCDiv ().addClass (CBootstrapCSS.MB_3).addChild (SMPRendererPublic.createLogoBig (aSWEC)));
    ret.addChild (new BootstrapPageHeader ().addChild ("Administration - Login"));
    return ret;
  }

  @Override
  @Nullable
  protected IHCNode createFormFooter (@Nonnull final ISimpleWebExecutionContext aSWEC)
  {
    final HCDiv aDiv = new HCDiv ().addClass (CBootstrapCSS.D_FLEX).addClass (CBootstrapCSS.MT_3);
    aDiv.addChild (new HCSmall ().addChild (CSMP.getApplicationTitleAndVersion ()));
    return aDiv;
  }
}
