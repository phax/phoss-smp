/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.app;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.pd.client.PDClient;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;

/**
 * A singleton that keeps track of the {@link PDClient} creation (PEPPOL
 * Directory client). It avoid instantiating the object too often and ensures
 * the object is correctly closed upon shutdown.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class PDClientProvider extends AbstractGlobalWebSingleton
{
  private PDClient m_aPDClient;

  @Deprecated
  @UsedViaReflection
  public PDClientProvider ()
  {}

  @Nonnull
  public static PDClientProvider getInstance ()
  {
    return getGlobalSingleton (PDClientProvider.class);
  }

  @Override
  protected void onDestroy (@Nonnull final IScope aScopeInDestruction)
  {
    m_aRWLock.writeLocked ( () -> {
      StreamHelper.close (m_aPDClient);
      m_aPDClient = null;
    });
  }

  /**
   * Reset the existing client in case the SMP settings changed.
   */
  public void resetPDClient ()
  {
    m_aRWLock.writeLocked ( () -> m_aPDClient = null);
  }

  /**
   * @return The {@link PDClient} to be used with the current settings. Never
   *         <code>null</code>.
   */
  @Nonnull
  public PDClient getPDClient ()
  {
    PDClient ret = m_aRWLock.readLocked ( () -> m_aPDClient);
    if (ret == null)
      ret = m_aRWLock.writeLocked ( () -> {
        if (m_aPDClient == null)
        {
          // Create a new one
          m_aPDClient = new PDClient (SMPMetaManager.getSettings ().getPEPPOLDirectoryHostName ());
        }
        return m_aPDClient;
      });
    return ret;
  }
}
