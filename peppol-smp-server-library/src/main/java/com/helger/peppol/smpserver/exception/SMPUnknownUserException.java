/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.exception;

import javax.annotation.Nullable;

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

  public SMPUnknownUserException (@Nullable final String sUserName)
  {
    super ("Unknown user '" + sUserName + "'");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (getMessage ());
    m_sUserName = sUserName;
  }

  /**
   * @return The user name which was not found. May be <code>null</code>.
   */
  @Nullable
  public String getUserName ()
  {
    return m_sUserName;
  }
}
