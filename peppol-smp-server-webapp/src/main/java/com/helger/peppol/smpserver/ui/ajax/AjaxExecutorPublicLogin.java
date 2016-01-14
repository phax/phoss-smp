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
package com.helger.peppol.smpserver.ui.ajax;

import java.util.Locale;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.debug.GlobalDebug;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.render.HCRenderer;
import com.helger.json.JsonObject;
import com.helger.peppol.smpserver.app.CApp;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.ajax.executor.AbstractAjaxExecutor;
import com.helger.photon.core.ajax.response.AjaxJsonResponse;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.login.CLogin;
import com.helger.photon.security.login.ELoginResult;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Ajax executor to login a user from view application.
 *
 * @author Philip Helger
 */
public final class AjaxExecutorPublicLogin extends AbstractAjaxExecutor
{
  public static final String JSON_LOGGEDIN = "loggedin";
  public static final String JSON_HTML = "html";

  private static final Logger s_aLogger = LoggerFactory.getLogger (AjaxExecutorPublicLogin.class);

  @Override
  @Nonnull
  protected AjaxJsonResponse mainHandleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope) throws Exception
  {
    final LayoutExecutionContext aLEC = LayoutExecutionContext.createForAjaxOrAction (aRequestScope);
    final String sLoginName = aRequestScope.getAttributeAsString (CLogin.REQUEST_ATTR_USERID);
    final String sPassword = aRequestScope.getAttributeAsString (CLogin.REQUEST_ATTR_PASSWORD);

    // Main login
    final ELoginResult eLoginResult = LoggedInUserManager.getInstance ().loginUser (sLoginName,
                                                                                    sPassword,
                                                                                    CApp.REQUIRED_ROLE_IDS_VIEW);
    if (eLoginResult.isSuccess ())
      return AjaxJsonResponse.createSuccess (new JsonObject ().add (JSON_LOGGEDIN, true));

    // Get the rendered content of the menu area
    if (GlobalDebug.isDebugMode ())
      s_aLogger.warn ("Login of '" + sLoginName + "' failed because " + eLoginResult);

    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final IHCNode aRoot = new BootstrapErrorBox ().addChild (EPhotonCoreText.LOGIN_ERROR_MSG.getDisplayText (aDisplayLocale) +
                                                             " " +
                                                             eLoginResult.getDisplayText (aDisplayLocale));

    // Set as result property
    return AjaxJsonResponse.createSuccess (new JsonObject ().add (JSON_LOGGEDIN, false)
                                                            .add (JSON_HTML,
                                                                  HCRenderer.getAsHTMLStringWithoutNamespaces (aRoot)));
  }
}
