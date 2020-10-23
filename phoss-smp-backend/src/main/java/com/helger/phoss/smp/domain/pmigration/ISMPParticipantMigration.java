package com.helger.phoss.smp.domain.pmigration;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Defines the details of a single participant migration
 *
 * @author Philip Helger
 * @since 5.3.1
 */
public interface ISMPParticipantMigration extends IHasID <String>, Serializable
{
  /**
   * @return Outbound or inbound migration?
   */
  @Nonnull
  EParticipantMigrationDirection getDirection ();

  /**
   * @return The participant identifier that is going to be migrated away.
   */
  @Nonnull
  IParticipantIdentifier getParticipantIdentifier ();

  /**
   * @return The date and time, when the migration was initiated. This is
   *         relevant, as the migration key is only valid for some time.
   */
  @Nonnull
  LocalDateTime getInitiationDateTime ();

  /**
   * @return The created migration key.
   */
  @Nonnull
  @Nonempty
  String getMigrationKey ();
}
