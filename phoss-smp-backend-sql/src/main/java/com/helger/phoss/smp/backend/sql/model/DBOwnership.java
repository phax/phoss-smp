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

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.helger.db.jpa.annotation.UsedOnlyByJPA;

/**
 * Define the ownership of a service group -&gt; relates DB user to DB service
 * group.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Entity
@Table (name = "smp_ownership")
public class DBOwnership implements Serializable
{
  private DBOwnershipID m_aID;
  private DBUser m_aUser;

  @Deprecated
  @UsedOnlyByJPA
  public DBOwnership ()
  {}

  public DBOwnership (final DBOwnershipID aID, final DBUser aUser)
  {
    m_aID = aID;
    m_aUser = aUser;
  }

  @EmbeddedId
  public DBOwnershipID getId ()
  {
    return m_aID;
  }

  @Deprecated
  @UsedOnlyByJPA
  public void setId (final DBOwnershipID aID)
  {
    m_aID = aID;
  }

  // No Cascade here!
  @ManyToOne (fetch = FetchType.LAZY)
  @JoinColumn (name = "username", nullable = false, insertable = false, updatable = false)
  public DBUser getUser ()
  {
    return m_aUser;
  }

  public void setUser (final DBUser aUser)
  {
    m_aUser = aUser;
  }
}
