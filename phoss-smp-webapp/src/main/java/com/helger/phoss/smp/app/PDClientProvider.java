/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.pd.client.PDClient;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;

/**
 * A singleton that keeps track of the {@link PDClient} creation (Peppol Directory client). It avoid
 * instantiating the object too often and ensures the object is correctly closed upon shutdown.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class PDClientProvider extends AbstractGlobalWebSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDClientProvider.class);

  private PDClient m_aPDClient;

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public PDClientProvider ()
  {}

  @NonNull
  public static PDClientProvider getInstance ()
  {
    return getGlobalSingleton (PDClientProvider.class);
  }

  @Override
  protected void onDestroy (@NonNull final IScope aScopeInDestruction)
  {
    m_aRWLock.writeLocked (() -> {
      StreamHelper.close (m_aPDClient);
      m_aPDClient = null;
    });
  }

  /**
   * Reset the existing client in case the SMP settings changed.
   */
  public void resetPDClient ()
  {
    m_aRWLock.writeLocked (() -> {
      // Ensure to close the old one first
      StreamHelper.close (m_aPDClient);
      m_aPDClient = null;
    });
  }

  /**
   * @return The {@link PDClient} to be used with the current settings. May be <code>null</code> if
   *         no Directory hostname is configured.
   * @throws PDClientCreationException
   *         If the client cannot be created, e.g. because the PD client configuration is incomplete
   *         or invalid. The cause is the original exception.
   * @see #getPDClientOrNull()
   */
  @Nullable
  public PDClient getPDClient () throws PDClientCreationException
  {
    PDClient ret = m_aRWLock.readLockedGet (() -> m_aPDClient);
    if (ret == null)
    {
      final String sPDHostName = SMPMetaManager.getSettings ().getDirectoryHostName ();
      if (StringHelper.isNotEmpty (sPDHostName))
      {
        m_aRWLock.writeLock ().lock ();
        try
        {
          // Try again in write lock
          ret = m_aPDClient;
          if (ret == null)
          {
            // Create a new one
            try
            {
              ret = m_aPDClient = new PDClient (sPDHostName);
              // Note: by default a logging exception handler is installed
            }
            catch (final RuntimeException ex)
            {
              // E.g. NullPointerException for a missing key store type
              throw new PDClientCreationException ("Failed to create the PDClient. Please check the PD client configuration.",
                                                   ex);
            }
          }
        }
        finally
        {
          m_aRWLock.writeLock ().unlock ();
        }
      }
    }
    return ret;
  }

  /**
   * Same as {@link #getPDClient()} but never throws an exception. Use this variant if the caller
   * cannot do anything useful with a failure, e.g. when running in the background.
   *
   * @return The {@link PDClient} to be used with the current settings. May be <code>null</code> if
   *         no Directory hostname is configured or if the client could not be created. In the
   *         latter case the problem is logged.
   */
  @Nullable
  public PDClient getPDClientOrNull ()
  {
    try
    {
      return getPDClient ();
    }
    catch (final PDClientCreationException ex)
    {
      LOGGER.error ("Failed to get the PDClient", ex);
      return null;
    }
  }
}
