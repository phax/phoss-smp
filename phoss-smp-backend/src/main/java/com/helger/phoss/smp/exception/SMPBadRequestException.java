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
   * @param aEffectedURI
   *        The URI that was not found.
   */
  public SMPBadRequestException (@Nonnull final String sMessage, @Nullable final URI aEffectedURI)
  {
    super ("Bad request: " + sMessage + (aEffectedURI == null ? "" : " at '" + aEffectedURI.toString () + "'"));
  }

  @Nonnull
  public static SMPBadRequestException failedToParseSG (@Nonnull final String sServiceGroupID,
                                                        @Nullable final URI aEffectedURI)
  {
    return new SMPBadRequestException ("Failed to parse Service Group ID '" + sServiceGroupID + "'", aEffectedURI);
  }

  @Nonnull
  public static SMPBadRequestException failedToParseDocType (@Nonnull final String sDocTypeID,
                                                             @Nullable final URI aEffectedURI)
  {
    return new SMPBadRequestException ("Failed to parse Document Type ID '" + sDocTypeID + "'", aEffectedURI);
  }
}
