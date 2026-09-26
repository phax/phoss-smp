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
package com.helger.phoss.smp.smlhook;

import java.time.Duration;
import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.string.StringHelper;
import com.helger.datetime.helper.PDTFactory;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.ManageServiceMetadataServiceCaller;
import com.helger.peppol.smlclient.SMLExceptionHelper;
import com.helger.peppol.smlclient.smp.NotFoundFault;
import com.helger.peppol.smlclient.smp.PublisherEndpointType;
import com.helger.peppol.smlclient.smp.ServiceMetadataPublisherServiceType;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.settings.ISMPSettings;

/**
 * A process wide cache for the registration of this SMP at the SML, as returned by the
 * <code>Read()</code> operation of chapter 3.1.3.2 of the SML specification.<br>
 * The SML is only queried lazily, when the cached entry is older than {@link #CACHE_DURATION}, so
 * that a page showing this information does not perform a remote call on every rendering. There is
 * deliberately no background job refreshing this - the value is only ever determined on demand.
 *
 * @author Philip Helger
 * @since 8.5.1
 */
@ThreadSafe
public final class SMLRegistrationCache
{
  /** The fixed duration for which a determined result is reused */
  public static final Duration CACHE_DURATION = Duration.ofHours (1);

  private static final Logger LOGGER = LoggerFactory.getLogger (SMLRegistrationCache.class);
  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();

  @GuardedBy ("RW_LOCK")
  private static String s_sCacheKey;
  @GuardedBy ("RW_LOCK")
  private static SMLRegistrationCheckResult s_aResult;

  private SMLRegistrationCache ()
  {}

  @NonNull
  @Nonempty
  private static String _getCacheKey (@NonNull final ISMLInfo aSMLInfo, @NonNull @Nonempty final String sSMPID)
  {
    // The result is only valid for the combination of SML and SMP ID it was determined for
    return aSMLInfo.getID () + "|" + sSMPID;
  }

  @NonNull
  private static SMLRegistrationCheckResult _readFromSML (@NonNull final ISMLInfo aSMLInfo,
                                                          @NonNull @Nonempty final String sSMPID)
  {
    try
    {
      final ManageServiceMetadataServiceCaller aCaller = SmpSmlHelper.createSMLCallerSMP (aSMLInfo);
      final ServiceMetadataPublisherServiceType aSMPService = aCaller.read (sSMPID);

      final PublisherEndpointType aEndpoint = aSMPService.getPublisherEndpoint ();
      final String sLogicalAddress = aEndpoint == null ? null : aEndpoint.getLogicalAddress ();
      final String sPhysicalAddress = aEndpoint == null ? null : aEndpoint.getPhysicalAddress ();

      LOGGER.info ("The SML '" +
                   aSMLInfo.getManagementServiceURL () +
                   "' knows the SMP '" +
                   sSMPID +
                   "' with the logical address '" +
                   sLogicalAddress +
                   "'");
      return SMLRegistrationCheckResult.createRegistered (sSMPID, sLogicalAddress, sPhysicalAddress);
    }
    catch (final NotFoundFault ex)
    {
      LOGGER.warn ("The SML '" + aSMLInfo.getManagementServiceURL () + "' does not know the SMP '" + sSMPID + "'");
      return SMLRegistrationCheckResult.createNotRegistered (sSMPID);
    }
    catch (final Exception ex)
    {
      // Prefer the SML fault message, because it is way more specific
      String sErrorMsg = SMLExceptionHelper.getFaultMessage (ex);
      if (StringHelper.isEmpty (sErrorMsg))
        sErrorMsg = ex.getClass ().getName () + " - " + ex.getMessage ();

      LOGGER.error ("Failed to read the registration of SMP '" +
                    sSMPID +
                    "' from the SML '" +
                    aSMLInfo.getManagementServiceURL () +
                    "': " +
                    sErrorMsg);
      return SMLRegistrationCheckResult.createCheckFailed (sSMPID, sErrorMsg);
    }
  }

  /**
   * Drop the cached result, so that the next invocation of
   * {@link #getRegistrationCheckResult()} queries the SML again. This should be called whenever
   * the registration of this SMP at the SML was changed.
   */
  public static void clearCache ()
  {
    RW_LOCK.writeLocked ( () -> {
      s_sCacheKey = null;
      s_aResult = null;
    });
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("The SML registration cache was cleared");
  }

  /**
   * Get the registration of this SMP at the SML. If a result was determined less than
   * {@link #CACHE_DURATION} ago, for the same SML and the same SMP ID, that result is reused.
   * Otherwise the SML is queried, which is a remote call that may take up to the configured SML
   * request timeout.<br>
   * Note: the SML is deliberately queried outside of the write lock, so that a slow SML does not
   * block all other readers. Two threads racing for an expired entry may therefore both query the
   * SML once.
   *
   * @return <code>null</code> if the SML connection is disabled, if no SML is selected or if no
   *         SMP ID is configured, because in those cases there is nothing to check.
   */
  @Nullable
  public static SMLRegistrationCheckResult getRegistrationCheckResult ()
  {
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    if (!aSettings.isSMLEnabled ())
      return null;

    final ISMLInfo aSMLInfo = aSettings.getSMLInfo ();
    if (aSMLInfo == null)
      return null;

    final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
    if (StringHelper.isEmpty (sSMPID))
      return null;

    final String sCacheKey = _getCacheKey (aSMLInfo, sSMPID);
    final LocalDateTime aNotBefore = PDTFactory.getCurrentLocalDateTime ().minus (CACHE_DURATION);

    final SMLRegistrationCheckResult aCached = RW_LOCK.readLockedGet ( () -> {
      if (s_aResult != null && sCacheKey.equals (s_sCacheKey) && s_aResult.getCheckDateTime ().isAfter (aNotBefore))
        return s_aResult;
      return null;
    });
    if (aCached != null)
      return aCached;

    // Query the SML outside of the lock
    final SMLRegistrationCheckResult ret = _readFromSML (aSMLInfo, sSMPID);
    RW_LOCK.writeLocked ( () -> {
      s_sCacheKey = sCacheKey;
      s_aResult = ret;
    });
    return ret;
  }
}
