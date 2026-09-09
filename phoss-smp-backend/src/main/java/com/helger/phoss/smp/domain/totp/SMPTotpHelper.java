/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.totp;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.totp.CTotp;
import com.helger.totp.code.DefaultCodeGenerator;
import com.helger.totp.code.DefaultCodeVerifier;
import com.helger.totp.code.EHashingAlgorithm;
import com.helger.totp.code.ICodeVerifier;
import com.helger.totp.qr.QrData;
import com.helger.totp.secret.DefaultSecretGenerator;
import com.helger.totp.time.ITimeProvider;
import com.helger.totp.time.SystemTimeProvider;

/**
 * Central helper around the <code>ph-totp</code> library. It defines the TOTP parameters used by
 * this application and offers secret generation as well as code verification.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
@Immutable
public final class SMPTotpHelper
{
  /** The hashing algorithm used. It must match the one encoded in the otpauth:// URI. */
  public static final EHashingAlgorithm HASHING_ALGORITHM = EHashingAlgorithm.SHA1;
  /** The number of digits of a one-time password. */
  public static final int CODE_DIGITS = CTotp.DEFAULT_CODE_DIGITS;
  /** The length of a time slot in seconds. */
  public static final int TIME_PERIOD_SECS = CTotp.DEFAULT_TIME_PERIOD_SECS;
  /** The number of time slots before and after the current one that are accepted. */
  public static final int TIME_PERIOD_DISCREPANCY = CTotp.DEFAULT_TIME_PERIOD_DISCREPANCY;

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPTotpHelper.class);

  private static final ITimeProvider TIME_PROVIDER = new SystemTimeProvider ();
  private static final ICodeVerifier CODE_VERIFIER = new DefaultCodeVerifier (new DefaultCodeGenerator (HASHING_ALGORITHM,
                                                                                                        CODE_DIGITS),
                                                                              TIME_PROVIDER).setTimePeriod (TIME_PERIOD_SECS)
                                                                                            .setAllowedTimePeriodDiscrepancy (TIME_PERIOD_DISCREPANCY);

  private SMPTotpHelper ()
  {}

  /**
   * @return A new random Base32 encoded shared secret. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  public static String createNewSecret ()
  {
    return new DefaultSecretGenerator ().generate ();
  }

  /**
   * @return The TOTP time slot that is currently valid.
   */
  public static long getCurrentTimeSlot ()
  {
    return Math.floorDiv (TIME_PROVIDER.getTime (), TIME_PERIOD_SECS);
  }

  /**
   * Check if the provided one-time password matches the provided secret.
   *
   * @param sSecret
   *        The Base32 encoded shared secret. May be <code>null</code>.
   * @param sCode
   *        The one-time password provided by the user. May be <code>null</code>.
   * @return <code>true</code> if the code is valid, <code>false</code> otherwise.
   */
  public static boolean isValidCode (@Nullable final String sSecret, @Nullable final String sCode)
  {
    if (StringHelper.isEmpty (sSecret) || StringHelper.isEmpty (sCode))
      return false;

    try
    {
      return CODE_VERIFIER.isValidCode (sSecret, sCode.trim ());
    }
    catch (final RuntimeException ex)
    {
      // Never let a malformed secret or code break the login
      LOGGER.warn ("Failed to verify the provided TOTP code: " + ex.getMessage ());
      return false;
    }
  }

  /**
   * Build the <code>otpauth://</code> URI to be encoded in a QR code.
   *
   * @param sIssuer
   *        The issuer to be displayed in the authenticator app. May neither be <code>null</code>
   *        nor empty.
   * @param sLabel
   *        The account label to be displayed in the authenticator app - usually the login name. May
   *        neither be <code>null</code> nor empty.
   * @param sSecret
   *        The Base32 encoded shared secret. May neither be <code>null</code> nor empty.
   * @return The <code>otpauth://</code> URI. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  public static String getOtpAuthURI (@NonNull @Nonempty final String sIssuer,
                                      @NonNull @Nonempty final String sLabel,
                                      @NonNull @Nonempty final String sSecret)
  {
    return new QrData.Builder ().label (sLabel)
                                .secret (sSecret)
                                .issuer (sIssuer)
                                .algorithm (HASHING_ALGORITHM)
                                .digits (CODE_DIGITS)
                                .period (TIME_PERIOD_SECS)
                                .build ()
                                .getUri ();
  }

  /**
   * Build the {@link QrData} object describing a TOTP enrollment.
   *
   * @param sIssuer
   *        The issuer to be displayed in the authenticator app. May neither be <code>null</code>
   *        nor empty.
   * @param sLabel
   *        The account label to be displayed in the authenticator app - usually the login name. May
   *        neither be <code>null</code> nor empty.
   * @param sSecret
   *        The Base32 encoded shared secret. May neither be <code>null</code> nor empty.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static QrData getQrData (@NonNull @Nonempty final String sIssuer,
                                  @NonNull @Nonempty final String sLabel,
                                  @NonNull @Nonempty final String sSecret)
  {
    return new QrData.Builder ().label (sLabel)
                                .secret (sSecret)
                                .issuer (sIssuer)
                                .algorithm (HASHING_ALGORITHM)
                                .digits (CODE_DIGITS)
                                .period (TIME_PERIOD_SECS)
                                .build ();
  }
}
