
package com.helger.peppol.smpserver.domain.businesscard;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.pd.businesscard.PDIdentifierType;

/**
 * A single business card identifier.
 *
 * @author Philip Helger
 */
@Immutable
public class SMPBusinessCardIdentifier implements Serializable
{
  private final String m_sScheme;
  private final String m_sValue;

  public SMPBusinessCardIdentifier (@Nonnull @Nonempty final String sScheme, @Nonnull @Nonempty final String sValue)
  {
    m_sScheme = ValueEnforcer.notEmpty (sScheme, "Scheme");
    m_sValue = ValueEnforcer.notEmpty (sValue, "Value");
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

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPBusinessCardIdentifier rhs = (SMPBusinessCardIdentifier) o;
    return m_sScheme.equals (rhs.m_sScheme) && m_sValue.equals (rhs.m_sValue);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sValue).append (m_sScheme).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("value", m_sValue).append ("scheme", m_sScheme).toString ();
  }
}
