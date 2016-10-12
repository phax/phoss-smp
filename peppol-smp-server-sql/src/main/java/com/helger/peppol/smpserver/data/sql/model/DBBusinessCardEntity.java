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
import java.time.LocalDate;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

import com.helger.db.jpa.annotation.UsedOnlyByJPA;
import com.helger.db.jpa.eclipselink.converter.JPALocalDateConverter;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;

/**
 * DB Business Card Entity representation
 *
 * @author Philip Helger
 */
@Entity
@Table (name = "smp_bce")
@Converter (name = "localdate", converterClass = JPALocalDateConverter.class)
public class DBBusinessCardEntity implements Serializable
{
  private String m_sID;
  private String m_sParticipantID;
  private String m_sName;
  private String m_sCountryCode;
  private String m_sGeographicalInformation;
  private String m_sIdentifiers;
  private String m_sWebsiteURIs;
  private String m_sContacts;
  private String m_sAdditionalInformation;
  private LocalDate m_aRegistrationDate;

  @Deprecated
  @UsedOnlyByJPA
  public DBBusinessCardEntity ()
  {}

  public DBBusinessCardEntity (final String sID,
                               final String sParticipantID,
                               final String sName,
                               final String sCountryCode,
                               final String sGeographicalInformation,
                               final String sIdentifiers,
                               final String sWebsiteURIs,
                               final String sContacts,
                               final String sAdditionalnformation,
                               final LocalDate aRegistrationDate)
  {
    m_sID = sID;
    setParticipantId (sParticipantID);
    setName (sName);
    setCountryCode (sCountryCode);
    setGeographicalInformation (sGeographicalInformation);
    setIdentifiers (sIdentifiers);
    setWebsiteURIs (sWebsiteURIs);
    setContacts (sContacts);
    setAdditionalInformation (sAdditionalnformation);
    setRegistrationDate (aRegistrationDate);
  }

  @Id
  @Column (name = "id", nullable = false)
  public String getId ()
  {
    return m_sID;
  }

  public void setId (final String sID)
  {
    m_sID = sID;
  }

  @Column (name = "pid", nullable = false, length = 256)
  public String getParticipantId ()
  {
    return m_sParticipantID;
  }

  public void setParticipantId (final String sParticipantID)
  {
    m_sParticipantID = DBHelper.getUnifiedParticipantDBValue (sParticipantID);
  }

  @Nonnull
  @Transient
  public SimpleParticipantIdentifier getAsBusinessIdentifier ()
  {
    return SimpleParticipantIdentifier.createFromURIPart (m_sParticipantID);
  }

  @Column (name = "name", nullable = false)
  public String getName ()
  {
    return m_sName;
  }

  public void setName (final String sName)
  {
    m_sName = sName;
  }

  @Column (name = "country", nullable = false, length = 3)
  public String getCountryCode ()
  {
    return m_sCountryCode;
  }

  public void setCountryCode (final String sCountryCode)
  {
    m_sCountryCode = sCountryCode;
  }

  @Column (name = "geoinfo", nullable = true)
  public String getGeographicalInformation ()
  {
    return m_sGeographicalInformation;
  }

  public void setGeographicalInformation (final String sGeographicalInformation)
  {
    m_sGeographicalInformation = sGeographicalInformation;
  }

  @Column (name = "identifiers", nullable = true)
  public String getIdentifiers ()
  {
    return m_sIdentifiers;
  }

  public void setIdentifiers (final String sIdentifiers)
  {
    m_sIdentifiers = sIdentifiers;
  }

  @Column (name = "websites", nullable = true)
  public String getWebsiteURIs ()
  {
    return m_sWebsiteURIs;
  }

  public void setWebsiteURIs (final String sWebsiteURIs)
  {
    m_sWebsiteURIs = sWebsiteURIs;
  }

  @Column (name = "contacts", nullable = true)
  public String getContacts ()
  {
    return m_sContacts;
  }

  public void setContacts (final String sContacts)
  {
    m_sContacts = sContacts;
  }

  @Column (name = "addon", nullable = true)
  public String getAdditionalInformation ()
  {
    return m_sAdditionalInformation;
  }

  public void setAdditionalInformation (final String sAdditionalInformation)
  {
    m_sAdditionalInformation = sAdditionalInformation;
  }

  @Column (name = "regdate", nullable = true)
  @Convert ("localdate")
  public LocalDate getRegistrationDate ()
  {
    return m_aRegistrationDate;
  }

  public void setRegistrationDate (final LocalDate aRegistrationDate)
  {
    m_aRegistrationDate = aRegistrationDate;
  }
}
