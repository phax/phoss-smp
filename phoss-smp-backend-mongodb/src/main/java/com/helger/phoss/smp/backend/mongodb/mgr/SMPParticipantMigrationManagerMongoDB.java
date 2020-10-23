/**
 * Copyright (C) 2019-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.backend.mongodb.mgr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.mongodb.client.model.Indexes;

/**
 * Implementation of {@link ISMPParticipantMigrationManager} for MongoDB
 *
 * @author Philip Helger
 * @since 5.3.1
 */
public final class SMPParticipantMigrationManagerMongoDB extends AbstractManagerMongoDB implements ISMPParticipantMigrationManager
{
  private static final String BSON_ID = "id";

  public SMPParticipantMigrationManagerMongoDB ()
  {
    super ("smp-participant-migration");
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
  }

  public ISMPParticipantMigration createOutboundParticipantMigration (final IParticipantIdentifier aParticipantID)
  {
    // TODO
    throw new UnsupportedOperationException ();
  }

  @Nonnull
  public EChange deleteParticipantMigration (@Nullable final String sParticipantMigrationID)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPParticipantMigration getParticipantMigrationOfID (final String sID)
  {
    // TODO
    return null;
  }

  public ICommonsList <ISMPParticipantMigration> getAllOutboundParticipantMigrations ()
  {
    // TODO
    return null;
  }

  public ICommonsList <ISMPParticipantMigration> getAllInboundParticipantMigrations ()
  {
    // TODO
    return null;
  }

  public boolean containsOutboundMigration (final IParticipantIdentifier aParticipantID)
  {
    // TODO
    return false;
  }

  public boolean containsInboundMigration (final IParticipantIdentifier aParticipantID)
  {
    // TODO
    return false;
  }
}
