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
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;

/**
 * ServiceGroupId == participant ID
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Embeddable
public class DBServiceGroupID implements Serializable
{
  private String m_sParticipantIdentifierScheme;
  private String m_sParticipantIdentifier;

  @Deprecated
  @UsedOnlyByJPA
  public DBServiceGroupID ()
  {}

  public DBServiceGroupID (@Nonnull final IParticipantIdentifier aBusinessID)
  {
    setBusinessIdentifierScheme (aBusinessID.getScheme ());
    setBusinessIdentifier (aBusinessID.getValue ());
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
  @Nonnull
  public IParticipantIdentifier getAsBusinessIdentifier ()
  {
    return new SimpleParticipantIdentifier (m_sParticipantIdentifierScheme, m_sParticipantIdentifier);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final DBServiceGroupID rhs = (DBServiceGroupID) o;
    return EqualsHelper.equals (m_sParticipantIdentifierScheme, rhs.m_sParticipantIdentifierScheme) &&
           EqualsHelper.equals (m_sParticipantIdentifier, rhs.m_sParticipantIdentifier);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sParticipantIdentifierScheme)
                                       .append (m_sParticipantIdentifier)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("participantIDScheme", m_sParticipantIdentifierScheme)
                                       .append ("participantIDValue", m_sParticipantIdentifier)
                                       .getToString ();
  }
}
