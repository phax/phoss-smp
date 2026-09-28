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
package com.helger.phoss.smp.dnscheck;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.smpclient.url.SMPDNSResolutionException;
import com.helger.smpclient.url.dns.IBDXLURLProvider;

/**
 * Performs the DNS check of a set of participants.<br>
 * In contrast to the paged read of the SML <code>List()</code> operation, every participant here is
 * an independent NAPTR lookup, so the lookups are performed in parallel. That is what makes the
 * check usable for more than a handful of participants at all - the wall clock time is dominated by
 * the DNS round trips, not by CPU.
 *
 * @author Philip Helger
 * @since 8.5.1
 */
@Immutable
public final class DNSChecker
{
  private static final Logger LOGGER = LoggerFactory.getLogger (DNSChecker.class);

  private DNSChecker ()
  {}

  /**
   * Check a single participant. This never throws.
   *
   * @param aURLProvider
   *        The URL provider to be used. May not be <code>null</code>.
   * @param aParticipantID
   *        The participant to be checked. May not be <code>null</code>.
   * @param sSMLZoneName
   *        The DNS zone of the SML. May be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static DNSCheckEntry checkParticipant (@NonNull final IBDXLURLProvider aURLProvider,
                                                @NonNull final IParticipantIdentifier aParticipantID,
                                                @Nullable final String sSMLZoneName)
  {
    ValueEnforcer.notNull (aURLProvider, "URLProvider");
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    String sDNSName = null;
    try
    {
      // Does not perform a lookup - it only builds the name
      sDNSName = aURLProvider.getDNSNameOfParticipant (aParticipantID, sSMLZoneName);
    }
    catch (final RuntimeException ex)
    {
      return new DNSCheckEntry (aParticipantID, EDNSCheckState.LOOKUP_FAILED, null, null, ex.getMessage ());
    }

    try
    {
      // This performs the NAPTR lookup and may take some time
      final String sSMPURI = aURLProvider.getSMPURIOfParticipant (aParticipantID, sSMLZoneName).toString ();
      return new DNSCheckEntry (aParticipantID, EDNSCheckState.RESOLVED, sDNSName, sSMPURI, null);
    }
    catch (final SMPDNSResolutionException ex)
    {
      // The participant is simply not registered in the SML
      return new DNSCheckEntry (aParticipantID, EDNSCheckState.NOT_REGISTERED, sDNSName, null, null);
    }
    catch (final RuntimeException ex)
    {
      return new DNSCheckEntry (aParticipantID, EDNSCheckState.LOOKUP_FAILED, sDNSName, null, ex.getMessage ());
    }
  }

  /**
   * Check all provided participants, in parallel.
   *
   * @param aURLProvider
   *        The URL provider to be used. May not be <code>null</code>.
   * @param aParticipantIDs
   *        The participants to be checked. May not be <code>null</code>.
   * @param sSMLZoneName
   *        The DNS zone of the SML. May be <code>null</code>.
   * @return Never <code>null</code>. The entries are in the order of the provided participants.
   */
  @NonNull
  public static DNSCheckResult checkAll (@NonNull final IBDXLURLProvider aURLProvider,
                                         @NonNull final Collection <? extends IParticipantIdentifier> aParticipantIDs,
                                         @Nullable final String sSMLZoneName)
  {
    ValueEnforcer.notNull (aURLProvider, "URLProvider");
    ValueEnforcer.notNull (aParticipantIDs, "ParticipantIDs");

    final LocalDateTime aStartDT = PDTFactory.getCurrentLocalDateTime ();
    final ICommonsList <DNSCheckEntry> aEntries = new CommonsArrayList <> ();

    if (aParticipantIDs.isEmpty ())
      return new DNSCheckResult (aStartDT, PDTFactory.getCurrentLocalDateTime (), aEntries);

    final int nThreads = Math.min (SMPServerConfiguration.getDNSCheckThreadCount (), aParticipantIDs.size ());
    LOGGER.info ("Checking the DNS state of " +
                 aParticipantIDs.size () +
                 " participant(s) using " +
                 nThreads +
                 " thread(s)");

    final ExecutorService aES = Executors.newFixedThreadPool (nThreads);
    try
    {
      final ICommonsList <Future <DNSCheckEntry>> aFutures = new CommonsArrayList <> ();
      for (final IParticipantIdentifier aParticipantID : aParticipantIDs)
        aFutures.add (aES.submit (() -> checkParticipant (aURLProvider, aParticipantID, sSMLZoneName)));

      for (final Future <DNSCheckEntry> aFuture : aFutures)
        try
        {
          aEntries.add (aFuture.get ());
        }
        catch (final InterruptedException ex)
        {
          Thread.currentThread ().interrupt ();
          LOGGER.warn ("The DNS check was interrupted after " + aEntries.size () + " participant(s)");
          break;
        }
        catch (final Exception ex)
        {
          // checkParticipant never throws, so this should not happen
          LOGGER.error ("Unexpected error in a DNS check task", ex);
        }
    }
    finally
    {
      aES.shutdownNow ();
      try
      {
        aES.awaitTermination (5, TimeUnit.SECONDS);
      }
      catch (final InterruptedException ex)
      {
        Thread.currentThread ().interrupt ();
      }
    }

    final DNSCheckResult ret = new DNSCheckResult (aStartDT, PDTFactory.getCurrentLocalDateTime (), aEntries);
    LOGGER.info ("Finished the DNS check of " +
                 ret.getEntryCount () +
                 " participant(s) in " +
                 ret.getDuration ().toMillis () +
                 " ms: " +
                 ret.getCountOfState (EDNSCheckState.RESOLVED) +
                 " resolved, " +
                 ret.getCountOfState (EDNSCheckState.NOT_REGISTERED) +
                 " not registered, " +
                 ret.getCountOfState (EDNSCheckState.LOOKUP_FAILED) +
                 " failed");
    return ret;
  }

  /**
   * @param aParticipantIDs
   *        The participants that would be checked. May not be <code>null</code>.
   * @return <code>true</code> if the check has to run as a background job, because there are too
   *         many participants to do it while a page is rendered.
   * @see SMPServerConfiguration#getDNSCheckAsyncThreshold()
   */
  public static boolean isAsyncNeeded (@NonNull final Collection <? extends IParticipantIdentifier> aParticipantIDs)
  {
    ValueEnforcer.notNull (aParticipantIDs, "ParticipantIDs");
    final int nThreshold = SMPServerConfiguration.getDNSCheckAsyncThreshold ();
    // 0 means "always in the background"
    return nThreshold <= 0 || aParticipantIDs.size () >= nThreshold;
  }

}
