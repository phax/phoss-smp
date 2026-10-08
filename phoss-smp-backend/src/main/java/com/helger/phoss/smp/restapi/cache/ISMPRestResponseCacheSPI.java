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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIInterface;

/**
 * The SPI interface for the storage of the REST API response cache. It only covers storing and
 * removing the payloads. Deciding whether caching is enabled, building the keys, protecting against
 * caching outdated responses and invalidating on modifications is done by
 * {@link SMPRestResponseCache} and is therefore available for all implementations.<br>
 * If no implementation is found via the {@link java.util.ServiceLoader},
 * {@link SMPRestResponseCacheManualCache} is used. If exactly one implementation is found, it is
 * used instead. More than one implementation is an error.<br>
 * Implementations must be thread-safe. They should honour the time to live
 * ({@link com.helger.phoss.smp.config.SMPServerConfiguration#getRestCacheTTL()}) and the maximum
 * number of items ({@link com.helger.phoss.smp.config.SMPServerConfiguration#getRestCacheMaxItems()})
 * from the configuration.
 *
 * @author Philip Helger
 * @since 8.6.1
 */
@IsSPIInterface
public interface ISMPRestResponseCacheSPI
{
  /**
   * Get a cached payload.
   *
   * @param aKey
   *        The key to query. May not be <code>null</code>.
   * @return <code>null</code> if no payload is cached for the key or if it is expired. The returned
   *         array must not be modified by the caller.
   */
  byte @Nullable [] get (@NonNull SMPRestResponseCacheKey aKey);

  /**
   * Add or replace a payload in the cache.
   *
   * @param aKey
   *        The key to use. May not be <code>null</code>.
   * @param aPayload
   *        The payload to be cached. May not be <code>null</code>. The array is not modified
   *        afterwards, so it may be stored as is.
   */
  void put (@NonNull SMPRestResponseCacheKey aKey, byte @NonNull [] aPayload);

  /**
   * Remove all cached payloads of the provided participant.
   *
   * @param sParticipantID
   *        The URI encoded participant identifier as returned by
   *        {@link SMPRestResponseCacheKey#getParticipantID()}. May neither be <code>null</code> nor
   *        empty.
   */
  void removeAllOfParticipant (@NonNull @Nonempty String sParticipantID);

  /**
   * Remove all cached payloads.
   */
  void removeAll ();

  /**
   * Release all resources held by this implementation. Called once when the SMP is shut down.
   */
  default void shutdown ()
  {}
}
