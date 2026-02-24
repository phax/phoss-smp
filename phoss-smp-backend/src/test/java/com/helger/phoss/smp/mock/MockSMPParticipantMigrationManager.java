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
package com.helger.phoss.smp.mock;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationDirection;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationState;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;

/**
 * Mock implementation of {@link ISMPParticipantMigrationManager}.
 *
 * @author Philip Helger
 * @since 5.4.0
 */
final class MockSMPParticipantMigrationManager implements ISMPParticipantMigrationManager
{
  public ISMPParticipantMigration createOutboundParticipantMigration (@NonNull final IParticipantIdentifier aParticipantID,
                                                                      @NonNull @Nonempty final String sMigrationKey)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPParticipantMigration createInboundParticipantMigration (@NonNull final IParticipantIdentifier aParticipantID,
                                                                     @NonNull @Nonempty final String sMigrationKey)
  {
    throw new UnsupportedOperationException ();
  }

  @NonNull
  public EChange deleteParticipantMigrationOfID (@Nullable final String sParticipantMigrationID)
  {
    throw new UnsupportedOperationException ();
  }

  @NonNull
  public EChange deleteAllParticipantMigrationsOfParticipant (@NonNull final IParticipantIdentifier aParticipantID)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange setParticipantMigrationState (@Nullable final String sParticipantMigrationID,
                                               @NonNull final EParticipantMigrationState eNewState)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPParticipantMigration getParticipantMigrationOfID (final String sID)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPParticipantMigration getParticipantMigrationOfParticipantID (@NonNull final EParticipantMigrationDirection eDirection,
                                                                          @NonNull final EParticipantMigrationState eState,
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
