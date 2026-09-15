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
package com.helger.phoss.smp.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.MultiLevelCacheBuilder;
import com.alicp.jetcache.SimpleCacheManager;
import com.alicp.jetcache.embedded.CaffeineCacheBuilder;
import com.alicp.jetcache.redis.lettuce.LettuceBroadcastManager;
import com.alicp.jetcache.redis.lettuce.LettuceConnectionManager;
import com.alicp.jetcache.redis.lettuce.RedisLettuceCacheBuilder;
import com.alicp.jetcache.redis.lettuce.RedisLettuceCacheConfig;
import com.alicp.jetcache.support.CacheNotifyMonitor;
import com.alicp.jetcache.support.Fastjson2KeyConvertor;
import com.alicp.jetcache.support.JavaValueDecoder;
import com.alicp.jetcache.support.JavaValueEncoder;
import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.string.StringHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;

/**
 * A cache for the responses of the read-only SMP REST API endpoints
 * <code>GET /{ServiceGroupId}</code> and
 * <code>GET /{ServiceGroupId}/services/{DocumentTypeId}</code>. Its main purpose is to avoid that
 * every single query hits the backend (e.g. the database).<br>
 * The cache is built on top of JetCache and uses a two level setup, if a Redis connection is
 * configured: an in-memory Caffeine cache as the first level and Redis as the shared second level.
 * Modifications on one SMP instance are propagated to the in-memory caches of all other instances
 * via a Redis Pub/Sub channel. If no Redis connection is configured, only the in-memory cache is
 * used.<br>
 * All cached payloads of a single participant are grouped into a single cache entry. That way a
 * single removal is sufficient to invalidate everything that is cached for a participant - no
 * matter how many document types or public URLs are involved. See
 * {@link #invalidateParticipant(IParticipantIdentifier)}.
 *
 * @author Philip Helger
 * @since 80.4.3-stormware
 */
@ThreadSafe
public final class SMPRestCache extends AbstractGlobalSingleton
{
  /** The JetCache name of the cache holding the "GET service group" responses */
  public static final String CACHE_NAME_SERVICE_GROUP = "SMPRestServiceGroup";
  /** The JetCache name of the cache holding the "GET service metadata" responses */
  public static final String CACHE_NAME_SERVICE_METADATA = "SMPRestServiceMetadata";

