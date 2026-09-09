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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.css.property.CCSSProperties;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.root.HCHtml;
import com.helger.html.hc.html.sections.HCBody;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.ui.pub.SMPRendererPublic;
import com.helger.photon.bootstrap5.CBootstrapCSS;
import com.helger.photon.bootstrap5.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap5.form.BootstrapForm;
import com.helger.photon.bootstrap5.form.BootstrapFormGroup;
import com.helger.photon.bootstrap5.layout.BootstrapContainer;
import com.helger.photon.bootstrap5.utils.BootstrapPageHeader;
import com.helger.photon.core.execcontext.ISimpleWebExecutionContext;
import com.helger.photon.core.html.AbstractSWECHTMLProvider;
import com.helger.photon.core.servlet.LogoutServlet;
import com.helger.photon.uicore.page.WebPageCSRFHandler;
import com.helger.url.SimpleURL;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * The HTML provider for the intermediate two-factor authentication step of the
 * <code>/secure</code> application. It is only shown, if the user that just provided valid
 * credentials has TOTP enabled.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
public class SMPSecondFactorHTMLProvider extends AbstractSWECHTMLProvider
{
  private static final String PAGE_TITLE = CSMP.getApplicationTitle () +
                                           " Administration - Two-factor authentication";

  private final boolean m_bError;
  private final String m_sErrorMsg;

  /**
   * Constructor
   *
   * @param bError
   *        <code>true</code> if a previously provided code was invalid.
   * @param sErrorMsg
   *        The error message to be shown. May be <code>null</code>.
   */
  public SMPSecondFactorHTMLProvider (final boolean bError, @Nullable final String sErrorMsg)
  {
    m_bError = bError;
    m_sErrorMsg = sErrorMsg;
  }

  @Override
  protected void fillBody (@NonNull final ISimpleWebExecutionContext aSWEC, @NonNull final HCHtml aHtml)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aSWEC.getRequestScope ();

    // Use the server-relative version without the hostname
    final BootstrapForm aForm = new BootstrapForm (aSWEC).setAction (new SimpleURL (aRequestScope.getURIDecoded ()));

    // The hidden field that triggers the validation
    aForm.addChild (new HCHiddenField (SMPSecondFactorHelper.REQUEST_PARAM_ACTION,
                                       SMPSecondFactorHelper.REQUEST_ACTION_VALIDATE_TOTP));
    aForm.addChild (WebPageCSRFHandler.INSTANCE.createCSRFNonceField ());

    if (m_bError)
      aForm.addChild (new BootstrapErrorBox ().addChild (m_sErrorMsg));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Authenticator code")
                                                 .setCtrl (new HCEdit (SMPSecondFactorHelper.REQUEST_ATTR_TOTP_CODE).setPlaceholder ("123456")
                                                                                                                    .setAutoFocus (true))
                                                 .setHelpText ("Enter the current one-time password of your authenticator app"));

    aForm.addChild (new BootstrapSubmitButton ().addChild ("Verify"));

    final HCSpan aSpan = new HCSpan ();
    aSpan.addStyle (CCSSProperties.MIN_HEIGHT.newValue ("100%"));
    aSpan.addStyle (CCSSProperties.MIN_HEIGHT.newValue ("100vh"));
    aSpan.addStyle (CCSSProperties.DISPLAY.newValue ("flex"));
    aSpan.addStyle (CCSSProperties.ALIGN_ITEMS.newValue ("center"));

    final BootstrapContainer aContainer = new BootstrapContainer ();
    aContainer.addChild (new HCDiv ().addClass (CBootstrapCSS.MB_3)
                                     .addChild (SMPRendererPublic.createLogoBig (aSWEC)));
    aContainer.addChild (new BootstrapPageHeader ().addChild ("Two-factor authentication"));
    aContainer.addChild (aForm);
    aContainer.addChild (new HCDiv ().addClass (CBootstrapCSS.MT_3)
                                     .addChild (new BootstrapButton ().addChild ("Cancel and logout")
                                                                      .setOnClick (new SimpleURL (aRequestScope.getContextPath () +
                                                                                                  LogoutServlet.SERVLET_DEFAULT_PATH))));
    aContainer.addChild (new HCDiv ().addClass (CBootstrapCSS.D_FLEX)
                                     .addClass (CBootstrapCSS.MT_3)
                                     .addChild (new HCSmall ().addChild (CSMP.getApplicationTitleAndVersion ())));
    aSpan.addChild (aContainer);

    final HCBody aBody = aHtml.body ();
    aBody.addChild (aSpan);
  }

  @Override
  protected void fillHead (@NonNull final ISimpleWebExecutionContext aSWEC, @NonNull final HCHtml aHtml)
  {
    super.fillHead (aSWEC, aHtml);
    aHtml.head ().setPageTitle (PAGE_TITLE);
  }
}
