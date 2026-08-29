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

import java.net.URI;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * HTTP 503 (Service Unavailable) exception wrapper. To be used for actions that are temporarily not
 * possible, but that may be retried later.
 *
 * @author Philip Helger
 * @since 8.2.1
 */
public class SMPServiceUnavailableException extends SMPServerException
{
  /**
   * Create a HTTP 503 (Service Unavailable) exception.
   *
   * @param sMessage
   *        the String that is the entity of the HTTP response.
   * @param aEffectedURI
   *        The URI effected.
   */
  public SMPServiceUnavailableException (@NonNull final String sMessage, @Nullable final URI aEffectedURI)
  {
    super ("Service unavailable: " +
           sMessage +
           (aEffectedURI == null ? "" : " at '" + aEffectedURI.toString () + "'"));
  }
}
