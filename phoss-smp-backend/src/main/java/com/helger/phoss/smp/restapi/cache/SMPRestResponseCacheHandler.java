/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.restapi.cache;

import java.util.concurrent.atomic.AtomicLong;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.functional.IThrowingSupplier;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Wraps an {@link ISMPRestResponseCacheSPI} implementation and adds the logic that is independent
 * of the storage: errors of the implementation never break the REST API, and a response that was
 * created while a modification happened is never cached.<br>
 * The latter is achieved with a generation counter, that is incremented with every invalidation. A
 * created response is only stored, if no invalidation happened since the creation started. Storing
 * and invalidating are executed under the same lock, so that a response created from outdated data
 * can never survive an invalidation.
 *
 * @author Philip Helger
 * @since 8.6.1
 */
@ThreadSafe
public final class SMPRestResponseCacheHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPRestResponseCacheHandler.class);

  private final SimpleLock m_aLock = new SimpleLock ();
  private final AtomicLong m_aGeneration = new AtomicLong (0);
  private final ISMPRestResponseCacheSPI m_aImpl;

  /**
   * Constructor
   *
   * @param aImpl
   *        The storage implementation to use. May not be <code>null</code>.
   */
  public SMPRestResponseCacheHandler (@NonNull final ISMPRestResponseCacheSPI aImpl)
  {
    ValueEnforcer.notNull (aImpl, "Impl");
    m_aImpl = aImpl;
  }

  @Nullable
  private byte [] _get (@NonNull final SMPRestResponseCacheKey aKey)
  {
    try
    {
      return m_aImpl.get (aKey);
    }
    catch (final RuntimeException ex)
    {
      // Never let a caching problem break the REST API
      LOGGER.warn ("Failed to read " + aKey + " from the REST API response cache", ex);
      return null;
    }
  }

  private void _invalidate (@NonNull final String sWhat, @NonNull final Runnable aRemover)
  {
    m_aLock.locked ( () -> {
      // Ensure, that responses created before this invalidation are not stored afterwards
      m_aGeneration.incrementAndGet ();
      try
      {
        aRemover.run ();
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Invalidated the REST API response cache of " + sWhat);
      }
      catch (final RuntimeException ex)
      {
        LOGGER.warn ("Failed to invalidate the REST API response cache of " + sWhat, ex);
      }
    });
  }

  /**
   * @return The underlying storage implementation. Never <code>null</code>.
   */
  @NonNull
  public ISMPRestResponseCacheSPI getImplementation ()
  {
    return m_aImpl;
  }

  /**
   * Get the cached response or create it and put it in the cache.
   *
   * @param <EX>
   *        The exception type that may be thrown by the supplier
   * @param aKey
   *        The cache key. May not be <code>null</code>.
   * @param aSupplier
   *        The supplier that creates the response, if it is not cached. May not be
   *        <code>null</code>. Exceptions thrown are propagated and nothing is cached.
   * @return The cached or newly created response. Only <code>null</code> if the supplier returned
   *         <code>null</code>.
   * @throws EX
   *         If the supplier throws it
   */
  public <EX extends Exception> byte @Nullable [] getOrCreate (@NonNull final SMPRestResponseCacheKey aKey,
                                                               @NonNull final IThrowingSupplier <byte [], EX> aSupplier) throws EX
  {
    ValueEnforcer.notNull (aKey, "Key");
    ValueEnforcer.notNull (aSupplier, "Supplier");

    final byte [] aCached = _get (aKey);
    if (aCached != null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Serving " + aKey + " from the REST API response cache");
      return aCached;
    }

    final long nGenerationBefore = m_aGeneration.get ();
    final byte [] ret = aSupplier.get ();
    if (ret != null)
    {
      m_aLock.locked ( () -> {
        if (m_aGeneration.get () == nGenerationBefore)
        {
          try
          {
            m_aImpl.put (aKey, ret);
          }
          catch (final RuntimeException ex)
          {
            // Never let a caching problem break the REST API
            LOGGER.warn ("Failed to write " + aKey + " to the REST API response cache", ex);
          }
        }
        else
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Not caching " + aKey + " because the cache was invalidated in the meantime");
        }
      });
    }
    return ret;
  }

  /**
   * Remove all cached responses of the provided participant.
   *
   * @param aParticipantID
   *        The participant to invalidate. May not be <code>null</code>.
   */
  public void invalidateParticipant (@NonNull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final String sParticipantID = aParticipantID.getURIEncoded ();
    _invalidate ("participant '" + sParticipantID + "'", () -> m_aImpl.removeAllOfParticipant (sParticipantID));
  }

  /**
   * Remove all cached responses.
   */
  public void invalidateAll ()
  {
    _invalidate ("all participants", m_aImpl::removeAll);
  }

  /**
   * Shutdown the underlying implementation.
   */
  public void shutdown ()
  {
    try
    {
      m_aImpl.shutdown ();
    }
    catch (final RuntimeException ex)
    {
      LOGGER.warn ("Failed to shutdown the REST API response cache implementation " + m_aImpl, ex);
    }
  }
}
