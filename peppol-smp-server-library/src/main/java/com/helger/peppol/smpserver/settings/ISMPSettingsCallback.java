package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;

import com.helger.commons.callback.ICallback;

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
