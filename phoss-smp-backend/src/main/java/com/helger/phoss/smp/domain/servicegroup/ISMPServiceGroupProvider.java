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
package com.helger.phoss.smp.domain.servicegroup;

import com.helger.peppolid.IParticipantIdentifier;

import jakarta.annotation.Nullable;

/**
 * Base interface for an {@link ISMPServiceGroup} provider.
 *
 * @author Philip Helger
 */
public interface ISMPServiceGroupProvider
{
  /**
   * Get the service group of the passed participant identifier.
   *
   * @param aParticipantIdentifier
   *        The participant identifier to search. May be <code>null</code>.
   * @return <code>null</code> if the participant identifier is
   *         <code>null</code> or if it is not contained.
   */
  @Nullable
  ISMPServiceGroup getSMPServiceGroupOfID (@Nullable IParticipantIdentifier aParticipantIdentifier);
}
