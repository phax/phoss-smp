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
package com.helger.phoss.smp.ui.ajax;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.debug.GlobalDebug;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.render.HCRenderer;
import com.helger.json.JsonObject;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.bootstrap4.traits.IHCBootstrap4Trait;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.core.login.CLogin;
import com.helger.photon.security.login.ELoginResult;
import com.helger.photon.security.login.LoggedInUserManager;

import jakarta.annotation.Nonnull;

/**
 * Ajax executor to login a user from public application.
 *
 * @author Philip Helger
 */
public final class AjaxExecutorPublicLogin extends AbstractSMPAjaxExecutor implements IHCBootstrap4Trait
{
  public static final String JSON_LOGGEDIN = "loggedin";
  public static final String JSON_HTML = "html";

  private static final Logger LOGGER = LoggerFactory.getLogger (AjaxExecutorPublicLogin.class);

  @Override
  protected void mainHandleRequest (@Nonnull final LayoutExecutionContext aLEC,
                                    @Nonnull final PhotonUnifiedResponse aAjaxResponse) throws Exception
  {
    final String sLoginName = aLEC.params ().getAsString (CLogin.REQUEST_ATTR_USERID);
    final String sPassword = aLEC.params ().getAsString (CLogin.REQUEST_ATTR_PASSWORD);

    // Main login
    final ELoginResult eLoginResult = LoggedInUserManager.getInstance ()
                                                         .loginUser (sLoginName,
                                                                     sPassword,
                                                                     CSMP.REQUIRED_ROLE_IDS_WRITABLERESTAPI);
    if (eLoginResult.isSuccess ())
    {
      aAjaxResponse.json (new JsonObject ().add (JSON_LOGGEDIN, true));
      return;
    }
    // Get the rendered content of the menu area
    if (GlobalDebug.isDebugMode ())
      LOGGER.warn ("Login of '" + sLoginName + "' failed because " + eLoginResult);

    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    String sErrorMessage = EPhotonCoreText.LOGIN_ERROR_MSG.getDisplayText (aDisplayLocale);
    if (SMPWebAppConfiguration.isSecurityLoginShowErrorDetails ())
    {
      // Append details
      sErrorMessage += " " + eLoginResult.getDisplayText (aDisplayLocale);
    }
    final IHCNode aRoot = error (sErrorMessage);

    // Set as result property
    aAjaxResponse.json (new JsonObject ().add (JSON_LOGGEDIN, false)
                                         .add (JSON_HTML, HCRenderer.getAsHTMLStringWithoutNamespaces (aRoot)));
  }
}
