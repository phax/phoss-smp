/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.domain.servicegroup;

import javax.annotation.Nonnull;

import com.helger.commons.callback.ICallback;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

/**
 * Interface for an SMP service group callback.
 *
 * @author Philip Helger
 */
public interface ISMPServiceGroupCallback extends ICallback
{
  /**
   * Invoked after an SMP service group was created.
   *
   * @param aServiceGroup
   *        The created object. Never <code>null</code>.
   */
  default void onSMPServiceGroupCreated (@Nonnull final ISMPServiceGroup aServiceGroup)
  {}

  /**
   * Invoked after an SMP service group was updated.
   *
   * @param aParticipantID
   *        The ID of the updated object. Never <code>null</code>.
   */
  default void onSMPServiceGroupUpdated (@Nonnull final IParticipantIdentifier aParticipantID)
  {}

  /**
   * Invoked after an SMP service group was deleted.
   *
   * @param aParticipantID
   *        The ID of the deleted object. Never <code>null</code>.
   */
  default void onSMPServiceGroupDeleted (@Nonnull final IParticipantIdentifier aParticipantID)
  {}
}
