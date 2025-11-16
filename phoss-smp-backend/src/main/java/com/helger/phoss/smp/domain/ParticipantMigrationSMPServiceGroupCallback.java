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
package com.helger.phoss.smp.domain;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupCallback;

/**
 * Special {@link ISMPServiceGroupCallback} to delete the participant
 * migrations, if the service group is deleted.
 *
 * @author Philip Helger
 */
public class ParticipantMigrationSMPServiceGroupCallback implements ISMPServiceGroupCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ParticipantMigrationSMPServiceGroupCallback.class);

  private final ISMPParticipantMigrationManager m_aParticipantMigrationMgr;

  public ParticipantMigrationSMPServiceGroupCallback (@NonNull final ISMPParticipantMigrationManager aParticipantMigrationMgr)
  {
    ValueEnforcer.notNull (aParticipantMigrationMgr, "ParticipantMigrationMgr");
    m_aParticipantMigrationMgr = aParticipantMigrationMgr;
  }

  public void onSMPServiceGroupCreated (@NonNull final ISMPServiceGroup aServiceGroup, final boolean bCreateInSML)
  {}

  public void onSMPServiceGroupUpdated (@NonNull final IParticipantIdentifier aParticipantID)
  {}

  @Override
  public void onSMPServiceGroupDeleted (@NonNull final IParticipantIdentifier aParticipantID,
                                        final boolean bDeleteInSML)
  {
    // If service group is deleted, also delete respective participant
    // migrations
    final EChange eChange = m_aParticipantMigrationMgr.deleteAllParticipantMigrationsOfParticipant (aParticipantID);
    if (eChange.isChanged ())
      LOGGER.info ("Deleted all participant migrations for participant ID '" + aParticipantID.getURIEncoded () + "'");
    else
      LOGGER.info ("Found no participant migrations for participant ID '" + aParticipantID.getURIEncoded () + "'");
  }
}
