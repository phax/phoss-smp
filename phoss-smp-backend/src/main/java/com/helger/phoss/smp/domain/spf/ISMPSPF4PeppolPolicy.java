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

import java.util.Comparator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.id.IHasID;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Interface representing an SPF4Peppol policy for a participant.
 * <p>
 * SPF4Peppol allows participants to declare which Access Points are authorized to send documents on
 * their behalf, similar to email SPF (RFC 7208).
 *
 * @author Steven Noels
 */
@MustImplementEqualsAndHashcode
public interface ISMPSPF4PeppolPolicy extends IHasID <String>
{
  /** The current SPF4Peppol specification version */
  String VERSION = "1.0";

  /** Default TTL in seconds (1 hour) */
  int DEFAULT_TTL = 3600;

  /** Minimum TTL in seconds (1 minute) */
  int MIN_TTL = 60;

  /** Maximum TTL in seconds (24 hours) */
  int MAX_TTL = 86400;

  /**
   * @return The participant identifier this policy belongs to. Never <code>null</code>.
   */
  @NonNull
  IParticipantIdentifier getParticipantIdentifier ();

  /**
   * @return A copy of all policy terms. Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <SPF4PeppolTerm> getAllTerms ();

  /**
   * @return The number of terms in this policy. Always &ge; 0.
   */
  @Nonnegative
  int getTermCount ();

  /**
   * @return The TTL (time-to-live) in seconds for caching this policy. May be <code>null</code> to
   *         use the default (3600 seconds).
   */
  @Nullable
  Integer getTTL ();

  /**
   * @return The effective TTL in seconds, using the default if not explicitly set.
   */
  default int getEffectiveTTL ()
  {
    final Integer aTTL = getTTL ();
    return aTTL != null ? aTTL.intValue () : DEFAULT_TTL;
  }

  /**
   * @return An optional human-readable explanation text (max 500 chars). May be <code>null</code>.
   */
  @Nullable
  String getExplanation ();

  /**
   * @return <code>true</code> if an explanation is present, <code>false</code> otherwise.
   */
  default boolean hasExplanation ()
  {
    return getExplanation () != null;
  }

  @NonNull
  static Comparator <ISMPSPF4PeppolPolicy> comparator ()
  {
    return Comparator.comparing (ISMPSPF4PeppolPolicy::getID);
  }
}
