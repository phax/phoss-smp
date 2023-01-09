/*
 * Copyright (C) 2015-2023 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.exception;

import java.net.URI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTP 412 (Precondition Failed) exception wrapper
 *
 * @author Philip Helger
 * @since 5.6.0
 */
public class SMPPreconditionFailedException extends SMPServerException
{
  /**
   * Create a HTTP 412 (Precondition Failed) exception.
   *
   * @param sMessage
   *        the String that is the entity of the HTTP response.
   * @param aEffectedURI
   *        The URI effected.
   */
  public SMPPreconditionFailedException (@Nonnull final String sMessage, @Nullable final URI aEffectedURI)
  {
    super ("Precondition failed: " + sMessage + (aEffectedURI == null ? "" : " at '" + aEffectedURI.toString () + "'"));
  }
}
