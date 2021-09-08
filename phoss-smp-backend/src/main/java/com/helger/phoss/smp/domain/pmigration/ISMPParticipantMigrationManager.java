/**
 * Copyright (C) 2015-2021 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.pmigration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
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
   * identifier.
   *
   * @param aParticipantID
   *        The participant ID to use. May not be <code>null</code>.
   * @param sMigrationKey
   *        The migration key received from the SML. May neither be
   *        <code>null</code> nor empty.
   * @return The created migration domain object. May be <code>null</code> in
   *         case persistence failed.
   */
  @Nullable
  ISMPParticipantMigration createOutboundParticipantMigration (@Nonnull IParticipantIdentifier aParticipantID,
                                                               @Nonnull @Nonempty String sMigrationKey);

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
  @Nonnull
  EChange setParticipantMigrationState (@Nullable String sParticipantMigrationID, @Nonnull EParticipantMigrationState eNewState);

  /**
   * Find the participant migration with the provided ID.
   *
   * @param sID
   *        The ID to lookup. May be <code>null</code>.
   * @return <code>null</code> if no such participant migration is contained.
   */
  @Nullable
  ISMPParticipantMigration getParticipantMigrationOfID (@Nullable String sID);

  /**
   * Get all outbound participant migrations that have the provided state.
   *
   * @param eState
   *        The state to be used to filter. May be <code>null</code>.
   * @return A list of all contained outbound participant migrations. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
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
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPParticipantMigration> getAllInboundParticipantMigrations (@Nullable EParticipantMigrationState eState);

  /**
   * Check if an outbound migration for the provided participant identifier is
   * already running.
   *
   * @param aParticipantID
   *        The participant ID to check. May be <code>null</code>.
   * @return <code>true</code> if an outbound migration is already running,
   *         <code>false</code> if not.
   */
  boolean containsOutboundMigrationInProgress (@Nullable IParticipantIdentifier aParticipantID);

  /**
   * Check if an inbound migration for the provided participant identifier is
   * already running.
   *
   * @param aParticipantID
   *        The participant ID to check. May be <code>null</code>.
   * @return <code>true</code> if an inbound migration is already running,
   *         <code>false</code> if not.
   */
  boolean containsInboundMigrationInProgress (@Nullable IParticipantIdentifier aParticipantID);
}
