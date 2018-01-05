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
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.identifier.generic.process.SimpleProcessIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;

/**
 * The ID of a single process.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Embeddable
public class DBProcessID implements Serializable
{
  private String m_sParticipantIdentifierScheme;
  private String m_sParticipantIdentifier;
  private String m_sDocumentTypeIdentifierScheme;
  private String m_sDocumentTypeIdentifier;
  private String m_sProcessIdentifierScheme;
  private String m_sProcessIdentifier;

  @Deprecated
  @UsedOnlyByJPA
  public DBProcessID ()
  {}

  public DBProcessID (@Nonnull final DBServiceMetadataID aSMID, @Nonnull final IProcessIdentifier aProcessID)
  {
    setBusinessIdentifier (aSMID.getAsBusinessIdentifier ());
    setDocumentTypeIdentifier (aSMID.getAsDocumentTypeIdentifier ());
    setProcessIdentifier (aProcessID);
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
  public void setBusinessIdentifier (@Nonnull final IParticipantIdentifier aBusinessIdentifier)
  {
    setBusinessIdentifierScheme (aBusinessIdentifier.getScheme ());
    setBusinessIdentifier (aBusinessIdentifier.getValue ());
  }

  @Column (name = "documentIdentifierScheme",
           nullable = false,
           length = PeppolIdentifierHelper.MAX_IDENTIFIER_SCHEME_LENGTH)
  public String getDocumentIdentifierScheme ()
  {
    return m_sDocumentTypeIdentifierScheme;
  }

  public void setDocumentIdentifierScheme (final String sDocumentIdentifierScheme)
  {
    m_sDocumentTypeIdentifierScheme = sDocumentIdentifierScheme;
  }

  @Column (name = "documentIdentifier",
           nullable = false,
           length = PeppolIdentifierHelper.MAX_DOCUEMNT_TYPE_VALUE_LENGTH)
  public String getDocumentIdentifier ()
  {
    return m_sDocumentTypeIdentifier;
  }

  public void setDocumentIdentifier (final String sDocumentIdentifier)
  {
    m_sDocumentTypeIdentifier = sDocumentIdentifier;
  }

  @Transient
  public void setDocumentTypeIdentifier (@Nonnull final IDocumentTypeIdentifier aDocumentTypeID)
  {
    setDocumentIdentifierScheme (aDocumentTypeID.getScheme ());
    setDocumentIdentifier (aDocumentTypeID.getValue ());
  }

  @Column (name = "processIdentifierType",
           nullable = false,
           length = PeppolIdentifierHelper.MAX_IDENTIFIER_SCHEME_LENGTH)
  public String getProcessIdentifierScheme ()
  {
    return m_sProcessIdentifierScheme;
  }

  public void setProcessIdentifierScheme (final String sProcessIdentifierScheme)
  {
    m_sProcessIdentifierScheme = sProcessIdentifierScheme;
  }

  @Column (name = "processIdentifier", nullable = false, length = PeppolIdentifierHelper.MAX_PROCESS_VALUE_LENGTH)
  public String getProcessIdentifier ()
  {
    return m_sProcessIdentifier;
  }

  public void setProcessIdentifier (final String sProcessIdentifier)
  {
    m_sProcessIdentifier = sProcessIdentifier;
  }

  @Transient
  public void setProcessIdentifier (@Nonnull final IProcessIdentifier aProcessID)
  {
    setProcessIdentifierScheme (aProcessID.getScheme ());
    setProcessIdentifier (aProcessID.getValue ());
  }

  @Transient
  @Nonnull
  public IParticipantIdentifier getAsBusinessIdentifier ()
  {
    return new SimpleParticipantIdentifier (m_sParticipantIdentifierScheme, m_sParticipantIdentifier);
  }

  @Transient
  @Nonnull
  public IDocumentTypeIdentifier getAsDocumentTypeIdentifier ()
  {
    return new SimpleDocumentTypeIdentifier (m_sDocumentTypeIdentifierScheme, m_sDocumentTypeIdentifier);
  }

  @Transient
  @Nonnull
  public IProcessIdentifier getAsProcessIdentifier ()
  {
    return new SimpleProcessIdentifier (m_sProcessIdentifierScheme, m_sProcessIdentifier);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final DBProcessID rhs = (DBProcessID) o;
    return EqualsHelper.equals (m_sParticipantIdentifierScheme, rhs.m_sParticipantIdentifierScheme) &&
           EqualsHelper.equals (m_sParticipantIdentifier, rhs.m_sParticipantIdentifier) &&
           EqualsHelper.equals (m_sDocumentTypeIdentifierScheme, rhs.m_sDocumentTypeIdentifierScheme) &&
           EqualsHelper.equals (m_sDocumentTypeIdentifier, rhs.m_sDocumentTypeIdentifier) &&
           EqualsHelper.equals (m_sProcessIdentifierScheme, rhs.m_sProcessIdentifierScheme) &&
           EqualsHelper.equals (m_sProcessIdentifier, rhs.m_sProcessIdentifier);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sParticipantIdentifierScheme)
                                       .append (m_sParticipantIdentifier)
                                       .append (m_sDocumentTypeIdentifierScheme)
                                       .append (m_sDocumentTypeIdentifier)
                                       .append (m_sProcessIdentifierScheme)
                                       .append (m_sProcessIdentifier)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("participantIDScheme", m_sParticipantIdentifierScheme)
                                       .append ("participantIDValue", m_sParticipantIdentifier)
                                       .append ("documentTypeIDScheme", m_sDocumentTypeIdentifierScheme)
                                       .append ("documentTypeIDValue", m_sDocumentTypeIdentifier)
                                       .append ("processIDScheme", m_sProcessIdentifierScheme)
                                       .append ("processIDValue", m_sProcessIdentifier)
                                       .getToString ();
  }
}
