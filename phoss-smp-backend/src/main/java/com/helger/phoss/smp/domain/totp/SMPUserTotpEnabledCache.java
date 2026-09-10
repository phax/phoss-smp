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
package com.helger.phoss.smp.domain.totp;

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.cache.impl.ProviderCache;
import com.helger.phoss.smp.domain.SMPMetaManager;

/**
 * A JVM wide cache from user ID to the "is TOTP enabled" state. This avoids a backend roundtrip on
 * every single request of the <code>/secure</code> application. Cached entries expire automatically
 * after {@link #TIME_TO_LIVE}. Additionally all modifying methods of {@link ISMPUserTotpManager}
 * invalidate the respective cache entry, so that changes performed in this JVM are effective
 * immediately.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
@ThreadSafe
public final class SMPUserTotpEnabledCache
{
  /** The maximum age of a cached entry. */
  public static final Duration TIME_TO_LIVE = Duration.ofMinutes (5);

  private static final String CACHE_NAME = "phoss.smp.usertotp.enabled";

  private static final ProviderCache <String, Boolean> CACHE = ProviderCache.<String, Boolean> builder ()
                                                                            .name (CACHE_NAME)
                                                                            .expireAfterWrite (TIME_TO_LIVE)
                                                                            .valueProvider (SMPUserTotpEnabledCache::_resolveTotpEnabled)
                                                                            .build ();

  private SMPUserTotpEnabledCache ()
  {}

  @NonNull
  private static Boolean _resolveTotpEnabled (@NonNull final String sUserID)
  {
    final ISMPUserTotpManager aTotpMgr = SMPMetaManager.getUserTotpMgr ();
    return Boolean.valueOf (aTotpMgr != null && aTotpMgr.isTotpEnabled (sUserID));
  }

  /**
   * Check if the provided user has a confirmed TOTP enrollment, using the cached state.
   *
   * @param sUserID
   *        The ID of the user in question. May be <code>null</code>.
   * @return <code>true</code> if TOTP is enabled for that user.
   */
  public static boolean isTotpEnabled (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return false;

    final Boolean aEnabled = CACHE.getFromCache (sUserID);
    return aEnabled != null && aEnabled.booleanValue ();
  }

  /**
   * Remove the cached state of a single user. This must be called, whenever the TOTP enrollment of
   * that user is created, enabled, disabled or deleted.
   *
   * @param sUserID
   *        The ID of the user in question. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if something was removed from the cache.
   */
  @NonNull
  public static EChange clearCache (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    return CACHE.removeFromCache (sUserID);
  }

  /**
   * Remove the cached state of all users.
   *
   * @return {@link EChange#CHANGED} if something was removed from the cache.
   */
  @NonNull
  public static EChange clearCache ()
  {
    return CACHE.clearCache ();
  }
}
