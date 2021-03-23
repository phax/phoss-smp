/**
 * Copyright (C) 2014-2021 Philip Helger and contributors
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
package com.helger.phoss.smp.app;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.pd.client.PDClient;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;

/**
 * A singleton that keeps track of the {@link PDClient} creation (Peppol
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
    PDClient ret = m_aRWLock.readLockedGet ( () -> m_aPDClient);
    if (ret == null)
    {
      m_aRWLock.writeLock ().lock ();
      try
      {
        // Try again in write lock
        ret = m_aPDClient;
        if (ret == null)
        {
          // Create a new one
          ret = m_aPDClient = new PDClient (SMPMetaManager.getSettings ().getDirectoryHostName ());
          // Note: by default a logging exception handler is installed
        }
      }
      finally
      {
        m_aRWLock.writeLock ().unlock ();
      }
    }
    return ret;
  }
}
