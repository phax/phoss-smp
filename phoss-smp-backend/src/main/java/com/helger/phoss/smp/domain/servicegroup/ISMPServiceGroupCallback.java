/*
 * Copyright (C) 2015-2022 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.servicegroup;

import javax.annotation.Nonnull;

import com.helger.commons.callback.ICallback;
import com.helger.peppolid.IParticipantIdentifier;

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
   * @param bCreateInSML
   *        <code>true</code> if the service group was also created in the SML,
   *        <code>false</code> if not.
   */
  void onSMPServiceGroupCreated (@Nonnull ISMPServiceGroup aServiceGroup, boolean bCreateInSML);

  /**
   * Invoked after an SMP service group was updated.
   *
   * @param aParticipantID
   *        The ID of the updated object. Never <code>null</code>.
   */
  void onSMPServiceGroupUpdated (@Nonnull IParticipantIdentifier aParticipantID);

  /**
   * Invoked after an SMP service group was deleted.
   *
   * @param aParticipantID
   *        The ID of the deleted object. Never <code>null</code>.
   * @param bDeleteInSML
   *        <code>true</code> if the service group was also deleted in the SML,
   *        <code>false</code> if not.
   */
  void onSMPServiceGroupDeleted (@Nonnull IParticipantIdentifier aParticipantID, boolean bDeleteInSML);
}
