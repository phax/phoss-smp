/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.totp;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;

/**
 * Base interface for a manager that handles {@link ISMPUserTotp} objects. Each ph-oton user can
 * have at most one TOTP enrollment, so the user ID is used as the ID of the contained objects.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
public interface ISMPUserTotpManager
{
  /**
   * Create a new, not yet confirmed TOTP enrollment for the provided user. An already existing
   * enrollment of that user - confirmed or not - is replaced.
   *
   * @param sUserID
   *        The ID of the user. May neither be <code>null</code> nor empty.
   * @param sSecret
   *        The Base32 encoded shared secret. May neither be <code>null</code> nor empty.
   * @return The created object. Never <code>null</code>.
   */
  @NonNull
  ISMPUserTotp createOrReplaceTotp (@NonNull @Nonempty String sUserID, @NonNull @Nonempty String sSecret);

  /**
   * Enable or disable an existing enrollment.
   *
   * @param sUserID
   *        The ID of the user. May be <code>null</code>.
   * @param bEnabled
   *        <code>true</code> to enable, <code>false</code> to disable.
   * @return {@link EChange#CHANGED} if something was changed.
   */
  @NonNull
  EChange setTotpEnabled (@Nullable String sUserID, boolean bEnabled);

  /**
   * Remember the last successfully used TOTP time slot of a user, to prevent a replay of the same
   * one-time password.
   *
   * @param sUserID
   *        The ID of the user. May be <code>null</code>.
   * @param nTimeSlot
   *        The time slot that was just used.
   * @return {@link EChange#CHANGED} if something was changed.
   */
  @NonNull
  EChange setTotpLastUsedTimeSlot (@Nullable String sUserID, long nTimeSlot);

  /**
   * Delete the enrollment of the provided user.
   *
   * @param sUserID
   *        The ID of the user. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if the removal was successful.
   */
  @NonNull
  EChange deleteTotp (@Nullable String sUserID);

  /**
   * Get the TOTP enrollment of the provided user.
   *
   * @param sUserID
   *        The ID of the user. May be <code>null</code>.
   * @return <code>null</code> if the user has no TOTP enrollment.
   */
  @Nullable
  ISMPUserTotp getTotpOfUserID (@Nullable String sUserID);

  /**
   * Check if the provided user has a <b>confirmed</b> TOTP enrollment, meaning that a TOTP code is
   * required to login.
   *
   * @param sUserID
   *        The ID of the user. May be <code>null</code>.
   * @return <code>true</code> if TOTP is enabled for that user.
   */
  default boolean isTotpEnabled (@Nullable final String sUserID)
  {
    final ISMPUserTotp aTotp = getTotpOfUserID (sUserID);
    return aTotp != null && aTotp.isEnabled ();
  }

  /**
   * @return A list of all contained TOTP enrollments. Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISMPUserTotp> getAllTotps ();
}
