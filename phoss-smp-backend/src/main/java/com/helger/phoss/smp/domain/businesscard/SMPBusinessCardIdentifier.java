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
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.id.IHasID;
import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppol.businesscard.generic.PDIdentifier;
import com.helger.peppol.businesscard.v3.PD3APIHelper;
import com.helger.peppol.businesscard.v3.PD3IdentifierType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A single business card identifier.
 * <p>
 * The files in this package are licensed under Apache 2.0 license
 * </p>
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
  public PD3IdentifierType getAsJAXBObject ()
  {
    return PD3APIHelper.createIdentifier (m_sScheme, m_sValue);
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

  @Nonnull
  public static SMPBusinessCardIdentifier createFromGenericObject (@Nonnull final PDIdentifier aEntity)
  {
    return new SMPBusinessCardIdentifier (aEntity.getScheme (), aEntity.getValue ());
  }
}
