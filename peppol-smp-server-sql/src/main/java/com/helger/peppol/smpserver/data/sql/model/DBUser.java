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
package com.helger.peppol.smpserver.data.sql.model;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.db.jpa.annotation.UsedOnlyByJPA;
import com.helger.peppol.smpserver.domain.user.ISMPUserEditable;

/**
 * Represents a single user within the SMP database.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Entity
@Table (name = "smp_user")
public class DBUser implements ISMPUserEditable
{
  private String m_sUserName;
  private String m_sPassword;
  private Set <DBOwnership> m_aOwnerships = new CommonsHashSet <> ();

  @Deprecated
  @UsedOnlyByJPA
  public DBUser ()
  {}

  public DBUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    setUserName (sUserName);
    setPassword (sPassword);
  }

  @Transient
  public String getID ()
  {
    return getUserName ();
  }

  @Id
  @Column (name = "username", nullable = false, length = 256)
  public String getUserName ()
  {
    return m_sUserName;
  }

  public void setUserName (@Nonnull final String sUserName)
  {
    m_sUserName = sUserName;
  }

  @Column (name = "password", nullable = false, length = 256)
  public String getPassword ()
  {
    return m_sPassword;
  }

  public void setPassword (@Nonnull final String sPassword)
  {
    m_sPassword = sPassword;
  }

  @OneToMany (fetch = FetchType.LAZY, mappedBy = "user", cascade = { CascadeType.ALL })
  public Set <DBOwnership> getOwnerships ()
  {
    return m_aOwnerships;
  }

  public void setOwnerships (final Set <DBOwnership> aOwnerships)
  {
    m_aOwnerships = aOwnerships;
  }
}
