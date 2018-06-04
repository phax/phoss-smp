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
import com.helger.pd.businesscard.generic.PDContact;
import com.helger.pd.businesscard.v1.PD1ContactType;

/**
 * A single business card contact.
 * <p>
 * The files in this package are licensed under Apache 2.0 license
 * </p>
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
  public PD1ContactType getAsJAXBObject ()
  {
    final PD1ContactType ret = new PD1ContactType ();
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

  @Nonnull
  public static SMPBusinessCardContact createFromGenericObject (@Nonnull final PDContact aEntity)
  {
    return new SMPBusinessCardContact (aEntity.getType (),
                                       aEntity.getName (),
                                       aEntity.getPhoneNumber (),
                                       aEntity.getEmail ());
  }
}
