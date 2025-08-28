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
package com.helger.phoss.smp.mock;

import com.helger.annotation.style.UnsupportedOperation;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.settings.ISMPSettingsCallback;
import com.helger.phoss.smp.settings.ISMPSettingsManager;

import jakarta.annotation.Nullable;

/**
 * Mock implementation of {@link ISMPSettingsManager}.
 *
 * @author Philip Helger
 */
final class MockSMPSettingsManager implements ISMPSettingsManager
{
  @UnsupportedOperation
  public CallbackList <ISMPSettingsCallback> callbacks ()
  {
    throw new UnsupportedOperationException ();
  }

  @UnsupportedOperation
  public EChange updateSettings (final boolean bRESTWritableAPIDisabled,
                                 final boolean bDirectoryIntegrationEnabled,
                                 final boolean bDirectoryIntegrationRequired,
                                 final boolean bDirectoryIntegrationAutoUpdate,
                                 @Nullable final String sDirectoryHostName,
                                 final boolean bSMLActive,
                                 final boolean bSMLRequired,
                                 @Nullable final String sSMLInfoID)
  {
    throw new UnsupportedOperationException ();
  }

  @UnsupportedOperation
  public ISMPSettings getSettings ()
  {
    throw new UnsupportedOperationException ();
  }
}
