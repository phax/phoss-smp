/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.smlhook;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.string.StringHelper;
import com.helger.peppol.smlclient.SMLExceptionHelper;

/**
 * This exception is thrown when communicating with the SML failed.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class RegistrationHookException extends Exception
{
  @Nonnull
  private static String _getRealMessage (@Nullable final String sMsg, @Nullable final Throwable aCause)
  {
    final String ret = StringHelper.getNotNull (sMsg);
    final String sFaultMessage = SMLExceptionHelper.getFaultMessage (aCause);
    return StringHelper.getConcatenatedOnDemand (ret, " - ", sFaultMessage);
  }

  public RegistrationHookException (@Nullable final String sMsg, @Nullable final Throwable aCause)
  {
    super (_getRealMessage (sMsg, aCause), aCause);
  }
}
