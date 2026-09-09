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
package com.helger.phoss.smp.servlet;

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.EContinue;
import com.helger.base.concurrent.ThreadHelper;
import com.helger.http.CHttp;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.ui.SMPLoginManager;
import com.helger.phoss.smp.ui.SMPSecondFactorHTMLProvider;
import com.helger.phoss.smp.ui.SMPSecondFactorHelper;
import com.helger.photon.app.csrf.CSRFSessionManager;
import com.helger.photon.app.html.PhotonHTMLHelper;
import com.helger.photon.core.servlet.AbstractUnifiedResponseFilter;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.servlet.ServletException;

/**
 * A special servlet filter that checks that a user can only access the config
 * application after authenticating.
 *
 * @author Philip Helger
 */
public final class SecureLoginFilter extends AbstractUnifiedResponseFilter
{
  /** The duration to wait after an invalid second factor was provided. */
  private static final Duration FAILED_TOTP_WAIT_TIME = Duration.ofSeconds (1);

  private static final Logger LOGGER = LoggerFactory.getLogger (SecureLoginFilter.class);

  private SMPLoginManager m_aLogin;

  @Override
  public void init () throws ServletException
  {
    super.init ();
    // Make the application login configurable if you like
    m_aLogin = new SMPLoginManager ();
  }

  @Override
  @NonNull
  protected EContinue handleRequest (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                     @NonNull final UnifiedResponse aUnifiedResponse) throws ServletException
  {
    if (m_aLogin.checkUserAndShowLogin (aRequestScope, aUnifiedResponse).isBreak ())
    {
      // Show login screen
      return EContinue.BREAK;
    }

    // Check if the currently logged in user has the required roles
    final String sCurrentUserID = LoggedInUserManager.getInstance ().getCurrentUserID ();
    if (!SecurityHelper.hasUserAllRoles (sCurrentUserID, CSMP.REQUIRED_ROLE_IDS_CONFIG))
    {
      aUnifiedResponse.setStatus (CHttp.HTTP_FORBIDDEN);
      return EContinue.BREAK;
    }

    // The credentials are valid - check the second factor, but only if the user enabled it
    if (SMPSecondFactorHelper.isSecondFactorRequired (sCurrentUserID) &&
        !SMPSecondFactorHelper.isSecondFactorProvided ())
    {
      return _checkSecondFactor (sCurrentUserID, aRequestScope, aUnifiedResponse);
    }

    return EContinue.CONTINUE;
  }

  @NonNull
  private static EContinue _checkSecondFactor (@NonNull final String sCurrentUserID,
                                               @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                               @NonNull final UnifiedResponse aUnifiedResponse)
  {
    boolean bError = false;
    String sErrorMsg = null;

    if (SMPSecondFactorHelper.isValidationInProgress (aRequestScope.params ()
                                                                   .getAsString (SMPSecondFactorHelper.REQUEST_PARAM_ACTION)))
    {
      // The second factor form was submitted
      final String sNonce = aRequestScope.params ().getAsString (CPageParam.FIELD_NONCE);
      if (!CSRFSessionManager.getInstance ().isExpectedNonce (sNonce))
      {
        LOGGER.warn ("The second factor form of user ID '" + sCurrentUserID + "' contained an invalid CSRF nonce");
        bError = true;
        sErrorMsg = "Your session has expired. Please try again.";
      }
      else
      {
        final String sCode = aRequestScope.params ().getAsStringTrimmed (SMPSecondFactorHelper.REQUEST_ATTR_TOTP_CODE);
        if (SMPSecondFactorHelper.isValidSecondFactor (sCurrentUserID, sCode))
        {
          SMPSecondFactorHelper.markSecondFactorProvided ();
          CSRFSessionManager.getInstance ().generateNewNonce ();

          LOGGER.info ("Successfully verified the second authentication factor of user ID '" + sCurrentUserID + "'");

          // Avoid a double submit by redirecting to the desired destination URL
          aUnifiedResponse.setRedirect (aRequestScope.getURIDecoded ());
          return EContinue.BREAK;
        }

        bError = true;
        sErrorMsg = "The provided authenticator code is invalid. Please try again.";

        // Slow down brute force attempts on the second factor
        ThreadHelper.sleep (FAILED_TOTP_WAIT_TIME);
      }
    }

    // Show the second factor screen
    PhotonHTMLHelper.createHTMLResponse (aRequestScope,
                                         aUnifiedResponse,
                                         new SMPSecondFactorHTMLProvider (bError, sErrorMsg));
    return EContinue.BREAK;
  }
}
