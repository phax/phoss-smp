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
package com.helger.phoss.smp.domain.businesscard;

import com.helger.base.callback.ICallback;

import jakarta.annotation.Nonnull;

/**
 * Callback interface for {@link ISMPBusinessCardManager} objects.
 * <p>
 * The files in this package are licensed under Apache 2.0 license
 * </p>
 *
 * @author Philip Helger
 * @since 5.0.4
 */
public interface ISMPBusinessCardCallback extends ICallback
{
  /**
   * Invoked after a business card was created or updated.
   *
   * @param aBusinessCard
   *        The new business card. May not be <code>null</code>.
   */
  void onSMPBusinessCardCreatedOrUpdated (@Nonnull ISMPBusinessCard aBusinessCard);

  /**
   * Invoked after a business card was deleted.
   *
   * @param aBusinessCard
   *        The deleted business card. May not be <code>null</code>.
   */
  void onSMPBusinessCardDeleted (@Nonnull ISMPBusinessCard aBusinessCard);
}
