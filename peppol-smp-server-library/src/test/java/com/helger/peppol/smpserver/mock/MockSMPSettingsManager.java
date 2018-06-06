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
package com.helger.peppol.smpserver.mock;

import javax.annotation.Nullable;

import com.helger.commons.annotation.UnsupportedOperation;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.state.EChange;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smpserver.settings.ISMPSettings;
import com.helger.peppol.smpserver.settings.ISMPSettingsCallback;
import com.helger.peppol.smpserver.settings.ISMPSettingsManager;

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
                                 final boolean bPEPPOLDirectoryIntegrationAutoUpdate,
                                 @Nullable final String sPEPPOLDirectoryHostName,
                                 final boolean bSMLActive,
                                 final boolean bSMLNeeded,
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
