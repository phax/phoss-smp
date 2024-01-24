/*
 * Copyright (C) 2015-2024 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.serviceinfo;

import javax.annotation.Nonnull;

import com.helger.commons.callback.ICallback;

/**
 * Interface for an SMP service information callback.
 *
 * @author Philip Helger
 * @since 5.1.0
 */
public interface ISMPServiceInformationCallback extends ICallback
{
  /**
   * Invoked after an SMP service information was created.
   *
   * @param aServiceInformation
   *        The created object. Never <code>null</code>.
   */
  default void onSMPServiceInformationCreated (@Nonnull final ISMPServiceInformation aServiceInformation)
  {}

  /**
   * Invoked after an SMP service information was updated.
   *
   * @param aServiceInformation
   *        The updated object. Never <code>null</code>.
   */
  default void onSMPServiceInformationUpdated (@Nonnull final ISMPServiceInformation aServiceInformation)
  {}

  /**
   * Invoked after an SMP service information was deleted.
   *
   * @param aServiceInformation
   *        The deleted object. Never <code>null</code>.
   */
  default void onSMPServiceInformationDeleted (@Nonnull final ISMPServiceInformation aServiceInformation)
  {}
}
