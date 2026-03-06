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
package com.helger.phoss.smp.domain.spf;

import java.io.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;

/**
 * A single SPF4Peppol policy term consisting of a qualifier (result) and mechanism (how to match).
 *
 * @author Steven Noels
 */
@MustImplementEqualsAndHashcode
public class SPF4PeppolTerm implements Serializable
{
  private final ESPF4PeppolQualifier m_eQualifier;
  private final ESPF4PeppolMechanism m_eMechanism;
  private final String m_sValue;

  /**
   * Constructor for terms that require a value (seatid, certfp, reference).
   *
   * @param eQualifier
   *        The qualifier (pass, fail, softfail, neutral). May not be <code>null</code>.
   * @param eMechanism
   *        The mechanism (seatid, certfp, reference). May not be <code>null</code>.
   * @param sValue
   *        The mechanism value. May not be <code>null</code> for mechanisms that require a value.
   */
  public SPF4PeppolTerm (@NonNull final ESPF4PeppolQualifier eQualifier,
                         @NonNull final ESPF4PeppolMechanism eMechanism,
                         @Nullable final String sValue)
  {
    ValueEnforcer.notNull (eQualifier, "Qualifier");
    ValueEnforcer.notNull (eMechanism, "Mechanism");
    if (eMechanism.requiresValue ())
      ValueEnforcer.notEmpty (sValue, "Value");

    m_eQualifier = eQualifier;
    m_eMechanism = eMechanism;
    m_sValue = sValue;
  }

  /**
   * @return The qualifier determining the result when this mechanism matches. Never
   *         <code>null</code>.
   */
  @NonNull
  public ESPF4PeppolQualifier getQualifier ()
  {
    return m_eQualifier;
  }

  /**
   * @return The mechanism used to identify matching Access Points. Never <code>null</code>.
   */
  @NonNull
  public ESPF4PeppolMechanism getMechanism ()
  {
    return m_eMechanism;
  }

  /**
   * @return The mechanism-specific value. May be <code>null</code> for mechanisms that don't
   *         require a value (smp, all).
   */
  @Nullable
  public String getValue ()
  {
    return m_sValue;
  }

  /**
   * @return <code>true</code> if this term has a value, <code>false</code> otherwise.
   */
  public boolean hasValue ()
  {
    return m_sValue != null;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SPF4PeppolTerm rhs = (SPF4PeppolTerm) o;
    return m_eQualifier.equals (rhs.m_eQualifier) &&
           m_eMechanism.equals (rhs.m_eMechanism) &&
           EqualsHelper.equals (m_sValue, rhs.m_sValue);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_eQualifier)
                                       .append (m_eMechanism)
                                       .append (m_sValue)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Qualifier", m_eQualifier)
                                       .append ("Mechanism", m_eMechanism)
                                       .appendIfNotNull ("Value", m_sValue)
                                       .getToString ();
  }

  /**
   * Factory method for creating a term without a value (for smp and all mechanisms).
   *
   * @param eQualifier
   *        The qualifier. May not be <code>null</code>.
   * @param eMechanism
   *        The mechanism. May not be <code>null</code>.
   * @return New term instance. Never <code>null</code>.
   */
  @NonNull
  public static SPF4PeppolTerm createWithoutValue (@NonNull final ESPF4PeppolQualifier eQualifier,
                                                   @NonNull final ESPF4PeppolMechanism eMechanism)
  {
    return new SPF4PeppolTerm (eQualifier, eMechanism, null);
  }

  /**
   * Factory method for creating a pass:seatid term.
   *
   * @param sSeatID
   *        The Seat ID value. May not be <code>null</code> or empty.
   * @return New term instance. Never <code>null</code>.
   */
  @NonNull
  public static SPF4PeppolTerm createPassSeatID (@NonNull final String sSeatID)
  {
    return new SPF4PeppolTerm (ESPF4PeppolQualifier.PASS, ESPF4PeppolMechanism.SEATID, sSeatID);
  }

  /**
   * Factory method for creating a fail:all term (deny all others).
   *
   * @return New term instance. Never <code>null</code>.
   */
  @NonNull
  public static SPF4PeppolTerm createFailAll ()
  {
    return createWithoutValue (ESPF4PeppolQualifier.FAIL, ESPF4PeppolMechanism.ALL);
  }

  /**
   * Factory method for creating a softfail:all term (monitoring mode).
   *
   * @return New term instance. Never <code>null</code>.
   */
  @NonNull
  public static SPF4PeppolTerm createSoftfailAll ()
  {
    return createWithoutValue (ESPF4PeppolQualifier.SOFTFAIL, ESPF4PeppolMechanism.ALL);
  }
}
