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

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.concurrent.BasicThreadFactory;
import com.helger.base.spi.ServiceLoaderHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.config.SMPServerConfiguration;

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
  /**
   * The readiness checks are executed in a separate thread, so that a check that blocks - like
   * waiting for a database connection - cannot block the HTTP thread indefinitely. A cached thread
   * pool is used, so that a check that is still stuck does not delay the next readiness request.
   */
  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool (BasicThreadFactory.builder ()
                                                                                                   .namingPattern ("smp-ready-%d")
                                                                                                   .daemon (true)
                                                                                                   .build ());
  static
  {
    LIST.addAll (ServiceLoaderHelper.getAllSPIImplementations (ISMPReadyProviderExtensionSPI.class));
    LOGGER.info ("Found " +
                 LIST.size () +
                 " implementation(s) of " +
                 ISMPReadyProviderExtensionSPI.class.getSimpleName ());
    if (LIST.isEmpty ())
    {
      LOGGER.error ("No implementation of " +
                    ISMPReadyProviderExtensionSPI.class.getSimpleName () +
                    " was found. The SMP will therefore never be considered as being ready. Every backend must provide exactly one implementation of this SPI interface.");
    }
  }

  private SMPReadyProvider ()
  {}

  /**
   * Invoke all provided readiness checks in the calling thread.
   *
   * @param aProviders
   *        The readiness checks to be invoked. May not be <code>null</code>.
   * @return <code>true</code> only if at least one check is contained and all of them report ready.
   */
  @VisibleForTesting
  static boolean areAllReady (@NonNull final Iterable <? extends ISMPReadyProviderExtensionSPI> aProviders)
  {
    boolean bFound = false;
    for (final ISMPReadyProviderExtensionSPI aProvider : aProviders)
    {
      bFound = true;
      try
      {
        if (!aProvider.isReady ())
          return false;
      }
      catch (final RuntimeException ex)
      {
        LOGGER.warn ("Readiness check of " + aProvider.getClass ().getName () + " failed", ex);
        return false;
      }
    }
    // Not a single readiness check means, that the backend did not declare its readiness at all
    return bFound;
  }

  /**
   * Invoke all provided readiness checks, but never take longer than the provided timeout. If the
   * checks do not finish in time, the SMP is considered to be not ready.
   *
   * @param aProviders
   *        The readiness checks to be invoked. May not be <code>null</code>.
   * @param aTimeout
   *        The maximum duration the checks may take. If it is <code>null</code> or &le; 0, the
   *        checks are invoked in the calling thread without a time limit.
   * @return <code>true</code> only if at least one check is contained and all of them reported
   *         ready in time.
   */
  @VisibleForTesting
  static boolean areAllReady (@NonNull final Iterable <? extends ISMPReadyProviderExtensionSPI> aProviders,
                              final Duration aTimeout)
  {
    if (aTimeout == null || aTimeout.isZero () || aTimeout.isNegative ())
    {
      // No time limit requested
      return areAllReady (aProviders);
    }

    final Future <Boolean> aFuture = EXECUTOR.submit ( () -> Boolean.valueOf (areAllReady (aProviders)));
    try
    {
      return aFuture.get (aTimeout.toMillis (), TimeUnit.MILLISECONDS).booleanValue ();
    }
    catch (final TimeoutException ex)
    {
      // Let the check run to its end - it is not re-used anyway
      aFuture.cancel (true);
      LOGGER.warn ("The readiness check did not finish within " +
                   aTimeout.toString () +
                   " - the SMP is considered to be not ready");
      return false;
    }
    catch (final InterruptedException ex)
    {
      Thread.currentThread ().interrupt ();
      return false;
    }
    catch (final ExecutionException ex)
    {
      LOGGER.warn ("The readiness check failed with an exception", ex.getCause ());
      return false;
    }
  }

  /**
   * @return <code>true</code> if all registered backend checks report ready within the configured
   *         timeout. If no check is registered at all, the SMP is considered to be not ready,
   *         because every backend - including the XML backend - must provide an implementation of
   *         {@link ISMPReadyProviderExtensionSPI}.
   * @see SMPServerConfiguration#getReadyTimeout()
   */
  public static boolean isReady ()
  {
    return areAllReady (LIST, SMPServerConfiguration.getReadyTimeout ());
  }
}
