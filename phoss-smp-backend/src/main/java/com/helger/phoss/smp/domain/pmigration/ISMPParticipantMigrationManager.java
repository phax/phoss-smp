package com.helger.phoss.smp.domain.pmigration;

import javax.annotation.Nonnull;

import com.helger.peppolid.IParticipantIdentifier;

/**
 * This is the interface for managing participant migrations.
 *
 * @author Philip Helger
 * @since 5.3.1
 */
public interface ISMPParticipantMigrationManager
{
  @Nonnull
  ISMPParticipantMigration createOutboundMigration (@Nonnull IParticipantIdentifier aParticipantID);
}
