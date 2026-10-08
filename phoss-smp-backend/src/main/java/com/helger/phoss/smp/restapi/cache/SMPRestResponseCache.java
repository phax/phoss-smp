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

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.UsedViaReflection;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.exception.InitializationException;
import com.helger.base.functional.IThrowingSupplier;
import com.helger.base.spi.ServiceLoaderHelper;
import com.helger.base.string.StringImplode;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * The cache for the responses of the read-only REST API endpoints <code>GET /{ServiceGroupId}</code>
 * and <code>GET /{ServiceGroupId}/services/{DocumentTypeId}</code>. It avoids that every single
 * query hits the backend and that every service metadata response needs to be signed again.<br>
 * The cache is opt-in (see {@link SMPServerConfiguration#isRestCacheEnabled()}). The storage can be
 * replaced via {@link ISMPRestResponseCacheSPI} - by default {@link SMPRestResponseCacheManualCache}
 * is used.<br>
 * All cached responses of a participant are invalidated as soon as anything of that participant is
 * modified on this SMP instance (see {@link SMPRestResponseCacheInvalidationCallback}).
 *
 * @author Philip Helger
 * @since 8.6.1
 */
public final class SMPRestResponseCache extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPRestResponseCache.class);

  // null if caching is disabled
  private SMPRestResponseCacheHandler m_aHandler;

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public SMPRestResponseCache ()
  {}

  /**
   * Select the storage implementation to be used.
   *
   * @param aSPIImpls
   *        All implementations found via the SPI. May not be <code>null</code>.
   * @return The only provided implementation or a new {@link SMPRestResponseCacheManualCache} if no
   *         implementation is provided. Never <code>null</code>.
   * @throws InitializationException
   *         If more than one implementation is provided
   */
  @NonNull
  @VisibleForTesting
  static ISMPRestResponseCacheSPI selectImplementation (@NonNull final List <ISMPRestResponseCacheSPI> aSPIImpls)
  {
    if (aSPIImpls.isEmpty ())
      return new SMPRestResponseCacheManualCache (SMPServerConfiguration.getRestCacheTTL (),
                                                  SMPServerConfiguration.getRestCacheMaxItems ());
    if (aSPIImpls.size () == 1)
      return aSPIImpls.get (0);

    throw new InitializationException ("Found " +
                                       aSPIImpls.size () +
                                       " implementations of " +
                                       ISMPRestResponseCacheSPI.class.getSimpleName () +
                                       " but at most one is allowed: " +
                                       StringImplode.getImplodedMapped (", ",
                                                                        aSPIImpls,
                                                                        x -> x.getClass ().getName ()));
  }

  @Override
  protected void onAfterInstantiation (@NonNull final IScope aScope)
  {
    if (!SMPServerConfiguration.isRestCacheEnabled ())
    {
      LOGGER.info ("The REST API response cache is disabled");
      return;
    }

    final ISMPRestResponseCacheSPI aImpl = selectImplementation (ServiceLoaderHelper.getAllSPIImplementations (ISMPRestResponseCacheSPI.class));
    m_aHandler = new SMPRestResponseCacheHandler (aImpl);
    LOGGER.info ("The REST API response cache is enabled with a TTL of " +
                 SMPServerConfiguration.getRestCacheTTL () +
                 " and a maximum of " +
                 SMPServerConfiguration.getRestCacheMaxItems () +
                 " items, using " +
                 aImpl.getClass ().getName ());
  }

  @Override
  protected void onBeforeDestroy (@NonNull final IScope aScopeToBeDestroyed)
  {
    if (m_aHandler != null)
      m_aHandler.shutdown ();
  }

  @NonNull
  public static SMPRestResponseCache getInstance ()
  {
    return getGlobalSingleton (SMPRestResponseCache.class);
  }

  /**
   * @return The handler of the cache or <code>null</code> if caching is disabled.
   */
  @Nullable
  public SMPRestResponseCacheHandler getHandler ()
  {
    return m_aHandler;
  }

  /**
   * @return <code>true</code> if the REST API response cache is enabled in the configuration. Can
   *         be used to avoid creating cache keys if caching is disabled anyway.
   */
  public static boolean isEnabled ()
  {
    return SMPServerConfiguration.isRestCacheEnabled ();
  }

  /**
   * Get the cached response or create it and put it in the cache.
   *
   * @param <EX>
   *        The exception type that may be thrown by the supplier
   * @param aKey
   *        The cache key. May be <code>null</code> in which case the response is created without
   *        caching.
   * @param aSupplier
   *        The supplier that creates the response, if it is not cached. May not be
   *        <code>null</code>.
   * @return The cached or newly created response. Only <code>null</code> if the supplier returned
   *         <code>null</code>.
   * @throws EX
   *         If the supplier throws it
   */
  public static <EX extends Exception> byte @Nullable [] getOrCreate (@Nullable final SMPRestResponseCacheKey aKey,
                                                                      @NonNull final IThrowingSupplier <byte [], EX> aSupplier) throws EX
  {
    if (aKey != null)
    {
      final SMPRestResponseCacheHandler aHandler = getInstance ().m_aHandler;
      if (aHandler != null)
        return aHandler.getOrCreate (aKey, aSupplier);
    }
    return aSupplier.get ();
  }

  /**
   * Remove all cached responses of the provided participant. This never instantiates the cache.
   *
   * @param aParticipantID
   *        The participant to invalidate. May be <code>null</code> in which case nothing happens.
   */
  public static void invalidateParticipant (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID != null)
    {
      final SMPRestResponseCache aInstance = getGlobalSingletonIfInstantiated (SMPRestResponseCache.class);
      if (aInstance != null && aInstance.m_aHandler != null)
        aInstance.m_aHandler.invalidateParticipant (aParticipantID);
    }
  }

  /**
   * Remove all cached responses. This is needed for modifications that affect an unknown number of
   * participants, or if the signing key changed. This never instantiates the cache.
   */
  public static void invalidateAll ()
  {
    final SMPRestResponseCache aInstance = getGlobalSingletonIfInstantiated (SMPRestResponseCache.class);
    if (aInstance != null && aInstance.m_aHandler != null)
      aInstance.m_aHandler.invalidateAll ();
  }
}
