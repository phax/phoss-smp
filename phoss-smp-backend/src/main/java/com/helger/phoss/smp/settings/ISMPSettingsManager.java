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
package com.helger.phoss.smp.settings;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;

/**
 * Base interface for the SMP settings manager
 *
 * @author Philip Helger
 */
public interface ISMPSettingsManager
{
  /**
   * @return A non-<code>null</code> mutable list of callbacks.
   */
  @NonNull
  @ReturnsMutableObject
  CallbackList <ISMPSettingsCallback> callbacks ();

  /**
   * @return The contained settings. Never <code>null</code>.
   */
  @NonNull
  ISMPSettings getSettings ();

  /**
   * Update the existing settings
   *
   * @param bRESTWritableAPIDisabled
   *        <code>true</code> to enable writable access by REST services
   * @param bDirectoryIntegrationEnabled
   *        <code>true</code> to enable Directory integration
   * @param bDirectoryIntegrationRequired
   *        <code>true</code> to warn if Directory is disabled
   * @param bDirectoryIntegrationAutoUpdate
   *        <code>true</code> to automatically update the Directory if a
   *        business card changes
   * @param sDirectoryHostName
   *        The hostname of the Directory server to use. Must be fully qualified
   *        including the protocol.
   * @param bSMLEnabled
   *        <code>true</code> to enable write access to the SML
   * @param bSMLRequired
   *        <code>true</code> to warn if SML is disabled
   * @param sSMLInfoID
   *        The SMLInfo object ID to use. May be <code>null</code> if not
   *        active.
   * @return {@link EChange}
   */
  @NonNull
  EChange updateSettings (boolean bRESTWritableAPIDisabled,
                          boolean bDirectoryIntegrationEnabled,
                          boolean bDirectoryIntegrationRequired,
                          boolean bDirectoryIntegrationAutoUpdate,
                          @Nullable String sDirectoryHostName,
                          boolean bSMLEnabled,
                          boolean bSMLRequired,
                          @Nullable String sSMLInfoID);
}
