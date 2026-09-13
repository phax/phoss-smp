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
package com.helger.phoss.smp.domain.serviceinfo;

import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.state.ESuccess;
import com.helger.datetime.format.PDTToString;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.period.LocalDatePeriod;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;

/**
 * Helper class to deal with specific
 * 
 * @author Philip Helger
 */
@Immutable
public final class SMPEndpointHelper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPEndpointHelper.class);
  private static final LocalDate DATE_MIN = PDTFactory.createLocalDate (0, Month.JANUARY, 1);
  private static final LocalDate DATE_MAX = PDTFactory.createLocalDate (9999, Month.DECEMBER, 31);

  private SMPEndpointHelper ()
  {}

  @NonNull
  public static LocalDatePeriod createSafePeriod (@Nullable final LocalDate aNotBeforeDate,
                                                  @Nullable final LocalDate aNotAfterDate)
  {
    // The LocalDatePeriod handles null differently from what we expect
    return new LocalDatePeriod (aNotBeforeDate != null ? aNotBeforeDate : DATE_MIN,
                                aNotAfterDate != null ? aNotAfterDate : DATE_MAX);
  }

  @Nullable
  public static String getAsValidityString (@Nullable final LocalDate aNotBefore,
                                            @Nullable final LocalDate aNotAfter,
                                            @NonNull final Locale aDisplayLocale)
  {
    if (aNotBefore == null && aNotAfter == null)
      return null;

    String ret;
    if (aNotBefore == null)
      ret = "[since forever]";
    else
      ret = PDTToString.getAsString (aNotBefore, aDisplayLocale);

    if (aNotBefore != null && aNotAfter != null && aNotBefore.equals (aNotAfter))
    {
      // Only valid on that one day - no need to add an end date
    }
    else
    {
      ret += " - ";
      if (aNotAfter == null)
        ret += "[until eternity]";
      else
        ret += PDTToString.getAsString (aNotAfter, aDisplayLocale);
    }
    return ret;
  }

  @NonNull
  @Nonempty
  public static String createUniqueEndpointID ()
  {
    return GlobalIDFactory.getNewPersistentStringID ();
  }

  /**
   * Resolve the Access Point references of all endpoints contained in the provided service
   * information against the provided manager. Every endpoint that references an Access Point gets
   * the managed instance assigned, so that later changes of the Access Point are immediately
   * effective. Endpoints that contain the endpoint reference URL and the certificate directly are
   * not touched.
   *
   * @param aAccessPointMgr
   *        The Access Point manager to use. May not be <code>null</code>.
   * @param aServiceInformation
   *        The service information to handle. May be <code>null</code>.
   * @return <code>true</code> if all contained Access Point references could be resolved.
   */
  public static boolean resolveAccessPoints (@NonNull final ISMPAccessPointManager aAccessPointMgr,
                                             @Nullable final ISMPServiceInformation aServiceInformation)
  {
    ValueEnforcer.notNull (aAccessPointMgr, "AccessPointMgr");
    if (aServiceInformation == null)
      return true;

    boolean bAllResolved = true;
    for (final ISMPProcess aProcess : aServiceInformation.getAllProcesses ())
      for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        if (resolveAccessPoint (aAccessPointMgr, aEndpoint).isFailure ())
          bAllResolved = false;
    return bAllResolved;
  }

  /**
   * Resolve the Access Point reference of a single endpoint. If the endpoint does not reference an
   * Access Point, nothing happens.
   *
   * @param aAccessPointMgr
   *        The Access Point manager to use. May not be <code>null</code>.
   * @param aEndpoint
   *        The endpoint to handle. May be <code>null</code>.
   * @return {@link ESuccess#FAILURE} if the referenced Access Point could not be resolved.
   */
  @NonNull
  public static ESuccess resolveAccessPoint (@NonNull final ISMPAccessPointManager aAccessPointMgr,
                                             @Nullable final ISMPEndpoint aEndpoint)
  {
    ValueEnforcer.notNull (aAccessPointMgr, "AccessPointMgr");
    if (!(aEndpoint instanceof SMPEndpoint))
      return ESuccess.SUCCESS;

    final SMPEndpoint aRealEndpoint = (SMPEndpoint) aEndpoint;
    final ISMPAccessPoint aReferenced = aRealEndpoint.getAccessPoint ();
    if (aReferenced == null)
    {
      // Endpoint contains URL and certificate directly
      return ESuccess.SUCCESS;
    }

    final ISMPAccessPoint aResolved = aAccessPointMgr.getAccessPointOfID (aReferenced.getID ());
    if (aResolved == null)
    {
      LOGGER.warn ("Failed to resolve the SMP Access Point with ID '" +
                   aReferenced.getID () +
                   "' referenced from endpoint '" +
                   aRealEndpoint.getID () +
                   "'");
      return ESuccess.FAILURE;
    }

    // Important: use the managed object as-is and do not create a copy of it. Access Points are
    // shared between all endpoints referencing them, so that changing e.g. the certificate of an
    // Access Point is immediately effective for all of them.
    aRealEndpoint.setAccessPoint (aResolved);
    return ESuccess.SUCCESS;
  }
}
