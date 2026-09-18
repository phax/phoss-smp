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

import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.string.StringHelper;
import com.helger.diagnostics.error.IError;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.ui.secure.hc.HCButtonToolbarSticky;
import com.helger.photon.bootstrap5.alert.BootstrapBox;
import com.helger.photon.bootstrap5.alert.EBootstrapAlertType;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.grid.BootstrapGridSpec;
import com.helger.photon.bootstrap5.pages.AbstractBootstrapWebPageForm;
import com.helger.photon.core.appid.CApplicationID;
import com.helger.photon.core.appid.XServletFilterAppIDExplicit;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Base class for form based pages
 *
 * @author Philip Helger
 * @param <DATATYPE>
 *        The handled data type.
 */
public abstract class AbstractSMPWebPageForm <DATATYPE extends IHasID <String>> extends
                                             AbstractBootstrapWebPageForm <DATATYPE, WebPageExecutionContext>
{
  /** Grid spec for identifier schemes */
  protected static final BootstrapGridSpec GS_IDENTIFIER_SCHEME = BootstrapGridSpec.builder ()
                                                                                   .xs (6)
                                                                                   .lg (4)
                                                                                   .xl (3)
                                                                                   .build ();
  /** Grid spec for identifier values */
  protected static final BootstrapGridSpec GS_IDENTIFIER_VALUE = GS_IDENTIFIER_SCHEME.getInverse ();

  protected static final String HR_EXT_WARNING = AbstractSMPWebPage.HR_EXT_WARNING;

  protected AbstractSMPWebPageForm (@NonNull @Nonempty final String sID, @NonNull final String sName)
  {
    super (sID, sName);
  }

  /**
   * Create a {@link WebPageExecutionContext} for this page to be used from within on-demand AJAX
   * callbacks (e.g. the DataTables server side processing callback). This explicitly pins the
   * request to the "secure" application before resolving the {@link LayoutExecutionContext}.
   * 
   * This is necessary because the on-demand AJAX function is registered application-agnostically
   * (it is not covered by the {@code /secure/*} servlet path and therefore not handled by the
   * usual application-ID determination filter). Without this, the application ID would be
   * resolved from the session-wide "last used application ID", which can be "public" (e.g. if the
   * public part of the SMP was used in the same browser session before), causing action links
   * (edit, copy, delete, ...) build from the resulting execution context to point to the public
   * application instead of the secure one.
   *
   * @param aRequestScope
   *        The current request scope. May not be <code>null</code>.
   * @return A new {@link WebPageExecutionContext} for this page, guaranteed to be based on the
   *         secure application menu tree.
   */
  @NonNull
  protected final WebPageExecutionContext createSecureWPECForAjax (@NonNull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    // Same thing the XServletFilterAppIDExplicit servlet filter would do for a regular
    // "/secure/*" request - explicitly pin the application ID for this request.
    XServletFilterAppIDExplicit.setStatePerApp (aRequestScope, CApplicationID.APP_ID_SECURE);
    return new WebPageExecutionContext (LayoutExecutionContext.createForAjaxOrAction (aRequestScope), this);
  }

  @Override
  protected void onInputFormError (@NonNull final WebPageExecutionContext aWPEC,
                                   @NonNull final FormErrorList aFormErrors)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    // Show all global errors that don't have a specific error field
    for (final IError aError : aFormErrors)
      if (StringHelper.isEmpty (aError.getErrorFieldName ()))
      {
        final EBootstrapAlertType eType = aError.isError () ? EBootstrapAlertType.DANGER : EBootstrapAlertType.WARNING;
        aNodeList.addChild (new BootstrapBox (eType).addChild (aError.getAsString (aDisplayLocale)));
      }
  }

  @Override
  @NonNull
  protected BootstrapButtonToolbar createNewCreateToolbar (@NonNull final WebPageExecutionContext aWPEC)
  {
    return new HCButtonToolbarSticky (aWPEC);
  }

  @Override
  @NonNull
  protected BootstrapButtonToolbar createNewEditToolbar (@NonNull final WebPageExecutionContext aWPEC)
  {
    return new HCButtonToolbarSticky (aWPEC);
  }

  @Override
  @NonNull
  protected BootstrapButtonToolbar createNewViewToolbar (@NonNull final WebPageExecutionContext aWPEC)
  {
    return new HCButtonToolbarSticky (aWPEC);
  }
}
