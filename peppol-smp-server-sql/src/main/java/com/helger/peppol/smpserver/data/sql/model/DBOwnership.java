/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
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

  public void setServiceGroup (final DBServiceGroup aServiceGroup)
  {
    m_aServiceGroup = aServiceGroup;
  }
}
