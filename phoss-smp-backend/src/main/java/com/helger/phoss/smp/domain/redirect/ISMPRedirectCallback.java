/*
 * Copyright (C) 2015-2021 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.redirect;

import javax.annotation.Nonnull;

import com.helger.commons.callback.ICallback;

/**
 * Interface for an SMP redirect callback.
 *
 * @author Philip Helger
 */
public interface ISMPRedirectCallback extends ICallback
{
  /**
   * Invoked after an SMP redirect was created.
   *
   * @param aRedirect
   *        The created object. Never <code>null</code>.
   */
  default void onSMPRedirectCreated (@Nonnull final ISMPRedirect aRedirect)
  {}

  /**
   * Invoked after an SMP redirect was updated.
   *
   * @param aRedirect
   *        The updated object. Never <code>null</code>.
   */
  default void onSMPRedirectUpdated (@Nonnull final ISMPRedirect aRedirect)
  {}

  /**
   * Invoked after an SMP redirect was deleted.
   *
   * @param aRedirect
   *        The ID deleted object. Never <code>null</code>.
   */
  default void onSMPRedirectDeleted (@Nonnull final ISMPRedirect aRedirect)
  {}
}
