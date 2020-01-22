/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.settings;

import javax.annotation.Nonnull;

import com.helger.commons.callback.ICallback;

/**
 * Interface for an SMP settings callback.
 * 
 * @author Philip Helger
 */
public interface ISMPSettingsCallback extends ICallback
{
  /**
   * Invoked after the SMP settings were changed.
   *
   * @param aSettings
   *        The settings object that changed.
   */
  void onSMPSettingsChanged (@Nonnull ISMPSettings aSettings);
}
