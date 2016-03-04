
package com.helger.peppol.smpserver.domain.businesscard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.joda.time.LocalDate;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.pd.businesscard.PDBusinessEntityType;

@NotThreadSafe
public class SMPBusinessCardEntity implements Serializable
{
  private String m_sName;
  private String m_sCountryCode;
  private String m_sGeographicalInformation;
  private final List <SMPBusinessCardIdentifier> m_aIdentifiers = new ArrayList <> ();
  private final List <String> m_aWebsiteURIs = new ArrayList <> ();
  private final List <SMPBusinessCardContact> m_aContacts = new ArrayList <> ();
  private String m_sAdditionalInformation;
  private LocalDate m_aRegistrationDate;

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

  /**
   * Gets the value of the identifier property.
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot.
   * Therefore any modification you make to the returned list will be present
   * inside the JAXB object. This is why there is not a <CODE>set</CODE> method
   * for the identifier property.
   * <p>
   * For example, to add a new item, do as follows:
   *
   * <pre>
   * getIdentifier ().add (newItem);
   * </pre>
   * <p>
   * Objects of the following type(s) are allowed in the list
   * {@link SMPBusinessCardIdentifier }
   *
   * @return The identifier list. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject ("JAXB implementation style")
  public List <SMPBusinessCardIdentifier> getIdentifiers ()
  {
    return m_aIdentifiers;
  }

  public boolean hasIdentifiers ()
  {
    return !m_aIdentifiers.isEmpty ();
  }

  /**
   * Gets the value of the websiteURI property.
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot.
   * Therefore any modification you make to the returned list will be present
   * inside the JAXB object. This is why there is not a <CODE>set</CODE> method
   * for the websiteURI property.
   * <p>
   * For example, to add a new item, do as follows:
   *
   * <pre>
   * getWebsiteURI ().add (newItem);
   * </pre>
   * <p>
   * Objects of the following type(s) are allowed in the list {@link String }
   *
   * @return The website URI list. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject ("JAXB implementation style")
  public List <String> getWebsiteURIs ()
  {
    return m_aWebsiteURIs;
  }

  public boolean hasWebsiteURIs ()
  {
    return !m_aWebsiteURIs.isEmpty ();
  }

  /**
   * Gets the value of the contact property.
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot.
   * Therefore any modification you make to the returned list will be present
   * inside the JAXB object. This is why there is not a <CODE>set</CODE> method
   * for the contact property.
   * <p>
   * For example, to add a new item, do as follows:
   *
   * <pre>
   * getContact ().add (newItem);
   * </pre>
   * <p>
   * Objects of the following type(s) are allowed in the list
   * {@link SMPBusinessCardContact }
   *
   * @return The contact list. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject ("JAXB implementation style")
  public List <SMPBusinessCardContact> getContacts ()
  {
    return m_aContacts;
  }

  public boolean hasContacts ()
  {
    return !m_aContacts.isEmpty ();
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
  public PDBusinessEntityType getAsJAXBObject ()
  {
    final PDBusinessEntityType ret = new PDBusinessEntityType ();
    ret.setName (m_sName);
    ret.setCountryCode (m_sCountryCode);
    ret.setGeographicalInformation (m_sGeographicalInformation);
    for (final SMPBusinessCardIdentifier aItem : m_aIdentifiers)
      ret.addIdentifier (aItem.getAsJAXBObject ());
    for (final String sItem : m_aWebsiteURIs)
      ret.addWebsiteURI (sItem);
    for (final SMPBusinessCardContact aItem : m_aContacts)
      ret.addContact (aItem.getAsJAXBObject ());
    ret.setAdditionalInformation (m_sAdditionalInformation);
    ret.setRegistrationDate (m_aRegistrationDate);
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final SMPBusinessCardEntity rhs = (SMPBusinessCardEntity) o;
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
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName)
                                       .append (m_sCountryCode)
                                       .append (m_sGeographicalInformation)
                                       .append (m_aIdentifiers)
                                       .append (m_aWebsiteURIs)
                                       .append (m_aContacts)
                                       .append (m_sAdditionalInformation)
                                       .append (m_aRegistrationDate)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("name", m_sName)
                                       .append ("countryCode", m_sCountryCode)
                                       .append ("geographicalInformation", m_sGeographicalInformation)
                                       .append ("identifier", m_aIdentifiers)
                                       .append ("websiteURI", m_aWebsiteURIs)
                                       .append ("contact", m_aContacts)
                                       .append ("additionalInformation", m_sAdditionalInformation)
                                       .append ("registrationDate", m_aRegistrationDate)
                                       .toString ();
  }
}
