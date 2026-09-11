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

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.collection.commons.ICommonsSet;
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
   * Resolve the Access Points of all endpoints contained in the provided service information. Every
   * endpoint gets the managed - and therefore de-duplicated - Access Point matching its endpoint
   * reference URL and certificate assigned. This must be called by every backend before persisting
   * a service information object.
   *
   * @param aAccessPointMgr
   *        The Access Point manager to use. May not be <code>null</code>.
   * @param aServiceInformation
   *        The service information to handle. May be <code>null</code>.
   */
  public static void resolveAccessPoints (@NonNull final ISMPAccessPointManager aAccessPointMgr,
                                          @Nullable final ISMPServiceInformation aServiceInformation)
  {
    ValueEnforcer.notNull (aAccessPointMgr, "AccessPointMgr");
    if (aServiceInformation == null)
      return;

    for (final ISMPProcess aProcess : aServiceInformation.getAllProcesses ())
      for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        resolveAccessPoint (aAccessPointMgr, aEndpoint);
  }

  /**
   * Resolve the Access Point of a single endpoint.
   *
   * @param aAccessPointMgr
   *        The Access Point manager to use. May not be <code>null</code>.
   * @param aEndpoint
   *        The endpoint to handle. May be <code>null</code>.
   * @return The resolved Access Point or <code>null</code> if the endpoint was <code>null</code> or
   *         of an unsupported type.
   */
  @Nullable
  public static ISMPAccessPoint resolveAccessPoint (@NonNull final ISMPAccessPointManager aAccessPointMgr,
                                                    @Nullable final ISMPEndpoint aEndpoint)
  {
    ValueEnforcer.notNull (aAccessPointMgr, "AccessPointMgr");
    if (!(aEndpoint instanceof SMPEndpoint))
      return aEndpoint == null ? null : aEndpoint.getAccessPoint ();

    final SMPEndpoint aRealEndpoint = (SMPEndpoint) aEndpoint;
    final ISMPAccessPoint aAccessPoint = aAccessPointMgr.getOrCreateAccessPoint (aRealEndpoint.getEndpointReference (),
                                                                                 aRealEndpoint.getCertificate ());
    aRealEndpoint.setAccessPoint (aAccessPoint);
    return aAccessPoint;
  }

  /**
   * Collect the IDs of all Access Points referenced by the endpoints of the provided service
   * information.
   *
   * @param aServiceInformation
   *        The service information to scan. May be <code>null</code>.
   * @param aTarget
   *        The target set to fill. May not be <code>null</code>.
   */
  public static void collectAccessPointIDs (@Nullable final ISMPServiceInformation aServiceInformation,
                                            @NonNull final ICommonsSet <String> aTarget)
  {
    ValueEnforcer.notNull (aTarget, "Target");
    if (aServiceInformation == null)
      return;

    for (final ISMPProcess aProcess : aServiceInformation.getAllProcesses ())
      for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        aTarget.add (aEndpoint.getAccessPointID ());
  }
}
