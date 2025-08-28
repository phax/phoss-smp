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
package com.helger.phoss.smp.domain.pmigration;

import java.time.LocalDateTime;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.peppolid.IParticipantIdentifier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Defines the details of a single participant migration
 *
 * @author Philip Helger
 * @since 5.4.0
 */
public interface ISMPParticipantMigration extends IHasID <String>
{
  /**
   * @return Outbound or inbound migration? Never <code>null</code>.
   */
  @Nonnull
  EParticipantMigrationDirection getDirection ();

  /**
   * @return The migration state of this participant. Never <code>null</code>.
   */
  @Nonnull
  EParticipantMigrationState getState ();

  /**
   * Check if the current states matches the provided state.
   *
   * @param eState
   *        The state to compare to. May be <code>null</code> which matches all
   *        states.
   * @return <code>true</code> if it matches, <code>false</code> if not.
   */
  default boolean isMatchingState (@Nullable final EParticipantMigrationState eState)
  {
    return eState == null || eState.equals (getState ());
  }

  /**
   * @return The participant identifier that is going to be migrated away. Never
   *         <code>null</code>.
   */
  @Nonnull
  IParticipantIdentifier getParticipantIdentifier ();

  /**
   * @return The date and time, when the migration was initiated. This is
   *         relevant, as the migration key is only valid for some time. Never
   *         <code>null</code>.
   */
  @Nonnull
  LocalDateTime getInitiationDateTime ();

  /**
   * @return The created migration key. Neither <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  String getMigrationKey ();
}
