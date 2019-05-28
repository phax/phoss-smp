/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.exception;

import javax.annotation.Nonnull;

/**
 * Exception that is thrown to indicate an HTTP 500 error.
 *
 * @author Philip Helger
 * @since 5.1.0
 */
public class SMPInternalErrorException extends SMPServerException
{
  public SMPInternalErrorException (@Nonnull final String sMsg, @Nonnull final Throwable aCause)
  {
    super (sMsg, aCause);
  }
}
