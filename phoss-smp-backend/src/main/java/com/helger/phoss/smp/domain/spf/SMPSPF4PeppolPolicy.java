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

import java.util.Collection;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.type.ObjectType;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Implementation of {@link ISMPSPF4PeppolPolicy}.
 *
 * @author Steven Noels
 */
public class SMPSPF4PeppolPolicy implements ISMPSPF4PeppolPolicy
{
  public static final ObjectType OT = new ObjectType ("smpspfpolicy");

  private final IParticipantIdentifier m_aParticipantID;
  private final ICommonsList <SPF4PeppolTerm> m_aTerms;
  private final Integer m_aTTL;
  private final String m_sExplanation;

  public SMPSPF4PeppolPolicy (@NonNull final IParticipantIdentifier aParticipantID,
                              @NonNull final Collection <SPF4PeppolTerm> aTerms,
                              @Nullable final Integer aTTL,
                              @Nullable final String sExplanation)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aTerms, "Terms");
    if (aTTL != null)
    {
      ValueEnforcer.isBetweenInclusive (aTTL.intValue (), "TTL", MIN_TTL, MAX_TTL);
    }
    if (sExplanation != null)
    {
      ValueEnforcer.isTrue (sExplanation.length () <= 500,
                            () -> "Explanation must be at most 500 characters, but is " + sExplanation.length ());
    }

    m_aParticipantID = aParticipantID;
    m_aTerms = new CommonsArrayList <> (aTerms);
    m_aTTL = aTTL;
    m_sExplanation = sExplanation;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_aParticipantID.getURIEncoded ();
  }

  @NonNull
  public IParticipantIdentifier getParticipantIdentifier ()
  {
    return m_aParticipantID;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <SPF4PeppolTerm> getAllTerms ()
  {
    return m_aTerms.getClone ();
  }

  @Nonnegative
  public int getTermCount ()
  {
    return m_aTerms.size ();
  }

  @Nullable
  public Integer getTTL ()
  {
    return m_aTTL;
  }

  @Nullable
  public String getExplanation ()
  {
    return m_sExplanation;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPSPF4PeppolPolicy rhs = (SMPSPF4PeppolPolicy) o;
    return m_aParticipantID.equals (rhs.m_aParticipantID) &&
           m_aTerms.equals (rhs.m_aTerms) &&
           EqualsHelper.equals (m_aTTL, rhs.m_aTTL) &&
           EqualsHelper.equals (m_sExplanation, rhs.m_sExplanation);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aParticipantID)
                                       .append (m_aTerms)
                                       .append (m_aTTL)
                                       .append (m_sExplanation)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ParticipantID", m_aParticipantID)
                                       .append ("Terms", m_aTerms)
                                       .appendIfNotNull ("TTL", m_aTTL)
                                       .appendIfNotNull ("Explanation", m_sExplanation)
                                       .getToString ();
  }
}
