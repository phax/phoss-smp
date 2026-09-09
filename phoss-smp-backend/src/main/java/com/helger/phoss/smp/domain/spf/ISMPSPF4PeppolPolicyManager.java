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
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Manager for {@link ISMPSPF4PeppolPolicy} objects.
 *
 * @author Steven Noels
 */
public interface ISMPSPF4PeppolPolicyManager
{
  /**
   * @return The callbacks for the SPF4Peppol policy manager. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableObject
  CallbackList <ISMPSPF4PeppolPolicyCallback> spfCallbacks ();

  /**
   * Create or update an SPF4Peppol policy for a participant.
   *
   * @param aParticipantID
   *        The participant ID the policy belongs to. May not be <code>null</code>.
   * @param aTerms
   *        The policy terms. May not be <code>null</code>.
   * @param aTTL
   *        The optional TTL in seconds. May be <code>null</code> for default.
   * @param sExplanation
   *        The optional explanation text (max 500 chars). May be <code>null</code>.
   * @return The new or updated policy. <code>null</code> if persistence failed.
   */
  @Nullable
  ISMPSPF4PeppolPolicy createOrUpdateSPFPolicy (@NonNull IParticipantIdentifier aParticipantID,
                                                @NonNull Collection <SPF4PeppolTerm> aTerms,
                                                @Nullable Integer aTTL,
                                                @Nullable String sExplanation);

  /**
   * Delete an SPF4Peppol policy.
   *
   * @param aPolicy
   *        The policy to delete. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if deletion was successful.
   */
  @NonNull
  EChange deleteSPFPolicy (@Nullable ISMPSPF4PeppolPolicy aPolicy);

  /**
   * @return All contained SPF4Peppol policies. Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISMPSPF4PeppolPolicy> getAllSPFPolicies ();

  /**
   * @return All contained SPF4Peppol policy IDs. Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsSet <String> getAllSPFPolicyIDs ();

  /**
   * Check if a policy exists for the given participant.
   *
   * @param aID
   *        The participant ID to check. May be <code>null</code>.
   * @return <code>true</code> if a policy exists, <code>false</code> otherwise.
   */
  boolean containsSPFPolicyOfID (@Nullable IParticipantIdentifier aID);

  /**
   * Get the policy for the given participant.
   *
   * @param aID
   *        The participant ID. May be <code>null</code>.
   * @return The policy or <code>null</code> if none exists.
   */
  @Nullable
  ISMPSPF4PeppolPolicy getSPFPolicyOfID (@Nullable IParticipantIdentifier aID);

  /**
   * @return The count of all contained policies. Always &ge; 0.
   */
  @Nonnegative
  long getSPFPolicyCount ();
}
