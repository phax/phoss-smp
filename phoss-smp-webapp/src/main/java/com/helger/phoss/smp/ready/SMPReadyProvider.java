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
package com.helger.phoss.smp.ready;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.spi.ServiceLoaderHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * Evaluates all backend-specific readiness checks.
 *
 * @author vinit-thummar
 * @since 8.3.1
 */
@ThreadSafe
public final class SMPReadyProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPReadyProvider.class);
  private static final ICommonsList <ISMPReadyProviderExtensionSPI> LIST = new CommonsArrayList <> ();
  static
  {
    LIST.addAll (ServiceLoaderHelper.getAllSPIImplementations (ISMPReadyProviderExtensionSPI.class));
    LOGGER.info ("Found " +
                 LIST.size () +
                 " implementation(s) of " +
                 ISMPReadyProviderExtensionSPI.class.getSimpleName ());
  }

  private SMPReadyProvider ()
  {}

  static boolean areAllReady (@NonNull final Iterable <? extends ISMPReadyProviderExtensionSPI> aProviders)
  {
    for (final ISMPReadyProviderExtensionSPI aProvider : aProviders)
      try
      {
        if (!aProvider.isReady ())
          return false;
      }
      catch (final RuntimeException ex)
      {
        LOGGER.warn ("Readiness check failed", ex);
        return false;
      }
    return true;
  }

  /**
   * @return <code>true</code> if all registered backend checks report ready. If no check is
   *         registered, the backend is considered ready. This is the intended behaviour for the
   *         XML backend.
   */
  public static boolean isReady ()
  {
    return areAllReady (LIST);
  }
}
