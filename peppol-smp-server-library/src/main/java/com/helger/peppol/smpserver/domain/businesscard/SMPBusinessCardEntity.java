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
package com.helger.peppol.smpserver.domain.businesscard;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.id.IHasID;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.pd.businesscard.generic.PDBusinessEntity;
import com.helger.pd.businesscard.generic.PDContact;
import com.helger.pd.businesscard.generic.PDIdentifier;
import com.helger.pd.businesscard.v1.PD1BusinessEntityType;

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
  private String m_sName;
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
   * @return The entity name. May be <code>null</code>.
   */
  @Nullable
  public String getName ()
  {
    return m_sName;
  }

  /**
   * Sets the value of the name property.
   *
   * @param value
   *        name of the entity
   */
  public void setName (@Nullable final String value)
  {
    m_sName = value;
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
    return StringHelper.hasText (m_sGeographicalInformation);
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
  @ReturnsMutableCopy
  public ICommonsList <SMPBusinessCardIdentifier> getIdentifiers ()
  {
    return m_aIdentifiers.getClone ();
  }

  public boolean hasIdentifiers ()
  {
    return m_aIdentifiers.isNotEmpty ();
  }

  public void addIdentifier (@Nonnull final SMPBusinessCardIdentifier aIdentifier)
  {
    ValueEnforcer.notNull (aIdentifier, "Identifier");
    m_aIdentifiers.add (aIdentifier);
  }

  public void setIdentifiers (@Nonnull final List <SMPBusinessCardIdentifier> aIdentifiers)
  {
    ValueEnforcer.notNull (aIdentifiers, "Identifiers");
    m_aIdentifiers.setAll (aIdentifiers);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getAllWebsiteURIs ()
  {
    return m_aWebsiteURIs.getClone ();
  }

  public boolean hasWebsiteURIs ()
  {
    return m_aWebsiteURIs.isNotEmpty ();
  }

  public void addWebsiteURI (@Nonnull final String sWebsiteURI)
  {
    ValueEnforcer.notNull (sWebsiteURI, "WebsiteURI");
    m_aWebsiteURIs.add (sWebsiteURI);
  }

  public void setWebsiteURIs (@Nonnull final List <String> aWebsiteURIs)
  {
    ValueEnforcer.notNull (aWebsiteURIs, "WebsiteURIs");
    m_aWebsiteURIs.setAll (aWebsiteURIs);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <SMPBusinessCardContact> getContacts ()
  {
    return m_aContacts.getClone ();
  }

  public boolean hasContacts ()
  {
    return m_aContacts.isNotEmpty ();
  }

  public void addContact (@Nonnull final SMPBusinessCardContact aContact)
  {
    ValueEnforcer.notNull (aContact, "Contact");
    m_aContacts.add (aContact);
  }

  public void setContacts (@Nonnull final List <SMPBusinessCardContact> aContacts)
  {
    ValueEnforcer.notNull (aContacts, "Contacts");
    m_aContacts.setAll (aContacts);
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
    return StringHelper.hasText (m_sAdditionalInformation);
  }

  /**
   * Sets the value of the additionalInformation property.
   *
   * @param value
   *        The additional information to set. May be <code>null</code>.
   */
  public void setAdditionalInformation (@Nullable final String value)
  {
    m_sAdditionalInformation = value;
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
  public PD1BusinessEntityType getAsJAXBObject ()
  {
    final PD1BusinessEntityType ret = new PD1BusinessEntityType ();
    ret.setName (m_sName);
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
    return EqualsHelper.equals (m_sName, rhs.m_sName) &&
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
                                       .append ("name", m_sName)
                                       .append ("countryCode", m_sCountryCode)
                                       .append ("geographicalInformation", m_sGeographicalInformation)
                                       .append ("identifier", m_aIdentifiers)
                                       .append ("websiteURI", m_aWebsiteURIs)
                                       .append ("contact", m_aContacts)
                                       .append ("additionalInformation", m_sAdditionalInformation)
                                       .append ("registrationDate", m_aRegistrationDate)
                                       .getToString ();
  }

  @Nonnull
  public static SMPBusinessCardEntity createFromGenericObject (final PDBusinessEntity aEntity)
  {
    final SMPBusinessCardEntity ret = new SMPBusinessCardEntity ();
    ret.setName (aEntity.getName ());
    ret.setCountryCode (aEntity.getCountryCode ());
    ret.setGeographicalInformation (aEntity.getGeoInfo ());
    for (final PDIdentifier aItem : aEntity.identifiers ())
      ret.addIdentifier (SMPBusinessCardIdentifier.createFromGenericObject (aItem));
    for (final String sItem : aEntity.websiteURIs ())
      ret.addWebsiteURI (sItem);
    for (final PDContact aItem : aEntity.contacts ())
      ret.addContact (SMPBusinessCardContact.createFromGenericObject (aItem));
    ret.setAdditionalInformation (aEntity.getAdditionalInfo ());
    ret.setRegistrationDate (aEntity.getRegistrationDate ());
    return ret;
  }
}
