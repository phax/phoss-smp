/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.mock;

import javax.annotation.Nullable;

import com.helger.commons.annotation.UnsupportedOperation;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.state.EChange;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.settings.ISMPSettingsCallback;
import com.helger.phoss.smp.settings.ISMPSettingsManager;

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
                                 final boolean bPEPPOLDirectoryIntegrationEnabled,
                                 final boolean bPEPPOLDirectoryIntegrationRequired,
                                 final boolean bPEPPOLDirectoryIntegrationAutoUpdate,
                                 @Nullable final String sPEPPOLDirectoryHostName,
                                 final boolean bSMLActive,
                                 final boolean bSMLRequired,
                                 @Nullable final ISMLInfo aSMLInfo)
  {
    throw new UnsupportedOperationException ();
  }

  @UnsupportedOperation
  public ISMPSettings getSettings ()
  {
    throw new UnsupportedOperationException ();
  }
}
