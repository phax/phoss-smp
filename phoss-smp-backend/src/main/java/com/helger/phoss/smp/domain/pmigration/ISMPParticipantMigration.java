package com.helger.phoss.smp.domain.pmigration;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Defines the details of a single participant migration
 *
 * @author Philip Helger
 * @since 5.4.0
 */
public interface ISMPParticipantMigration extends IHasID <String>, Serializable
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
