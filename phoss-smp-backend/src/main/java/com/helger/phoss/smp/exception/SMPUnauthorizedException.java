/*
 * Copyright (C) 2015-2021 Philip Helger and contributors
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception to be thrown if there is an ownership mismatch between object. This
 * exception is only thrown if the provided user credentials are valid.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public class SMPUnauthorizedException extends SMPServerException
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPUnauthorizedException.class);

  public SMPUnauthorizedException (@Nonnull final String sMessage)
  {
    this (sMessage, null);
  }

  public SMPUnauthorizedException (@Nonnull final String sMessage, @Nullable final URI aNotFoundURI)
  {
    super (sMessage + (aNotFoundURI == null ? "" : " at " + aNotFoundURI));

    // Always log!
    LOGGER.warn (sMessage + (aNotFoundURI == null ? "" : " at " + aNotFoundURI));
  }
}
