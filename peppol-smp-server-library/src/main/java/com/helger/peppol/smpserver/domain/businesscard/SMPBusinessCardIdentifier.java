
package com.helger.peppol.smpserver.domain.businesscard;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.id.IHasID;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.string.ToStringGenerator;
import com.helger.pd.businesscard.PDIdentifierType;

/**
 * A single business card identifier.
 *
 * @author Philip Helger
 */
@Immutable
public class SMPBusinessCardIdentifier implements IHasID <String>, Serializable
{
  private final String m_sID;
  private final String m_sScheme;
  private final String m_sValue;

  public SMPBusinessCardIdentifier (@Nonnull @Nonempty final String sScheme, @Nonnull @Nonempty final String sValue)
  {
    this (GlobalIDFactory.getNewPersistentStringID (), sScheme, sValue);
  }

  public SMPBusinessCardIdentifier (@Nonnull @Nonempty final String sID,
                                    @Nonnull @Nonempty final String sScheme,
                                    @Nonnull @Nonempty final String sValue)
  {
    m_sID = ValueEnforcer.notEmpty (sID, "ID");
    m_sScheme = ValueEnforcer.notEmpty (sScheme, "Scheme");
    m_sValue = ValueEnforcer.notEmpty (sValue, "Value");
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * Gets the value of the scheme property.
   *
   * @return the identifier scheme. Neither <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getScheme ()
  {
    return m_sScheme;
  }

  /**
   * Gets the value of the value property.
   *
   * @return The identifier value. Neither <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getValue ()
  {
    return m_sValue;
  }

  @Nonnull
  public PDIdentifierType getAsJAXBObject ()
  {
    return getAsJAXBObject (m_sScheme, m_sValue);
  }

  @Nonnull
  public static PDIdentifierType getAsJAXBObject (@Nonnull @Nonempty final String sScheme,
                                                  @Nonnull @Nonempty final String sValue)
  {
    final PDIdentifierType ret = new PDIdentifierType ();
    ret.setScheme (sScheme);
    ret.setValue (sValue);
    return ret;
  }

  public boolean isEqualContent (@Nullable final SMPBusinessCardIdentifier rhs)
  {
    if (rhs == null)
      return false;
    return m_sScheme.equals (rhs.m_sScheme) && m_sValue.equals (rhs.m_sValue);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPBusinessCardIdentifier rhs = (SMPBusinessCardIdentifier) o;
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
                                       .append ("value", m_sValue)
                                       .append ("scheme", m_sScheme)
                                       .toString ();
  }
}
