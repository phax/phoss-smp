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

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.db.jpa.annotation.UsedOnlyByJPA;
import com.helger.peppol.identifier.CIdentifier;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;

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

  @Column (name = "businessIdentifierScheme", nullable = false, length = CIdentifier.MAX_IDENTIFIER_SCHEME_LENGTH)
  public String getBusinessIdentifierScheme ()
  {
    return m_sParticipantIdentifierScheme;
  }

  public void setBusinessIdentifierScheme (final String sBusinessIdentifierScheme)
  {
    m_sParticipantIdentifierScheme = DBHelper.getUnifiedParticipantDBValue (sBusinessIdentifierScheme);
  }

  @Column (name = "businessIdentifier", nullable = false, length = CIdentifier.MAX_PARTICIPANT_IDENTIFIER_VALUE_LENGTH)
  public String getBusinessIdentifier ()
  {
    return m_sParticipantIdentifier;
  }

  public void setBusinessIdentifier (final String sBusinessIdentifier)
  {
    m_sParticipantIdentifier = DBHelper.getUnifiedParticipantDBValue (sBusinessIdentifier);
  }

  @Transient
  @Nonnull
  public SimpleParticipantIdentifier getAsBusinessIdentifier ()
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
                                       .toString ();
  }
}
