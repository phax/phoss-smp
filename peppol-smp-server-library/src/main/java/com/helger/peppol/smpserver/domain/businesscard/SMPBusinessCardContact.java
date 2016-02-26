
package com.helger.peppol.smpserver.domain.businesscard;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.pd.businesscard.PDContactType;

/**
 * A single business card contact.
 *
 * @author Philip Helger
 */
@Immutable
public class SMPBusinessCardContact implements Serializable
{
  private final String m_sType;
  private final String m_sName;
  private final String m_sPhoneNumber;
  private final String m_sEmail;

  public SMPBusinessCardContact (@Nullable final String sType,
                                 @Nullable final String sName,
                                 @Nullable final String sPhoneNumber,
                                 @Nullable final String sEmail)
  {
    m_sType = sType;
    m_sName = sName;
    m_sPhoneNumber = sPhoneNumber;
    m_sEmail = sEmail;
  }

  /**
   * @return The contact type. May be <code>null</code>.
   */
  @Nullable
  public String getType ()
  {
    return m_sType;
  }

  public boolean hasType ()
  {
    return StringHelper.hasText (m_sType);
  }

  /**
   * @return The contact name. May be <code>null</code>.
   */
  @Nullable
  public String getName ()
  {
    return m_sName;
  }

  public boolean hasName ()
  {
    return StringHelper.hasText (m_sName);
  }

  /**
   * @return The contact phone number. May be <code>null</code>.
   */
  @Nullable
  public String getPhoneNumber ()
  {
    return m_sPhoneNumber;
  }

  public boolean hasPhoneNumber ()
  {
    return StringHelper.hasText (m_sPhoneNumber);
  }

  /**
   * @return The contact email address. May be <code>null</code>.
   */
  @Nullable
  public String getEmail ()
  {
    return m_sEmail;
  }

  public boolean hasEmail ()
  {
    return StringHelper.hasText (m_sEmail);
  }

  public boolean isAnyFieldSet ()
  {
    return hasType () || hasName () || hasPhoneNumber () || hasEmail ();
  }

  @Nonnull
  public PDContactType getAsJAXBObject ()
  {
    final PDContactType ret = new PDContactType ();
    ret.setType (m_sType);
    ret.setName (m_sName);
    ret.setPhoneNumber (m_sPhoneNumber);
    ret.setEmail (m_sEmail);
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPBusinessCardContact rhs = (SMPBusinessCardContact) o;
    return EqualsHelper.equals (m_sType, rhs.m_sType) &&
           EqualsHelper.equals (m_sName, rhs.m_sName) &&
           EqualsHelper.equals (m_sPhoneNumber, rhs.m_sPhoneNumber) &&
           EqualsHelper.equals (m_sEmail, rhs.m_sEmail);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sType)
                                       .append (m_sName)
                                       .append (m_sPhoneNumber)
                                       .append (m_sEmail)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("type", m_sType)
                                       .append ("name", m_sName)
                                       .append ("phoneNumber", m_sPhoneNumber)
                                       .append ("email", m_sEmail)
                                       .toString ();
  }
}
