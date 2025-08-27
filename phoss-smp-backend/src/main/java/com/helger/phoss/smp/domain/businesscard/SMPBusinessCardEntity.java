/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.businesscard;

import java.io.Serializable;
import java.time.LocalDate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.id.IHasID;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.json.IJsonArray;
import com.helger.json.JsonArray;
import com.helger.peppol.businesscard.generic.PDBusinessEntity;
import com.helger.peppol.businesscard.generic.PDContact;
import com.helger.peppol.businesscard.generic.PDIdentifier;
import com.helger.peppol.businesscard.generic.PDName;
import com.helger.peppol.businesscard.v3.PD3BusinessEntityType;

/**
 * This class represents a single Business Card entity - a part of a Business
 * card.
 * <p>
 * The files in this package are licensed under Apache 2.0 license
 * </p>
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPBusinessCardEntity implements IHasID <String>, Serializable
{
  private final String m_sID;
  private final ICommonsList <SMPBusinessCardName> m_aNames = new CommonsArrayList <> ();
  private String m_sCountryCode;
  private String m_sGeographicalInformation;
  private final ICommonsList <SMPBusinessCardIdentifier> m_aIdentifiers = new CommonsArrayList <> ();
  private final ICommonsList <String> m_aWebsiteURIs = new CommonsArrayList <> ();
  private final ICommonsList <SMPBusinessCardContact> m_aContacts = new CommonsArrayList <> ();
  private String m_sAdditionalInformation;
  private LocalDate m_aRegistrationDate;

  /**
   * Create a new instance with a unique ID.
   */
  public SMPBusinessCardEntity ()
  {
    this (GlobalIDFactory.getNewPersistentStringID ());
  }

  /**
   * Create an instance with an existing ID. Only when editing!
   *
   * @param sID
   *        The ID of the object. May neither be <code>null</code> nor empty.
   */
  public SMPBusinessCardEntity (@Nonnull @Nonempty final String sID)
  {
    m_sID = ValueEnforcer.notEmpty (sID, "ID");
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return All names of the entity. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <SMPBusinessCardName> names ()
  {
    return m_aNames;
  }

  /**
   * @return <code>true</code> if this entity has exactly one name without a
   *         specific language code.
   * @since 5.7.0
   */
  public final boolean isSingleNameWithoutLanguage ()
  {
    return m_aNames.size () == 1 && m_aNames.getFirstOrNull ().hasNoLanguageCode ();
  }

  /**
   * @return All names as a single JSON array.
   * @since 5.7.0
   * @see SMPBusinessCardName#getAsJson()
   */
  @Nonnull
  public final IJsonArray getNamesAsJson ()
  {
    return new JsonArray ().addAllMapped (m_aNames, SMPBusinessCardName::getAsJson);
  }

  /**
   * @return The country code. May be <code>null</code>.
   */
  @Nullable
  public String getCountryCode ()
  {
    return m_sCountryCode;
  }

  /**
   * Sets the value of the countryCode property.
   *
   * @param value
   *        The country code to set. May be <code>null</code>.
   */
  public void setCountryCode (@Nullable final String value)
  {
    m_sCountryCode = value;
  }

  /**
   * Gets the value of the geographicalInformation property.
   *
   * @return The geographic information. May be <code>null</code>.
   */
  @Nullable
  public String getGeographicalInformation ()
  {
    return m_sGeographicalInformation;
  }

  /**
   * @return The geographic information. May be <code>null</code>.
   */
  public boolean hasGeographicalInformation ()
  {
    return StringHelper.isNotEmpty (m_sGeographicalInformation);
  }

  /**
   * Sets the value of the geographicalInformation property.
   *
   * @param value
   *        he geographic information to set. May be <code>null</code>.
   */
  public void setGeographicalInformation (@Nullable final String value)
  {
    m_sGeographicalInformation = value;
  }

  @Nonnull
  @ReturnsMutableObject
  public ICommonsList <SMPBusinessCardIdentifier> identifiers ()
  {
    return m_aIdentifiers;
  }

  @Nonnull
  @ReturnsMutableObject
  public ICommonsList <String> websiteURIs ()
  {
    return m_aWebsiteURIs;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <SMPBusinessCardContact> contacts ()
  {
    return m_aContacts;
  }

  /**
   * Gets the value of the additionalInformation property.
   *
   * @return The additional information. May be <code>null</code>.
   */
  @Nullable
  public String getAdditionalInformation ()
  {
    return m_sAdditionalInformation;
  }

  public boolean hasAdditionalInformation ()
  {
    return StringHelper.isNotEmpty (m_sAdditionalInformation);
  }

  /**
   * Sets the value of the additionalInformation property.
   *
   * @param sValue
   *        The additional information to set. May be <code>null</code>.
   */
  public void setAdditionalInformation (@Nullable final String sValue)
  {
    m_sAdditionalInformation = sValue;
  }

  /**
   * Gets the value of the registrationDate property.
   *
   * @return The registration date of the participant. May be <code>null</code>.
   */
  @Nullable
  public LocalDate getRegistrationDate ()
  {
    return m_aRegistrationDate;
  }

  public boolean hasRegistrationDate ()
  {
    return m_aRegistrationDate != null;
  }

  /**
   * Sets the value of the registrationDate property.
   *
   * @param value
   *        The registration date of the participant. May be <code>null</code>.
   */
  public void setRegistrationDate (@Nullable final LocalDate value)
  {
    m_aRegistrationDate = value;
  }

  @Nonnull
  public PD3BusinessEntityType getAsJAXBObject ()
  {
    final PD3BusinessEntityType ret = new PD3BusinessEntityType ();
    for (final SMPBusinessCardName aItem : m_aNames)
      ret.addName (aItem.getAsJAXBObject ());
    ret.setCountryCode (m_sCountryCode);
    if (hasGeographicalInformation ())
      ret.setGeographicalInformation (m_sGeographicalInformation);
    for (final SMPBusinessCardIdentifier aItem : m_aIdentifiers)
      ret.addIdentifier (aItem.getAsJAXBObject ());
    for (final String sItem : m_aWebsiteURIs)
      ret.addWebsiteURI (sItem);
    for (final SMPBusinessCardContact aItem : m_aContacts)
      ret.addContact (aItem.getAsJAXBObject ());
    if (hasAdditionalInformation ())
      ret.setAdditionalInformation (m_sAdditionalInformation);
    ret.setRegistrationDate (m_aRegistrationDate);
    return ret;
  }

  public boolean isEqualContent (@Nullable final SMPBusinessCardEntity rhs)
  {
    if (rhs == null)
      return false;
    return m_aNames.equals (rhs.m_aNames) &&
           EqualsHelper.equals (m_sCountryCode, rhs.m_sCountryCode) &&
           EqualsHelper.equals (m_sGeographicalInformation, rhs.m_sGeographicalInformation) &&
           m_aIdentifiers.equals (rhs.m_aIdentifiers) &&
           m_aWebsiteURIs.equals (rhs.m_aWebsiteURIs) &&
           m_aContacts.equals (rhs.m_aContacts) &&
           EqualsHelper.equals (m_sAdditionalInformation, rhs.m_sAdditionalInformation) &&
           EqualsHelper.equals (m_aRegistrationDate, rhs.m_aRegistrationDate);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPBusinessCardEntity rhs = (SMPBusinessCardEntity) o;
    return m_sID.equals (rhs.m_sID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", m_sID)
                                       .append ("Names", m_aNames)
                                       .append ("CountryCode", m_sCountryCode)
                                       .append ("GeographicalInformation", m_sGeographicalInformation)
                                       .append ("Identifier", m_aIdentifiers)
                                       .append ("WebsiteURI", m_aWebsiteURIs)
                                       .append ("Contact", m_aContacts)
                                       .append ("AdditionalInformation", m_sAdditionalInformation)
                                       .append ("RegistrationDate", m_aRegistrationDate)
                                       .getToString ();
  }

  @Nonnull
  public static SMPBusinessCardEntity createFromGenericObject (@Nonnull final PDBusinessEntity aEntity)
  {
    ValueEnforcer.notNull (aEntity, "Entity");
    final SMPBusinessCardEntity ret = new SMPBusinessCardEntity ();
    for (final PDName aItem : aEntity.names ())
      ret.names ().add (new SMPBusinessCardName (aItem.getName (), aItem.getLanguageCode ()));
    ret.setCountryCode (aEntity.getCountryCode ());
    ret.setGeographicalInformation (aEntity.getGeoInfo ());
    for (final PDIdentifier aItem : aEntity.identifiers ())
      ret.identifiers ().add (SMPBusinessCardIdentifier.createFromGenericObject (aItem));
    for (final String sItem : aEntity.websiteURIs ())
      ret.websiteURIs ().add (sItem);
    for (final PDContact aItem : aEntity.contacts ())
      ret.contacts ().add (SMPBusinessCardContact.createFromGenericObject (aItem));
    ret.setAdditionalInformation (aEntity.getAdditionalInfo ());
    ret.setRegistrationDate (aEntity.getRegistrationDate ());
    return ret;
  }
}
