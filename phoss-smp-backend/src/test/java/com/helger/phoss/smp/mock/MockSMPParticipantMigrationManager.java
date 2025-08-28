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
package com.helger.phoss.smp.mock;

import com.helger.annotation.Nonempty;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationDirection;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationState;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Mock implementation of {@link ISMPParticipantMigrationManager}.
 *
 * @author Philip Helger
 * @since 5.4.0
 */
final class MockSMPParticipantMigrationManager implements ISMPParticipantMigrationManager
{
  public ISMPParticipantMigration createOutboundParticipantMigration (@Nonnull final IParticipantIdentifier aParticipantID,
                                                                      @Nonnull @Nonempty final String sMigrationKey)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPParticipantMigration createInboundParticipantMigration (@Nonnull final IParticipantIdentifier aParticipantID,
                                                                     @Nonnull @Nonempty final String sMigrationKey)
  {
    throw new UnsupportedOperationException ();
  }

  @Nonnull
  public EChange deleteParticipantMigrationOfID (@Nullable final String sParticipantMigrationID)
  {
    throw new UnsupportedOperationException ();
  }

  @Nonnull
  public EChange deleteAllParticipantMigrationsOfParticipant (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange setParticipantMigrationState (@Nullable final String sParticipantMigrationID,
                                               @Nonnull final EParticipantMigrationState eNewState)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPParticipantMigration getParticipantMigrationOfID (final String sID)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPParticipantMigration getParticipantMigrationOfParticipantID (@Nonnull final EParticipantMigrationDirection eDirection,
                                                                          @Nonnull final EParticipantMigrationState eState,
                                                                          @Nullable final IParticipantIdentifier aParticipantID)
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsList <ISMPParticipantMigration> getAllOutboundParticipantMigrations (@Nullable final EParticipantMigrationState eState)
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsList <ISMPParticipantMigration> getAllInboundParticipantMigrations (@Nullable final EParticipantMigrationState eState)
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsOutboundMigrationInProgress (final IParticipantIdentifier aParticipantID)
  {
    return false;
  }

  public boolean containsInboundMigration (final IParticipantIdentifier aParticipantID)
  {
    return false;
  }
}
