/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.state.EChange;
import com.helger.peppol.sml.ISMLInfo;

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
  @Nonnull
  @ReturnsMutableObject
  CallbackList <ISMPSettingsCallback> callbacks ();

  /**
   * @return The contained settings. Never <code>null</code>.
   */
  @Nonnull
  ISMPSettings getSettings ();

  /**
   * Update the existing settings
   *
   * @param bRESTWritableAPIDisabled
   *        <code>true</code> to enable writable access by REST services
   * @param bPEPPOLDirectoryIntegrationEnabled
   *        <code>true</code> to enable PEPPOL Directory integration
   * @param bPEPPOLDirectoryIntegrationAutoUpdate
   *        <code>true</code> to automatically update the PEPPOL Directory if a
   *        business card changes
   * @param sPEPPOLDirectoryHostName
   *        The hostname of the PEPPOL Directory server to use. Must be fully
   *        qualified including the protocol.
   * @param bSMLActive
   *        <code>true</code> to enable write access to the SML
   * @param bSMLNeeded
   *        <code>true</code> to warn if SML is disabled
   * @param aSMLInfo
   *        The SMLInfo object to use. May be <code>null</code> if not active.
   * @return {@link EChange}
   */
  @Nonnull
  EChange updateSettings (boolean bRESTWritableAPIDisabled,
                          boolean bPEPPOLDirectoryIntegrationEnabled,
                          boolean bPEPPOLDirectoryIntegrationAutoUpdate,
                          @Nullable String sPEPPOLDirectoryHostName,
                          boolean bSMLActive,
                          boolean bSMLNeeded,
                          @Nullable ISMLInfo aSMLInfo);
}
