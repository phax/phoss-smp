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
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.id.IHasID;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.pd.businesscard.PDContactType;

/**
 * A single business card contact.
 *
 * @author Philip Helger
 */
@Immutable
public class SMPBusinessCardContact implements IHasID <String>, Serializable
{
  private final String m_sID;
  private final String m_sType;
  private final String m_sName;
  private final String m_sPhoneNumber;
  private final String m_sEmail;

  public SMPBusinessCardContact (@Nullable final String sType,
                                 @Nullable final String sName,
                                 @Nullable final String sPhoneNumber,
                                 @Nullable final String sEmail)
  {
    this (GlobalIDFactory.getNewPersistentStringID (), sType, sName, sPhoneNumber, sEmail);
  }

  public SMPBusinessCardContact (@Nonnull @Nonempty final String sID,
                                 @Nullable final String sType,
                                 @Nullable final String sName,
                                 @Nullable final String sPhoneNumber,
                                 @Nullable final String sEmail)
  {
    m_sID = ValueEnforcer.notEmpty (sID, "ID");
    m_sType = sType;
    m_sName = sName;
    m_sPhoneNumber = sPhoneNumber;
    m_sEmail = sEmail;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
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

  public boolean isEqualContent (@Nullable final SMPBusinessCardContact rhs)
  {
    if (rhs == null)
      return false;
    return EqualsHelper.equals (m_sType, rhs.m_sType) &&
           EqualsHelper.equals (m_sName, rhs.m_sName) &&
           EqualsHelper.equals (m_sPhoneNumber, rhs.m_sPhoneNumber) &&
           EqualsHelper.equals (m_sEmail, rhs.m_sEmail);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPBusinessCardContact rhs = (SMPBusinessCardContact) o;
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
                                       .append ("type", m_sType)
                                       .append ("name", m_sName)
                                       .append ("phoneNumber", m_sPhoneNumber)
                                       .append ("email", m_sEmail)
                                       .getToString ();
  }
}
