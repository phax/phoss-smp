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
                                       .getToString ();
  }
}
