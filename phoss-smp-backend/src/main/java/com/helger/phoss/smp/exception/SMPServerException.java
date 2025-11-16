/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
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

/**
 * Base class for all SMP server specific exceptions
 *
 * @author Philip Helger
 */
public class SMPServerException extends Exception
{
  public SMPServerException (@NonNull final String sMessage)
  {
    super (sMessage);
  }

  public SMPServerException (@NonNull final String sMessage, @NonNull final Throwable aCause)
  {
    super (sMessage, aCause);
  }
}