  /**
   * The maximum number of payloads that are cached per participant. This limits the memory consumed
   * by a single cache entry for participants with a huge amount of document types.
   */
  public static final int MAX_PAYLOADS_PER_PARTICIPANT = 200;

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPRestCache.class);

  private SimpleCacheManager m_aCacheMgr;
  private RedisClient m_aRedisClient;
  private LettuceBroadcastManager m_aBroadcastMgr;
  private Cache <String, HashMap <String, byte []>> m_aCacheSG;
  private Cache <String, HashMap <String, byte []>> m_aCacheSM;
  private boolean m_bEnabled;

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public SMPRestCache ()
  {
    _init ();
  }

  @NonNull
  public static SMPRestCache getInstance ()
  {
    return getGlobalSingleton (SMPRestCache.class);
  }

  /**
   * @return The cache instance to be used, or <code>null</code> if the REST API response caching is
   *         disabled or could not be initialized.
   */
  @Nullable
  private static SMPRestCache _getActiveInstance ()
  {
    if (!SMPServerConfiguration.isRestCacheEnabled ())
      return null;

    try
    {
      final SMPRestCache ret = getInstance ();
      return ret.m_bEnabled ? ret : null;
    }
    catch (final RuntimeException ex)
    {
      // E.g. no global scope available
      return null;
    }
  }

  /**
   * @return The already instantiated cache instance or <code>null</code> if it was never
   *         instantiated. This never creates a new instance.
   */
  @Nullable
  private static SMPRestCache _getExistingInstance ()
  {
    final SMPRestCache ret = getGlobalSingletonIfInstantiated (SMPRestCache.class);
    return ret != null && ret.m_bEnabled ? ret : null;
  }

  @Nullable
  private static RedisURI _createRedisURI ()
  {
    final String sUri = SMPServerConfiguration.getRestCacheRedisUri ();
    final RedisURI ret;
    if (StringHelper.isNotEmpty (sUri))
      ret = RedisURI.create (sUri);
    else
    {
      final String sHost = SMPServerConfiguration.getRestCacheRedisHost ();
      if (StringHelper.isEmpty (sHost))
      {
        // No Redis configured - use in-memory caching only
        return null;
      }

      final RedisURI.Builder aBuilder = RedisURI.Builder.redis (sHost, SMPServerConfiguration.getRestCacheRedisPort ())
                                                        .withDatabase (SMPServerConfiguration.getRestCacheRedisDatabase ())
                                                        .withSsl (SMPServerConfiguration.isRestCacheRedisSSL ());
      final char [] aPassword = SMPServerConfiguration.getRestCacheRedisPassword ();
      if (aPassword != null && aPassword.length > 0)
      {
        final String sUser = SMPServerConfiguration.getRestCacheRedisUser ();
        if (StringHelper.isNotEmpty (sUser))
          aBuilder.withAuthentication (sUser, aPassword);
        else
          aBuilder.withPassword (aPassword);
      }
      ret = aBuilder.build ();
    }
    ret.setTimeout (SMPServerConfiguration.getRestCacheRedisTimeout ());
    return ret;
  }

  private void _init ()
  {
    if (!SMPServerConfiguration.isRestCacheEnabled ())
    {
      LOGGER.info ("The SMP REST API response cache is disabled");
      return;
    }

    final Duration aTTL = SMPServerConfiguration.getRestCacheTTL ();
    final int nMaxItems = SMPServerConfiguration.getRestCacheMaxItems ();
    // Include the REST type, so that a configuration change never delivers payloads created for
    // the previously configured REST type
    final String sKeyPrefix = SMPServerConfiguration.getRestCacheKeyPrefix () +
                              SMPServerConfiguration.getRESTType ().getID () +
                              ":";
    final String sBroadcastChannel = SMPServerConfiguration.getRestCacheBroadcastChannel ();

    RedisURI aRedisURI = null;
    try
    {
      m_aCacheMgr = new SimpleCacheManager ();

      aRedisURI = _createRedisURI ();
      if (aRedisURI != null)
      {
        m_aRedisClient = RedisClient.create (aRedisURI);

        // Central broadcast manager, that invalidates the in-memory caches of all other SMP
        // instances
        final RedisLettuceCacheConfig <Object, Object> aBroadcastConfig = new RedisLettuceCacheConfig <> ();
        aBroadcastConfig.setRedisClient (m_aRedisClient);
        aBroadcastConfig.setKeyPrefix (sKeyPrefix);
        aBroadcastConfig.setKeyConvertor (Fastjson2KeyConvertor.INSTANCE);
        aBroadcastConfig.setValueEncoder (JavaValueEncoder.INSTANCE);
        aBroadcastConfig.setValueDecoder (JavaValueDecoder.INSTANCE);
        aBroadcastConfig.setBroadcastChannel (sBroadcastChannel);
        aBroadcastConfig.setPubSubConnection (m_aRedisClient.connectPubSub (new ByteArrayCodec ()));
        aBroadcastConfig.setConnectionManager (LettuceConnectionManager.defaultManager ());

        m_aBroadcastMgr = new LettuceBroadcastManager (m_aCacheMgr, aBroadcastConfig);
        m_aBroadcastMgr.startSubscribe ();
        // Must be registered before the caches, because the CacheNotifyMonitor resolves it eagerly
        m_aCacheMgr.putBroadcastManager (m_aBroadcastMgr);
      }

      m_aCacheSG = _createCache (CACHE_NAME_SERVICE_GROUP, sKeyPrefix + "sg:", sBroadcastChannel, aTTL, nMaxItems);
      m_aCacheSM = _createCache (CACHE_NAME_SERVICE_METADATA, sKeyPrefix + "sm:", sBroadcastChannel, aTTL, nMaxItems);

      m_bEnabled = true;
      LOGGER.info ("Successfully initialized the SMP REST API response cache with a TTL of " +
                   aTTL +
                   ", a maximum of " +
                   nMaxItems +
                   " in-memory entries per cache and " +
                   (aRedisURI != null ? "Redis at " + aRedisURI.getHost () + ":" + aRedisURI.getPort ()
                                      : "no Redis (in-memory only)"));
    }
    catch (final RuntimeException ex)
    {
      LOGGER.error ("Failed to initialize the SMP REST API response cache. Caching stays disabled.", ex);
      _shutdown ();
    }
  }

  @NonNull
  private Cache <String, HashMap <String, byte []>> _createCache (@NonNull @Nonempty final String sCacheName,
                                                                  @NonNull @Nonempty final String sKeyPrefix,
                                                                  @NonNull @Nonempty final String sBroadcastChannel,
                                                                  @NonNull final Duration aTTL,
                                                                  final int nMaxItems)
  {
    final long nTTLMillis = aTTL.toMillis ();

    final Cache <String, HashMap <String, byte []>> aLocalCache = CaffeineCacheBuilder.createCaffeineCacheBuilder ()
                                                                                      .limit (nMaxItems)
                                                                                      .expireAfterWrite (nTTLMillis,
                                                                                                         TimeUnit.MILLISECONDS)
                                                                                      .keyConvertor (Fastjson2KeyConvertor.INSTANCE)
                                                                                      .buildCache ();

    final Cache <String, HashMap <String, byte []>> ret;
    if (m_aRedisClient == null)
    {
      // In-memory only
      ret = aLocalCache;
    }
    else
    {
      final Cache <String, HashMap <String, byte []>> aRemoteCache = RedisLettuceCacheBuilder.createRedisLettuceCacheBuilder ()
                                                                                             .redisClient (m_aRedisClient)
                                                                                             .keyConvertor (Fastjson2KeyConvertor.INSTANCE)
                                                                                             .valueEncoder (JavaValueEncoder.INSTANCE)
                                                                                             .valueDecoder (JavaValueDecoder.INSTANCE)
                                                                                             .keyPrefix (sKeyPrefix)
                                                                                             .expireAfterWrite (nTTLMillis,
                                                                                                                TimeUnit.MILLISECONDS)
                                                                                             .broadcastChannel (sBroadcastChannel)
                                                                                             .buildCache ();

      ret = MultiLevelCacheBuilder.createMultiLevelCacheBuilder ()
                                  .addCache (aLocalCache, aRemoteCache)
                                  .expireAfterWrite (nTTLMillis, TimeUnit.MILLISECONDS)
                                  .buildCache ();
    }

    // Must happen before the monitor is used, because the notification handler resolves the cache
    // by name
    m_aCacheMgr.putCache (sCacheName, ret);

    if (m_aBroadcastMgr != null)
    {
      // Publish all local modifications to the other SMP instances
      ret.config ().getMonitors ().add (new CacheNotifyMonitor (m_aCacheMgr, sCacheName));
    }
    return ret;
  }

  private void _shutdown ()
  {
    m_bEnabled = false;
    m_aCacheSG = null;
    m_aCacheSM = null;

    if (m_aBroadcastMgr != null)
    {
      try
      {
        m_aBroadcastMgr.close ();
      }
      catch (final Exception ex)
      {
        LOGGER.warn ("Failed to close the SMP REST API cache broadcast manager", ex);
      }
      m_aBroadcastMgr = null;
    }

    if (m_aCacheMgr != null)
    {
      try
      {
        m_aCacheMgr.close ();
      }
      catch (final Exception ex)
      {
        LOGGER.warn ("Failed to close the SMP REST API cache manager", ex);
      }
      m_aCacheMgr = null;
    }

    if (m_aRedisClient != null)
    {
      try
      {
        LettuceConnectionManager.defaultManager ().removeAndClose (m_aRedisClient);
        m_aRedisClient.shutdown ();
      }
      catch (final Exception ex)
      {
        LOGGER.warn ("Failed to close the SMP REST API cache Redis client", ex);
      }
      m_aRedisClient = null;
    }
  }

  @Override
  protected void onDestroy (@NonNull final IScope aScopeInDestruction)
  {
    m_aRWLock.writeLocked (this::_shutdown);
  }

  @Nullable
  private byte [] _get (@Nullable final Cache <String, HashMap <String, byte []>> aCache,
                        @NonNull final String sCacheKey,
                        @NonNull final String sPayloadKey)
  {
    if (aCache == null)
      return null;

    try
    {
      final Map <String, byte []> aPayloads = aCache.get (sCacheKey);
      return aPayloads == null ? null : aPayloads.get (sPayloadKey);
    }
    catch (final RuntimeException ex)
    {
      // Never let a caching problem break the REST API
      LOGGER.warn ("Failed to read '" + sCacheKey + "' from the SMP REST API response cache", ex);
      return null;
    }
  }

  private void _put (@Nullable final Cache <String, HashMap <String, byte []>> aCache,
                     @NonNull final String sCacheKey,
                     @NonNull final String sPayloadKey,
                     final byte @NonNull [] aPayload)
  {
    if (aCache == null)
      return;

    try
    {
      final Map <String, byte []> aOld = aCache.get (sCacheKey);
      // Copy on write, so that a map that is concurrently read is never modified
      final HashMap <String, byte []> aNew = aOld == null ? new HashMap <> () : new HashMap <> (aOld);
      if (!aNew.containsKey (sPayloadKey) && aNew.size () >= MAX_PAYLOADS_PER_PARTICIPANT)
      {
        // Don't let a single cache entry grow indefinitely
        return;
      }
      aNew.put (sPayloadKey, aPayload);
      aCache.put (sCacheKey, aNew);
    }
    catch (final RuntimeException ex)
    {
      // Never let a caching problem break the REST API
      LOGGER.warn ("Failed to write '" + sCacheKey + "' to the SMP REST API response cache", ex);
    }
  }

  /**
   * @param aParticipantID
   *        The participant to query. May not be <code>null</code>.
   * @param sServiceGroupHref
   *        The absolute HREF of the service group as it is contained in the response. This is part
   *        of the cache key, because the response depends on the public URL of the SMP. May neither
   *        be <code>null</code> nor empty.
   * @return The cached XML representation of the service group or <code>null</code> if it is not
   *         cached.
   */
  @Nullable
  public static byte [] getServiceGroupPayload (@NonNull final IParticipantIdentifier aParticipantID,
                                                @NonNull @Nonempty final String sServiceGroupHref)
  {
    final SMPRestCache aCache = _getActiveInstance ();
    return aCache == null ? null : aCache._get (aCache.m_aCacheSG,
                                                aParticipantID.getURIEncoded (),
                                                sServiceGroupHref);
  }

  /**
   * Add the XML representation of a service group to the cache.
   *
   * @param aParticipantID
   *        The participant of the response. May not be <code>null</code>.
   * @param sServiceGroupHref
   *        The absolute HREF of the service group. May neither be <code>null</code> nor empty.
   * @param aPayload
   *        The XML bytes to be cached. May not be <code>null</code>.
   */
  public static void setServiceGroupPayload (@NonNull final IParticipantIdentifier aParticipantID,
                                             @NonNull @Nonempty final String sServiceGroupHref,
                                             final byte @NonNull [] aPayload)
  {
    final SMPRestCache aCache = _getActiveInstance ();
    if (aCache != null)
      aCache._put (aCache.m_aCacheSG, aParticipantID.getURIEncoded (), sServiceGroupHref, aPayload);
  }

  /**
   * @param aParticipantID
   *        The participant to query. May not be <code>null</code>.
   * @param aDocTypeID
   *        The document type to query. May not be <code>null</code>.
   * @return The cached signed XML representation of the service metadata or <code>null</code> if it
   *         is not cached.
   */
  @Nullable
  public static byte [] getServiceMetadataPayload (@NonNull final IParticipantIdentifier aParticipantID,
                                                   @NonNull final IDocumentTypeIdentifier aDocTypeID)
  {
    final SMPRestCache aCache = _getActiveInstance ();
    return aCache == null ? null : aCache._get (aCache.m_aCacheSM,
                                                aParticipantID.getURIEncoded (),
                                                aDocTypeID.getURIEncoded ());
  }

  /**
   * Add the signed XML representation of a service metadata to the cache.
   *
   * @param aParticipantID
   *        The participant of the response. May not be <code>null</code>.
   * @param aDocTypeID
   *        The document type of the response. May not be <code>null</code>.
   * @param aPayload
   *        The signed XML bytes to be cached. May not be <code>null</code>.
   */
  public static void setServiceMetadataPayload (@NonNull final IParticipantIdentifier aParticipantID,
                                                @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                final byte @NonNull [] aPayload)
  {
    final SMPRestCache aCache = _getActiveInstance ();
    if (aCache != null)
      aCache._put (aCache.m_aCacheSM, aParticipantID.getURIEncoded (), aDocTypeID.getURIEncoded (), aPayload);
  }

  /**
   * Remove everything that is cached for the provided participant. This covers the service group
   * response as well as the service metadata responses of all document types of that participant.
   * If a Redis connection is configured, the removal is propagated to all other SMP instances.
   *
   * @param aParticipantID
   *        The participant to be invalidated. May be <code>null</code> in which case nothing
   *        happens.
   */
  public static void invalidateParticipant (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return;

    // Don't instantiate the cache, just to invalidate something
    final SMPRestCache aCache = _getExistingInstance ();
    if (aCache == null)
      return;

    final String sCacheKey = aParticipantID.getURIEncoded ();
    try
    {
      if (aCache.m_aCacheSG != null)
        aCache.m_aCacheSG.remove (sCacheKey);
      if (aCache.m_aCacheSM != null)
        aCache.m_aCacheSM.remove (sCacheKey);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Invalidated the SMP REST API response cache of '" + sCacheKey + "'");
    }
    catch (final RuntimeException ex)
    {
      LOGGER.warn ("Failed to invalidate the SMP REST API response cache of '" + sCacheKey + "'", ex);
    }
  }
}
