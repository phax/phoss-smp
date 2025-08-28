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

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.id.IHasID;
import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppol.businesscard.generic.PDContact;
import com.helger.peppol.businesscard.v3.PD3ContactType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
    return StringHelper.isNotEmpty (m_sType);
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
    return StringHelper.isNotEmpty (m_sName);
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
    return StringHelper.isNotEmpty (m_sPhoneNumber);
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
    return StringHelper.isNotEmpty (m_sEmail);
  }

  public boolean isAnyFieldSet ()
  {
    return hasType () || hasName () || hasPhoneNumber () || hasEmail ();
  }

  @Nonnull
  public PD3ContactType getAsJAXBObject ()
  {
    final PD3ContactType ret = new PD3ContactType ();
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
                                       .append ("Type", m_sType)
                                       .append ("Name", m_sName)
                                       .append ("PhoneNumber", m_sPhoneNumber)
                                       .append ("Email", m_sEmail)
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
