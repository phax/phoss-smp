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

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
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
  private DBServiceGroup m_aServiceGroup;

  @Deprecated
  @UsedOnlyByJPA
  public DBOwnership ()
  {}

  public DBOwnership (final DBOwnershipID aID, final DBUser aUser, final DBServiceGroup aServiceGroup)
  {
    m_aID = aID;
    m_aUser = aUser;
    m_aServiceGroup = aServiceGroup;
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

  // No Cascade here!
  @OneToOne (fetch = FetchType.LAZY)
  @JoinColumns ({ @JoinColumn (name = "businessIdentifierScheme",
                               referencedColumnName = "businessIdentifierScheme",
                               nullable = false,
                               insertable = false,
                               updatable = false),
                  @JoinColumn (name = "businessIdentifier",
                               referencedColumnName = "businessIdentifier",
                               nullable = false,
                               insertable = false,
                               updatable = false) })
  public DBServiceGroup getServiceGroup ()
  {
    return m_aServiceGroup;
  }

  @Deprecated
  @UsedOnlyByJPA
  public void setServiceGroup (final DBServiceGroup aServiceGroup)
  {
    m_aServiceGroup = aServiceGroup;
  }
}
