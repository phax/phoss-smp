package com.helger.peppol.smpserver.mock;

import com.helger.commons.annotation.UnsupportedOperation;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.state.EChange;
import com.helger.peppol.smpserver.settings.ISMPSettings;
import com.helger.peppol.smpserver.settings.ISMPSettingsCallback;
import com.helger.peppol.smpserver.settings.ISMPSettingsManager;

public final class MockSMPSettingsManager implements ISMPSettingsManager
{
  @UnsupportedOperation
  public CallbackList <ISMPSettingsCallback> getCallbacks ()
  {
    throw new UnsupportedOperationException ();
  }

  @UnsupportedOperation
  public EChange updateSettings (final boolean bRESTWritableAPIDisabled,
                                 final boolean bPEPPOLDirectoryIntegrationEnabled,
                                 final String sPEPPOLDirectoryHostName,
                                 final boolean bWriteToSML,
                                 final String sSMLURL)
  {
    throw new UnsupportedOperationException ();
  }

  @UnsupportedOperation
  public ISMPSettings getSettings ()
  {
    throw new UnsupportedOperationException ();
  }
}
