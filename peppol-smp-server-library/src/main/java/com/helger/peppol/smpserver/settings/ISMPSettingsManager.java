package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.state.EChange;

/**
 * Base interface for the SMP settings manager
 *
 * @author Philip Helger
 */
public interface ISMPSettingsManager
{
  @Nonnull
  @ReturnsMutableObject ("by design")
  CallbackList <ISMPSettingsCallback> getCallbacks ();

  @Nonnull
  ISMPSettings getSettings ();

  @Nonnull
  EChange updateSettings (final boolean bRESTWritableAPIDisabled,
                          final boolean bPEPPOLDirectoryIntegrationEnabled,
                          @Nullable final String sPEPPOLDirectoryHostName,
                          final boolean bWriteToSML,
                          @Nullable final String sSMLURL);
}
