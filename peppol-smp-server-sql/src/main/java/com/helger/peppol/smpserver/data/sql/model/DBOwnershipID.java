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

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.db.jpa.annotation.UsedOnlyByJPA;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;

/**
 * ID for the ownership
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Embeddable
public class DBOwnershipID implements Serializable
{
  private String m_sUsername;
  private String m_sParticipantIdentifierScheme;
  private String m_sParticipantIdentifier;

  @Deprecated
  @UsedOnlyByJPA
  public DBOwnershipID ()
  {}

  public DBOwnershipID (final String sUserName, @Nonnull final IParticipantIdentifier aBusinessIdentifier)
  {
    m_sUsername = sUserName;
    setBusinessIdentifier (aBusinessIdentifier);
  }

  @Column (name = "username", nullable = false, length = 256)
  public String getUsername ()
  {
    return m_sUsername;
  }

  public void setUsername (final String sUserName)
  {
    m_sUsername = sUserName;
  }

  @Column (name = "businessIdentifierScheme",
           nullable = false,
           length = PeppolIdentifierHelper.MAX_IDENTIFIER_SCHEME_LENGTH)
  public String getBusinessIdentifierScheme ()
  {
    return m_sParticipantIdentifierScheme;
  }

  public void setBusinessIdentifierScheme (final String sBusinessIdentifierScheme)
  {
    m_sParticipantIdentifierScheme = sBusinessIdentifierScheme;
  }

  @Column (name = "businessIdentifier", nullable = false, length = PeppolIdentifierHelper.MAX_PARTICIPANT_VALUE_LENGTH)
  public String getBusinessIdentifier ()
  {
    return m_sParticipantIdentifier;
  }

  public void setBusinessIdentifier (final String sBusinessIdentifier)
  {
    m_sParticipantIdentifier = sBusinessIdentifier;
  }

  @Transient
  public void setBusinessIdentifier (@Nonnull final IParticipantIdentifier aPI)
  {
    setBusinessIdentifierScheme (aPI.getScheme ());
    setBusinessIdentifier (aPI.getValue ());
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final DBOwnershipID rhs = (DBOwnershipID) o;
    return EqualsHelper.equals (m_sUsername, rhs.m_sUsername) &&
           EqualsHelper.equals (m_sParticipantIdentifierScheme, rhs.m_sParticipantIdentifierScheme) &&
           EqualsHelper.equals (m_sParticipantIdentifier, rhs.m_sParticipantIdentifier);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sUsername)
                                       .append (m_sParticipantIdentifierScheme)
                                       .append (m_sParticipantIdentifier)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("username", m_sUsername)
                                       .append ("participantIDScheme", m_sParticipantIdentifierScheme)
                                       .append ("participantIDValue", m_sParticipantIdentifier)
                                       .getToString ();
  }
}
