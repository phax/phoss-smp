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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.totp.ISMPUserTotp;
import com.helger.phoss.smp.domain.totp.ISMPUserTotpManager;
import com.helger.phoss.smp.domain.totp.SMPTotpHelper;
import com.helger.phoss.smp.domain.totp.SMPUserTotpEnabledCache;
import com.helger.photon.security.login.GlobalUserIDProvider;
import com.helger.web.scope.ISessionWebScope;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * Helper around the second authentication factor (TOTP) of the <code>/secure</code> application.
 * The first factor (login name and password) is handled by {@link SMPLoginManager}. If - and only
 * if - the logged in user has TOTP enabled, an additional intermediate step is required, before any
 * secure page may be accessed.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
@Immutable
public final class SMPSecondFactorHelper
{
  /** The name of the request parameter that triggers the validation of the second factor. */
  public static final String REQUEST_PARAM_ACTION = "totp-action";
  /** The value of {@link #REQUEST_PARAM_ACTION} that triggers the validation. */
  public static final String REQUEST_ACTION_VALIDATE_TOTP = "validate-totp";
  /** The name of the request parameter that contains the one-time password. */
  public static final String REQUEST_ATTR_TOTP_CODE = "totpcode";

  /** The session attribute that remembers the user ID that provided the second factor. */
  private static final String SESSION_ATTR_TOTP_VERIFIED_USER_ID = "$phoss-smp.totp.verified.userid";
  /** The session attribute that counts the consecutive failed second factor validations. */
  private static final String SESSION_ATTR_TOTP_FAILED_COUNT = "$phoss-smp.totp.failedcount";

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPSecondFactorHelper.class);

  private SMPSecondFactorHelper ()
  {}

  @Nullable
  private static ISessionWebScope _getSessionScope (final boolean bCreateIfNotExisting)
  {
    try
    {
      return WebScopeManager.getSessionScope (bCreateIfNotExisting);
    }
    catch (final RuntimeException ex)
    {
      return null;
    }
  }

  /**
   * Check if the provided user needs to provide a second authentication factor. The result is
   * cached for {@link SMPUserTotpEnabledCache#TIME_TO_LIVE}, to avoid a backend roundtrip on every
   * single request.
   *
   * @param sUserID
   *        The ID of the user in question. May be <code>null</code>.
   * @return <code>true</code> if TOTP is globally enabled and enabled for that user.
   */
  public static boolean isSecondFactorRequired (@Nullable final String sUserID)
  {
    // If the feature is globally disabled, existing enrollments are not enforced
    if (!SMPServerConfiguration.isTotpEnabled ())
      return false;

    return SMPUserTotpEnabledCache.isTotpEnabled (sUserID);
  }

  /**
   * Check if the second factor was already provided in the current session by the provided user.
   * The user ID is part of the check, so that a session that is reused by another user (e.g. after
   * a re-login) needs to provide the second factor again.
   *
   * @param sUserID
   *        The ID of the user in question. May be <code>null</code>.
   * @return <code>true</code> if the second factor was already provided by that user.
   */
  public static boolean isSecondFactorProvided (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return false;

    final ISessionWebScope aSessionScope = _getSessionScope (false);
    if (aSessionScope == null)
      return false;

    return sUserID.equals (aSessionScope.attrs ().getAsString (SESSION_ATTR_TOTP_VERIFIED_USER_ID));
  }

  /**
   * Remember, that the second factor was successfully provided by the provided user in the current
   * session.
   *
   * @param sUserID
   *        The ID of the user that provided the second factor. May neither be <code>null</code> nor
   *        empty.
   */
  public static void markSecondFactorProvided (@NonNull @Nonempty final String sUserID)
  {
    ValueEnforcer.notEmpty (sUserID, "UserID");

    final ISessionWebScope aSessionScope = _getSessionScope (true);
    aSessionScope.attrs ().putIn (SESSION_ATTR_TOTP_VERIFIED_USER_ID, sUserID);
    aSessionScope.attrs ().remove (SESSION_ATTR_TOTP_FAILED_COUNT);
  }

  /**
   * Forget, that the second factor was provided in the current session. This is called on every
   * login, so that a new login always requires a new second factor.
   */
  public static void resetSecondFactorProvided ()
  {
    final ISessionWebScope aSessionScope = _getSessionScope (false);
    if (aSessionScope != null)
    {
      aSessionScope.attrs ().remove (SESSION_ATTR_TOTP_VERIFIED_USER_ID);
      aSessionScope.attrs ().remove (SESSION_ATTR_TOTP_FAILED_COUNT);
    }
  }

  /**
   * Remember another failed second factor validation in the current session.
   *
   * @return The number of consecutive failed second factor validations of the current session,
   *         including the current one. Always &ge; 1.
   */
  @Nonnegative
  public static int incrementSecondFactorFailureCount ()
  {
    final ISessionWebScope aSessionScope = _getSessionScope (true);
    final int nNewCount = aSessionScope.attrs ().getAsInt (SESSION_ATTR_TOTP_FAILED_COUNT, 0) + 1;
    aSessionScope.attrs ().putIn (SESSION_ATTR_TOTP_FAILED_COUNT, nNewCount);
    return nNewCount;
  }

  /**
   * @return The number of consecutive failed second factor validations of the current session.
   *         Always &ge; 0.
   */
  @Nonnegative
  public static int getSecondFactorFailureCount ()
  {
    final ISessionWebScope aSessionScope = _getSessionScope (false);
    return aSessionScope == null ? 0 : aSessionScope.attrs ().getAsInt (SESSION_ATTR_TOTP_FAILED_COUNT, 0);
  }

  /**
   * Check if the currently logged in user may access the secure area. This is the case, if either
   * no second factor is required or if it was already provided in the current session.
   *
   * @return <code>true</code> if the secure area may be accessed.
   */
  public static boolean isSecureAccessAllowed ()
  {
    final String sUserID = GlobalUserIDProvider.getCurrentUserID ();
    // Check the session first - that avoids the backend/cache lookup for verified sessions
    if (isSecondFactorProvided (sUserID))
      return true;
    return !isSecondFactorRequired (sUserID);
  }

  /**
   * Validate the provided one-time password or recovery code for the provided user, including the
   * replay protection. On success the used time slot is remembered, resp. the used recovery code is
   * consumed.
   *
   * @param sUserID
   *        The ID of the user in question. May be <code>null</code>.
   * @param sCode
   *        The one-time password or recovery code provided by the user. May be <code>null</code>.
   * @return <code>true</code> if the code is valid and was not used before.
   */
  public static boolean isValidSecondFactor (@Nullable final String sUserID, @Nullable final String sCode)
  {
    if (StringHelper.isEmpty (sUserID) || StringHelper.isEmpty (sCode))
      return false;

    final ISMPUserTotpManager aTotpMgr = SMPMetaManager.getUserTotpMgr ();
    if (aTotpMgr == null)
      return false;

    final ISMPUserTotp aTotp = aTotpMgr.getTotpOfUserID (sUserID);
    if (aTotp == null || !aTotp.isEnabled ())
      return false;

    // Determine the time slot the code matched - the current time slot is not sufficient, because
    // a code is accepted in the whole discrepancy window
    final Long aMatchingTimeSlot = SMPTotpHelper.getMatchingTimeSlot (aTotp.getSecret (), sCode);
    if (aMatchingTimeSlot == null)
    {
      // No valid one-time password - maybe it is a recovery code
      return _isValidRecoveryCode (aTotpMgr, sUserID, sCode);
    }

    // Prevent a replay of an already used one-time password. The check and the update are performed
    // atomically by the manager, so that two parallel submissions cannot both succeed.
    if (aTotpMgr.setTotpLastUsedTimeSlot (sUserID, aMatchingTimeSlot.longValue ()).isUnchanged ())
    {
      LOGGER.warn ("The TOTP code provided for user ID '" + sUserID + "' was already used before");
      return false;
    }

    return true;
  }

  private static boolean _isValidRecoveryCode (@NonNull final ISMPUserTotpManager aTotpMgr,
                                               @NonNull final String sUserID,
                                               @NonNull final String sCode)
  {
    final String sRecoveryCodeHash = SMPTotpHelper.getRecoveryCodeHash (sCode);
    if (sRecoveryCodeHash == null)
      return false;

    // Consuming is atomic, so that each recovery code can be used exactly once
    if (aTotpMgr.consumeRecoveryCodeHash (sUserID, sRecoveryCodeHash).isUnchanged ())
    {
      LOGGER.warn ("The second factor provided for user ID '" + sUserID + "' is invalid");
      return false;
    }

    LOGGER.info ("User ID '" + sUserID + "' used a recovery code as the second authentication factor");
    return true;
  }

  /**
   * Check if the current request contains the submission of the second factor form.
   *
   * @param sParamValue
   *        The value of the {@link #REQUEST_PARAM_ACTION} request parameter. May be
   *        <code>null</code>.
   * @return <code>true</code> if the second factor form was submitted.
   */
  public static boolean isValidationInProgress (@Nullable final String sParamValue)
  {
    return REQUEST_ACTION_VALIDATE_TOTP.equals (sParamValue);
  }
}
