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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.totp.ISMPUserTotp;
import com.helger.phoss.smp.domain.totp.ISMPUserTotpManager;
import com.helger.phoss.smp.domain.totp.SMPTotpHelper;
import com.helger.photon.security.login.LoggedInUserManager;
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

  /** The session attribute that remembers, that the second factor was provided. */
  private static final String SESSION_ATTR_TOTP_VERIFIED = "$phoss-smp.totp.verified";

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
   * Check if the provided user needs to provide a second authentication factor.
   *
   * @param sUserID
   *        The ID of the user in question. May be <code>null</code>.
   * @return <code>true</code> if TOTP is enabled for that user.
   */
  public static boolean isSecondFactorRequired (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return false;

    final ISMPUserTotpManager aTotpMgr = SMPMetaManager.getUserTotpMgr ();
    return aTotpMgr != null && aTotpMgr.isTotpEnabled (sUserID);
  }

  /**
   * @return <code>true</code> if the second factor was already provided in the current session.
   */
  public static boolean isSecondFactorProvided ()
  {
    final ISessionWebScope aSessionScope = _getSessionScope (false);
    return aSessionScope != null && aSessionScope.attrs ().getAsBoolean (SESSION_ATTR_TOTP_VERIFIED, false);
  }

  /**
   * Remember, that the second factor was successfully provided in the current session.
   */
  public static void markSecondFactorProvided ()
  {
    final ISessionWebScope aSessionScope = _getSessionScope (true);
    if (aSessionScope != null)
      aSessionScope.attrs ().putIn (SESSION_ATTR_TOTP_VERIFIED, true);
  }

  /**
   * Forget, that the second factor was provided in the current session.
   */
  public static void resetSecondFactorProvided ()
  {
    final ISessionWebScope aSessionScope = _getSessionScope (false);
    if (aSessionScope != null)
      aSessionScope.attrs ().remove (SESSION_ATTR_TOTP_VERIFIED);
  }

  /**
   * Check if the currently logged in user may access the secure area. This is the case, if either
   * no second factor is required or if it was already provided in the current session.
   *
   * @return <code>true</code> if the secure area may be accessed.
   */
  public static boolean isSecureAccessAllowed ()
  {
    final String sUserID = LoggedInUserManager.getInstance ().getCurrentUserID ();
    if (!isSecondFactorRequired (sUserID))
      return true;
    return isSecondFactorProvided ();
  }

  /**
   * Validate the provided one-time password for the provided user, including the replay protection.
   * On success the used time slot is remembered.
   *
   * @param sUserID
   *        The ID of the user in question. May be <code>null</code>.
   * @param sCode
   *        The one-time password provided by the user. May be <code>null</code>.
   * @return <code>true</code> if the code is valid and was not used before.
   */
  public static boolean isValidSecondFactor (@Nullable final String sUserID, @Nullable final String sCode)
  {
    if (StringHelper.isEmpty (sUserID))
      return false;

    final ISMPUserTotpManager aTotpMgr = SMPMetaManager.getUserTotpMgr ();
    if (aTotpMgr == null)
      return false;

    final ISMPUserTotp aTotp = aTotpMgr.getTotpOfUserID (sUserID);
    if (aTotp == null || !aTotp.isEnabled ())
      return false;

    if (!SMPTotpHelper.isValidCode (aTotp.getSecret (), sCode))
    {
      LOGGER.warn ("The TOTP code provided for user ID '" + sUserID + "' is invalid");
      return false;
    }

    // Prevent a replay of an already used one-time password
    final long nTimeSlot = SMPTotpHelper.getCurrentTimeSlot ();
    final Long aLastUsedTimeSlot = aTotp.getLastUsedTimeSlot ();
    if (aLastUsedTimeSlot != null && nTimeSlot <= aLastUsedTimeSlot.longValue ())
    {
      LOGGER.warn ("The TOTP code provided for user ID '" + sUserID + "' was already used before");
      return false;
    }
    aTotpMgr.setTotpLastUsedTimeSlot (sUserID, nTimeSlot);

    return true;
  }

  /**
   * Check if the current request contains the submission of the second factor form.
   *
   * @param aParamValue
   *        The value of the {@link #REQUEST_PARAM_ACTION} request parameter. May be
   *        <code>null</code>.
   * @return <code>true</code> if the second factor form was submitted.
   */
  public static boolean isValidationInProgress (@Nullable final String aParamValue)
  {
    return REQUEST_ACTION_VALIDATE_TOTP.equals (aParamValue);
  }
}
