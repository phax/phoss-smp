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
 * HTTP 404 exception wrapper
 *
 * @author Philip Helger
 */
public class SMPNotFoundException extends SMPServerException
{
  /**
   * Create a HTTP 404 (Not Found) exception.
   *
   * @param sMessage
   *        the String that is the entity of the 404 response.
   */
  public SMPNotFoundException (@NonNull final String sMessage)
  {
    this (sMessage, null);
  }

  /**
   * Create a HTTP 404 (Not Found) exception.
   *
   * @param sMessage
   *        the String that is the entity of the 404 response.
   * @param aNotFoundURI
   *        The URI that was not found.
   */
  public SMPNotFoundException (@NonNull final String sMessage, @Nullable final URI aNotFoundURI)
  {
    super ("Not found: " + sMessage + (aNotFoundURI == null ? "" : " at " + aNotFoundURI));
  }

  @NonNull
  public static SMPNotFoundException unknownSG (@NonNull final String sServiceGroupID, @Nullable final URI aEffectedURI)
  {
    return new SMPNotFoundException ("Service Group '" + sServiceGroupID + "' is not registered on this SMP",
                                     aEffectedURI);
  }

  @NonNull
  public static SMPNotFoundException unknownDocType (@NonNull final String sServiceGroupID,
                                                     @NonNull final String sDocTypeID,
                                                     @Nullable final URI aEffectedURI)
  {
    return new SMPNotFoundException ("Service Group '" +
                                     sServiceGroupID +
                                     "' doesn't support the document type ID '" +
                                     sDocTypeID +
                                     "'",
                                     aEffectedURI);
  }

  @NonNull
  public static SMPNotFoundException unknownServiceInformation (@NonNull final String sServiceGroupID,
                                                                @NonNull final String sDocTypeID,
                                                                @Nullable final URI aEffectedURI)
  {
    return new SMPNotFoundException ("Service Information for '" +
                                     sServiceGroupID +
                                     "' and '" +
                                     sDocTypeID +
                                     "' is not registered on this SMP",
                                     aEffectedURI);
  }

  @NonNull
  public static SMPNotFoundException unknownServiceRedirect (@NonNull final String sServiceGroupID,
                                                             @NonNull final String sDocTypeID,
                                                             @Nullable final URI aEffectedURI)
  {
    return new SMPNotFoundException ("Service Redirect for '" +
                                     sServiceGroupID +
                                     "' and '" +
                                     sDocTypeID +
                                     "' is not registered on this SMP",
                                     aEffectedURI);
  }
}
