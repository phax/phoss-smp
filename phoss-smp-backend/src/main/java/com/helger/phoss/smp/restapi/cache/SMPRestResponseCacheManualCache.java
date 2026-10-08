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

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.cache.eviction.CacheEvictionScheduler;
import com.helger.cache.impl.ManualCache;

/**
 * The default implementation of {@link ISMPRestResponseCacheSPI}, based on an in-memory
 * {@link ManualCache}. It is used, if no other implementation is provided via the SPI. The cache is
 * local to the SMP instance - in a multi-instance setup, a modification on one instance is visible
 * on the other instances at the latest after the configured time to live.
 *
 * @author Philip Helger
 * @since 8.6.1
 */
@ThreadSafe
public class SMPRestResponseCacheManualCache implements ISMPRestResponseCacheSPI
{
  public static final String CACHE_NAME = "phoss.smp.rest.response";
  private static final Duration EVICTION_INTERVAL = Duration.ofMinutes (1);

  private final ManualCache <SMPRestResponseCacheKey, byte []> m_aCache;

  /**
   * Constructor
   *
   * @param aTTL
   *        The time to live of each cache entry. May not be <code>null</code> and must be
   *        positive.
   * @param nMaxItems
   *        The maximum number of cached payloads. Values &le; 0 mean unlimited.
   */
  public SMPRestResponseCacheManualCache (@NonNull final Duration aTTL, final int nMaxItems)
  {
    ValueEnforcer.notNull (aTTL, "TTL");
    ValueEnforcer.isTrue (!aTTL.isZero () && !aTTL.isNegative (), "TTL must be positive");
    m_aCache = ManualCache.<SMPRestResponseCacheKey, byte []> builder ()
                          .name (CACHE_NAME)
                          .maxSize (nMaxItems)
                          .expireAfterWrite (aTTL)
                          .evictionInterval (EVICTION_INTERVAL)
                          .build ();
  }

  public byte @Nullable [] get (@NonNull final SMPRestResponseCacheKey aKey)
  {
    return m_aCache.getFromCache (aKey);
  }

  public void put (@NonNull final SMPRestResponseCacheKey aKey, final byte @NonNull [] aPayload)
  {
    m_aCache.putInCache (aKey, aPayload);
  }

  public void removeAllOfParticipant (@NonNull @Nonempty final String sParticipantID)
  {
    m_aCache.removeFromCacheIf (x -> x.getParticipantID ().equals (sParticipantID));
  }

  public void removeAll ()
  {
    m_aCache.clearCache ();
  }

  @Override
  public void shutdown ()
  {
    CacheEvictionScheduler.getInstance ().unregister (m_aCache);
    m_aCache.clearCache ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Cache", m_aCache).getToString ();
  }
}
