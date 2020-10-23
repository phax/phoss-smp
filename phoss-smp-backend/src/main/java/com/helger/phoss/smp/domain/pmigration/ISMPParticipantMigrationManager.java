package com.helger.phoss.smp.domain.pmigration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * This is the interface for managing participant migrations.
 *
 * @author Philip Helger
 * @since 5.3.1
 */
public interface ISMPParticipantMigrationManager
{
  /**
   * Create a new outbound participant migration for the provided participant
   * identifier. The migration key is automatically created inside.
   *
   * @param aParticipantID
   *        The participant ID to use. May not be <code>null</code>.
   * @return The created migration domain object. Never <code>null</code>.
   */
  @Nonnull
  ISMPParticipantMigration createOutboundParticipantMigration (@Nonnull IParticipantIdentifier aParticipantID);

  /**
   * Delete an existing participant migration.
   *
   * @param sParticipantMigrationID
   *        The ID of the participant migration to be deleted. May be
   *        <code>null</code>.
   * @return {@link EChange#CHANGED} if the removal was successful.
   */
  @Nonnull
  EChange deleteParticipantMigration (@Nullable String sParticipantMigrationID);

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
   * @return A list of all contained outbound participant migrations. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPParticipantMigration> getAllOutboundParticipantMigrations ();

  /**
   * @return A list of all contained inbound participant migrations. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPParticipantMigration> getAllInboundParticipantMigrations ();

  /**
   * Check if an outbound migration for the provided participant identifier is
   * already running.
   *
   * @param aParticipantID
   *        The participant ID to check. May be <code>null</code>.
   * @return <code>true</code> if an outbound migration is already running,
   *         <code>false</code> if not.
   */
  boolean containsOutboundMigration (@Nullable IParticipantIdentifier aParticipantID);

  /**
   * Check if an inbound migration for the provided participant identifier is
   * already running.
   *
   * @param aParticipantID
   *        The participant ID to check. May be <code>null</code>.
   * @return <code>true</code> if an inbound migration is already running,
   *         <code>false</code> if not.
   */
  boolean containsInboundMigration (@Nullable IParticipantIdentifier aParticipantID);
}
