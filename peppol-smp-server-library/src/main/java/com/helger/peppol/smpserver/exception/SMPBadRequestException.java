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
package com.helger.peppol.smpserver.exception;

import java.net.URI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTP 400 (Bad Request) exception wrapper
 *
 * @author Philip Helger
 * @since 5.1.0
 */
public class SMPBadRequestException extends SMPServerException
{
  /**
   * Create a HTTP 400 (Bad request) exception.
   *
   * @param sMessage
   *        the String that is the entity of the HTTP response.
   * @param aNotFoundURI
   *        The URI that was not found.
   */
  public SMPBadRequestException (@Nonnull final String sMessage, @Nullable final URI aNotFoundURI)
  {
    super ("Bad request: " + sMessage + (aNotFoundURI == null ? "" : " at " + aNotFoundURI));
  }
}
