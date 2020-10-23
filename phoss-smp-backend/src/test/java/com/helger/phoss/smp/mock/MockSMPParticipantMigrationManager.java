/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.mock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;

/**
 * Mock implementation of {@link ISMPParticipantMigrationManager}.
 *
 * @author Philip Helger
 */
final class MockSMPParticipantMigrationManager implements ISMPParticipantMigrationManager
{
  public ISMPParticipantMigration createOutboundParticipantMigration (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteParticipantMigration (@Nullable final String sParticipantMigrationID)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPParticipantMigration getParticipantMigrationOfID (final String sID)
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsList <ISMPParticipantMigration> getAllOutboundParticipantMigrations ()
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsList <ISMPParticipantMigration> getAllInboundParticipantMigrations ()
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsOutboundMigration (final IParticipantIdentifier aParticipantID)
  {
    return false;
  }

  public boolean containsInboundMigration (final IParticipantIdentifier aParticipantID)
  {
    return false;
  }
}
