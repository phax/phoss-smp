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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.smpclient.url.dns.IBDXLURLProvider;
import com.helger.text.ReadOnlyMultilingualText;
import com.helger.web.scope.mgr.WebScoped;

/**
 * The long running job that checks the DNS state of all Service Groups. Above
 * {@link com.helger.phoss.smp.config.SMPServerConfiguration#getDNSCheckAsyncThreshold()} Service
 * Groups this is the only sensible way to do it - performing one NAPTR lookup per participant while
 * a page is rendered does not scale.<br>
 * The result is stored in {@link DNSCheckCache}, so that the page can render it with its per row
 * actions intact.<br>
 * The caller must acquire {@link DNSCheckCache#LOCK} before starting this job - the job itself
 * releases the lock.
 *
 * @author Philip Helger
 * @since 8.6.0
 */
public class DNSCheckJob extends AbstractLongRunningJobRunnable
{
  /** The type of this long running job */
  public static final String JOB_TYPE = "dns-check";

  public DNSCheckJob (@NonNull @Nonempty final String sUserID)
  {
    super (JOB_TYPE,
           new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Check the DNS state of all participants"),
           () -> sUserID);
    ValueEnforcer.notEmpty (sUserID, "UserID");
  }

  @NonNull
  public LongRunningJobResult createLongRunningJobResult ()
  {
    final ISMPURLProvider aURLProvider = SMPMetaManager.getSMPURLProvider ();
    if (!(aURLProvider instanceof final IBDXLURLProvider aRealProvider))
      throw new IllegalStateException ("The configured URL provider cannot resolve participants via DNS");

    final ICommonsList <IParticipantIdentifier> aParticipantIDs = new CommonsArrayList <> ();
    for (final ISMPServiceGroup aServiceGroup : SMPMetaManager.getServiceGroupMgr ().getAllSMPServiceGroups ())
      aParticipantIDs.add (aServiceGroup.getParticipantIdentifier ());

    final String sSMLZoneName = SMPMetaManager.getSettings ().getSMLDNSZone ();
    final DNSCheckResult aResult = DNSChecker.checkAll (aRealProvider, aParticipantIDs, sSMLZoneName);

    // The page renders this, so that the per row actions stay available
    DNSCheckCache.setResult (aResult);

    return LongRunningJobResult.createText (aResult.getEntryCount () +
                                            " participant(s) checked in " +
                                            aResult.getDuration ().toMillis () +
                                            " ms: " +
                                            aResult.getCountOfState (EDNSCheckState.RESOLVED) +
                                            " resolved, " +
                                            aResult.getCountOfState (EDNSCheckState.NOT_REGISTERED) +
                                            " not registered in the SML, " +
                                            aResult.getCountOfState (EDNSCheckState.LOOKUP_FAILED) +
                                            " lookup failed");
  }

  @Override
  public void run ()
  {
    // A Web Scope is needed for the DB access as well as for storing the job result
    try (final WebScoped w = new WebScoped ())
    {
      super.run ();
    }
    finally
    {
      // Always release, even if the job failed
      DNSCheckCache.LOCK.release ();
    }
  }
}
