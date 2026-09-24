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
package com.helger.phoss.smp.exception;

import org.jspecify.annotations.NonNull;

import com.helger.base.string.StringHelper;

/**
 * This exception is thrown if an error occurred communicating with the SML
 *
 * @author Philip Helger
 * @since 5.1.0
 */
public class SMPSMLException extends SMPServerException
{
  /**
   * Append the message of the causing exception, so that the SML fault details survive into the
   * REST error payload - only the message of the top-level exception is serialized there (see
   * <code>SMPRestExceptionMapper</code>). This mirrors what
   * <code>RegistrationHookException</code> does with the SOAP fault message.
   *
   * @param sMsg
   *        The message of this exception. May not be <code>null</code>.
   * @param aCause
   *        The causing exception. May not be <code>null</code>.
   * @return The message to be used, never <code>null</code>.
   */
  @NonNull
  private static String _getRealMessage (@NonNull final String sMsg, @NonNull final Exception aCause)
  {
    return StringHelper.getConcatenatedOnDemand (sMsg, " - ", aCause.getMessage ());
  }

  public SMPSMLException (@NonNull final String sMsg, @NonNull final Exception aCause)
  {
    super (_getRealMessage (sMsg, aCause), aCause);
  }
}
