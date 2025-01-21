/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.ui.secure;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.phoss.smp.config.SMPHttpConfiguration;
import com.helger.scope.singleton.AbstractSessionSingleton;

/**
 * A helper class that checks once per session if proxy information are
 * configured or not. Usually this information does not change, it it is not
 * worth the effort to query that in every request.
 *
 * @author Philip Helger
 */
public final class SecureSessionState extends AbstractSessionSingleton
{
  private final boolean m_bHttpProxyEnabled;
  private final boolean m_bHttpsProxyEnabled;

  @Deprecated
  @UsedViaReflection
  public SecureSessionState ()
  {
    m_bHttpProxyEnabled = SMPHttpConfiguration.getAsHttpProxySettings () != null;
    m_bHttpsProxyEnabled = SMPHttpConfiguration.getAsHttpsProxySettings () != null;
  }

  @Nonnull
  public static SecureSessionState getInstance ()
  {
    return getSessionSingleton (SecureSessionState.class);
  }

  public boolean isHttpProxyEnabled ()
  {
    return m_bHttpProxyEnabled;
  }

  public boolean isHttpsProxyEnabled ()
  {
    return m_bHttpsProxyEnabled;
  }

  public boolean isAnyHttpProxyEnabled ()
  {
    return m_bHttpProxyEnabled || m_bHttpsProxyEnabled;
  }
}
