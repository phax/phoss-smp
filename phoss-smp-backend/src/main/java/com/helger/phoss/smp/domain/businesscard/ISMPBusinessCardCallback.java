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
package com.helger.phoss.smp.domain.businesscard;

import org.jspecify.annotations.NonNull;

import com.helger.base.callback.ICallback;

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
   * @param bSyncToDirectory
   *        <code>true</code> to synchronize the change to the remote directory, <code>false</code>
   *        to disable it
   */
  void onSMPBusinessCardCreatedOrUpdated (@NonNull ISMPBusinessCard aBusinessCard, boolean bSyncToDirectory);

  /**
   * Invoked after a business card was deleted.
   *
   * @param aBusinessCard
   *        The deleted business card. May not be <code>null</code>.
   * @param bSyncToDirectory
   *        <code>true</code> to synchronize the change to the remote directory, <code>false</code>
   *        to disable it
   */
  void onSMPBusinessCardDeleted (@NonNull ISMPBusinessCard aBusinessCard, boolean bSyncToDirectory);
}
