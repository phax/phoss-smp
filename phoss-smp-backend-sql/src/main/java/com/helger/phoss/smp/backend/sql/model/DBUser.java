/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.sql.model;

import javax.annotation.Nonnull;

import com.helger.phoss.smp.domain.user.ISMPUserEditable;

/**
 * Represents a single user within the SMP database.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public class DBUser implements ISMPUserEditable
{
  private String m_sUserName;
  private String m_sPassword;

  public DBUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    setUserName (sUserName);
    setPassword (sPassword);
  }

  public String getID ()
  {
    return getUserName ();
  }

  public String getUserName ()
  {
    return m_sUserName;
  }

  public void setUserName (@Nonnull final String sUserName)
  {
    m_sUserName = sUserName;
  }

  public String getPassword ()
  {
    return m_sPassword;
  }

  public void setPassword (@Nonnull final String sPassword)
  {
    m_sPassword = sPassword;
  }
}
