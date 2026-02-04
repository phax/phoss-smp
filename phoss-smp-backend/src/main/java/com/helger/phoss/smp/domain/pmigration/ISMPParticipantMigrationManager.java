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
package com.helger.phoss.smp.domain.pmigration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * This is the interface for managing participant migrations.
 *
 * @author Philip Helger
 * @since 5.4.0
 */
public interface ISMPParticipantMigrationManager
{
  /**
   * Create a new outbound participant migration for the provided participant
   * identifier. This means the participant is migrated FROM this SMP to another
   * SMP.
   *
   * @param aParticipantID
   *        The participant ID to use. May not be <code>null</code>.
   * @param sMigrationKey
   *        The migration key send to the SMK/SML. May neither be
   *        <code>null</code> nor empty.
   * @return The created migration domain object. May be <code>null</code> in
   *         case persistence failed.
   */
  @Nullable
  ISMPParticipantMigration createOutboundParticipantMigration (@NonNull IParticipantIdentifier aParticipantID,
                                                               @NonNull @Nonempty String sMigrationKey);

  /**
   * Create a new inbound participant migration for the provided participant
   * identifier. This means, the participant is migrated from another SMP TO
   * this SMP.
   *
   * @param aParticipantID
   *        The participant ID to use. May not be <code>null</code>.
   * @param sMigrationKey
   *        The migration key received from the other SMP. May neither be
   *        <code>null</code> nor empty.
   * @return The created migration domain object. May be <code>null</code> in
   *         case persistence failed.
   */
  @Nullable
  ISMPParticipantMigration createInboundParticipantMigration (@NonNull IParticipantIdentifier aParticipantID,
                                                              @NonNull @Nonempty String sMigrationKey);

  /**
   * Delete the participant migration with the provided ID.
   *
   * @param sParticipantMigrationID
   *        The ID to be deleted. May be <code>null</code>.
   * @return {@link EChange} and never <code>null</code>.
   */
  @NonNull
  EChange deleteParticipantMigrationOfID (@Nullable String sParticipantMigrationID);

  /**
   * Delete all participant migrations of the provided participant identifier.
   *
   * @param aParticipantID
   *        The participant identifier to delete. May not be <code>null</code>.
   * @return {@link EChange} and never <code>null</code>.
   */
  @NonNull
  EChange deleteAllParticipantMigrationsOfParticipant (@NonNull IParticipantIdentifier aParticipantID);

  /**
   * Change the participant migration state of the provided participant ID.
   *
   * @param sParticipantMigrationID
   *        The ID of the participant migration to be deleted. May be
   *        <code>null</code>.
   * @param eNewState
   *        The new participant migration state to use. May not be
   *        <code>null</code>.
   * @return {@link EChange#CHANGED} if the removal was successful.
   */
  @NonNull
  EChange setParticipantMigrationState (@Nullable String sParticipantMigrationID,
                                        @NonNull EParticipantMigrationState eNewState);

  /**
   * Find the participant migration with the provided ID.
   *
   * @param sParticipantMigrationID
   *        The ID to lookup. May be <code>null</code>.
   * @return <code>null</code> if no such participant migration is contained.
   */
  @Nullable
  ISMPParticipantMigration getParticipantMigrationOfID (@Nullable String sParticipantMigrationID);

  /**
   * Find the participant migration of the provided ID.
   *
   * @param eDirection
   *        The direction to query. May not be <code>null</code>.
   * @param eState
   *        The state the entry must have. May not be <code>null</code>. If this
   *        state is "cancelled" the result could be a list, so it's not
   *        advisable to use this URL.
   * @param aParticipantID
   *        The participant ID to check. May be <code>null</code> in which case
   *        the result is always <code>null</code>.
   * @return <code>null</code> if no such participant migration is contained.
   *         The first matching participant otherwise.
   */
  @Nullable
  ISMPParticipantMigration getParticipantMigrationOfParticipantID (@NonNull EParticipantMigrationDirection eDirection,
                                                                   @NonNull EParticipantMigrationState eState,
                                                                   @Nullable IParticipantIdentifier aParticipantID);

  /**
   * Get all outbound participant migrations that have the provided state.
   *
   * @param eState
   *        The state to be used to filter. May be <code>null</code>.
   * @return A list of all contained outbound participant migrations. Never
   *         <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISMPParticipantMigration> getAllOutboundParticipantMigrations (@Nullable EParticipantMigrationState eState);

  /**
   * Get all inbound participant migrations that have the provided state.
   *
   * @param eState
   *        The state to be used to filter. May be <code>null</code>.
   * @return A list of all contained inbound participant migrations. Never
   *         <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISMPParticipantMigration> getAllInboundParticipantMigrations (@Nullable EParticipantMigrationState eState);

  /**
   * Check if an outbound migration for the provided participant identifier is
   * already running.
   *
   * @param aParticipantID
   *        The participant ID to check. May be <code>null</code> in which case
   *        the result is always <code>false</code>.
   * @return <code>true</code> if an outbound migration is already running,
   *         <code>false</code> if not.
   */
  boolean containsOutboundMigrationInProgress (@Nullable IParticipantIdentifier aParticipantID);

  /**
   * Check if an inbound migration for the provided participant identifier is
   * already contained.
   *
   * @param aParticipantID
   *        The participant ID to check. May be <code>null</code> in which case
   *        the result is always <code>false</code>.
   * @return <code>true</code> if an inbound migration is already contained,
   *         <code>false</code> if not.
   */
  boolean containsInboundMigration (@Nullable IParticipantIdentifier aParticipantID);
}
