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

import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;

/**
 * Contains the TOTP (Time-based One-Time Password) enrollment data of a single user. The ID of this
 * object is the ID of the ph-oton user it belongs to.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
public interface ISMPUserTotp extends IHasID <String>
{
  /**
   * The maximum length of a stored secret. Used to match the SQL column restrictions.
   */
  int SECRET_MAX_LENGTH = 128;

  /**
   * @return The Base32 encoded shared secret of this enrollment. Neither <code>null</code> nor
   *         empty.
   */
  @NonNull
  @Nonempty
  String getSecret ();

  /**
   * @return <code>true</code> if the enrollment was confirmed by the user and TOTP is therefore
   *         enforced on login, <code>false</code> if the enrollment is still pending.
   */
  boolean isEnabled ();

  /**
   * @return The date and time when this enrollment was created. Never <code>null</code>.
   */
  @NonNull
  LocalDateTime getRegistrationDateTime ();

  /**
   * The last successfully used TOTP time slot. It is stored to prevent a replay of the very same
   * one-time password within its validity window.
   *
   * @return The last used time slot or <code>null</code> if no code was used so far.
   */
  @Nullable
  Long getLastUsedTimeSlot ();

  /**
   * @return <code>true</code> if {@link #getLastUsedTimeSlot()} is not <code>null</code>.
   */
  default boolean hasLastUsedTimeSlot ()
  {
    return getLastUsedTimeSlot () != null;
  }
}
