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

import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.concurrent.ThreadHelper;
import com.helger.base.state.EContinue;
import com.helger.http.CHttp;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.ui.SMPLoginManager;
import com.helger.phoss.smp.ui.SMPSecondFactorHTMLProvider;
import com.helger.phoss.smp.ui.SMPSecondFactorHelper;
import com.helger.photon.app.csrf.CSRFSessionManager;
import com.helger.photon.app.html.PhotonHTMLHelper;
import com.helger.photon.core.servlet.AbstractUnifiedResponseFilter;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.login.LoginThrottlePerIP;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.servlet.ServletException;

/**
 * A special servlet filter that checks that a user can only access the config application after
 * authenticating.
 *
 * @author Philip Helger
 */
public final class SecureLoginFilter extends AbstractUnifiedResponseFilter
{
  /** The base duration to wait after the first invalid second factor was provided. */
  private static final Duration FAILED_TOTP_BASE_WAIT_TIME = SMPLoginManager.FAILED_LOGIN_WAITING_TIME;
  /** The maximum duration to wait after an invalid second factor was provided. */
  private static final Duration FAILED_TOTP_MAX_WAIT_TIME = Duration.ofSeconds (30);
  /** The maximum exponent to be used for the exponential backoff. */
  private static final int FAILED_TOTP_MAX_EXPONENT = 16;
  /**
   * The suffix appended to the user ID to form the throttle key of the second factor.
   * <p>
   * The counter is kept per user and not per IP address, because the second factor is only ever
   * reached after the password was accepted - so an attacker cannot drive up the counter of a user
   * whose password they do not have, and rotating the source IP address does not help them either.
   * <p>
   * A dedicated suffix is used, so that the key can never collide with the plain IP address keys
   * that the first factor writes into the very same cache. That matters, because the key of the
   * first factor is cleared on every successful login - which the attacker that brute forces the
   * second factor can trigger at will, as they know the password by definition.
   */
  @VisibleForTesting
  static final String FAILED_TOTP_THROTTLE_KEY_SUFFIX = "-totp";

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

    // The credentials are valid - check the second factor, but only if the user enabled it.
    // The session check comes first, because it avoids the backend lookup for the majority of the
    // requests
    if (!SMPSecondFactorHelper.isSecondFactorProvided (sCurrentUserID) &&
        SMPSecondFactorHelper.isSecondFactorRequired (sCurrentUserID))
    {
      return _checkSecondFactor (sCurrentUserID, aRequestScope, aUnifiedResponse);
    }

    return EContinue.CONTINUE;
  }

  /**
   * Determine the time to wait after a failed second factor validation, using an exponential
   * backoff, capped at {@link #FAILED_TOTP_MAX_WAIT_TIME}.
   *
   * @param nFailureCount
   *        The number of consecutive failures. Must be &ge; 1.
   * @return The duration to wait. Never <code>null</code>.
   */
  @NonNull
  private static Duration _getBackoffWaitTime (final int nFailureCount)
  {
    final int nExponent = Math.min (Math.max (nFailureCount - 1, 0), FAILED_TOTP_MAX_EXPONENT);
    final Duration aWaitTime = FAILED_TOTP_BASE_WAIT_TIME.multipliedBy (1L << nExponent);
    return aWaitTime.compareTo (FAILED_TOTP_MAX_WAIT_TIME) > 0 ? FAILED_TOTP_MAX_WAIT_TIME : aWaitTime;
  }

  @NonNull
  private EContinue _checkSecondFactor (@NonNull final String sCurrentUserID,
                                        @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                        @NonNull final UnifiedResponse aUnifiedResponse)
  {
    // Throttling happens per user - see FAILED_TOTP_THROTTLE_KEY_SUFFIX
    final String sThrottleKey = sCurrentUserID + FAILED_TOTP_THROTTLE_KEY_SUFFIX;
    // Only used for logging, resolved the same way as for the first factor, so that a reverse
    // proxy setup logs the same address for both
    final String sIP = m_aLogin.getRemoteAddressForThrottling (aRequestScope);
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
          SMPSecondFactorHelper.markSecondFactorProvided (sCurrentUserID);
          // Successful second factor - remove the failed counter of this user
          LoginThrottlePerIP.getInstance ().onSuccessfulLogin (sThrottleKey);
          // Session fixation hardening: a new CSRF nonce is created. The HTTP session ID itself
          // cannot be changed here, because the ph-oton login state is bound to the session scope
          // ID and would be lost on a session renewal.
          CSRFSessionManager.getInstance ().generateNewNonce ();

          LOGGER.info ("Successfully verified the second authentication factor of user ID '" + sCurrentUserID + "'");

          // Avoid a double submit by redirecting to the desired destination URL
          aUnifiedResponse.setRedirect (aRequestScope.getURIDecoded ());
          return EContinue.BREAK;
        }

        bError = true;
        sErrorMsg = "The provided authenticator code or recovery code is invalid. Please try again.";

        // Slow down brute force attempts on the second factor, using an exponential backoff.
        // A counter in the session would be reset by simply discarding the session cookie
        final int nFailureCount = LoginThrottlePerIP.getInstance ().onFailedLogin (sThrottleKey);
        final Duration aWaitTime = _getBackoffWaitTime (nFailureCount);
        LOGGER.warn ("The second factor of user ID '" +
                     sCurrentUserID +
                     "' failed " +
                     nFailureCount +
                     " consecutive time(s), the last one from IP address '" +
                     sIP +
                     "' - waiting " +
                     aWaitTime.toMillis () +
                     " milliseconds");
        ThreadHelper.sleep (aWaitTime);
      }
    }

    // Show the second factor screen
    PhotonHTMLHelper.createHTMLResponse (aRequestScope,
                                         aUnifiedResponse,
                                         new SMPSecondFactorHTMLProvider (bError, sErrorMsg));
    return EContinue.BREAK;
  }
}
