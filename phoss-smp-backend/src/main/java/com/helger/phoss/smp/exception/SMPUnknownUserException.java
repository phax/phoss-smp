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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This exception is thrown if the provided user name does not exist.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public class SMPUnknownUserException extends SMPServerException
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPUnknownUserException.class);

  private final String m_sUserName;

  @Deprecated (forRemoval = true, since = "8.2.0")
  public SMPUnknownUserException (@Nullable final String sUserName)
  {
    this (sUserName, "Unknown user '" + sUserName + "'");
  }

  /**
   * Constructor with an explicit error message, so that the message returned to the caller can be
   * made independent of the user name.
   *
   * @param sUserName
   *        The user name which was not found. May be <code>null</code>.
   * @param sMessage
   *        The error message to be used. May not be <code>null</code>.
   * @since 8.2.0
   */
  public SMPUnknownUserException (@Nullable final String sUserName, @NonNull final String sMessage)
  {
    super (sMessage);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (getMessage ());
    m_sUserName = sUserName;
  }

  /**
   * @return The user name which was not found. May be <code>null</code>.
   */
  @Nullable
  public final String getUserName ()
  {
    return m_sUserName;
  }
}
