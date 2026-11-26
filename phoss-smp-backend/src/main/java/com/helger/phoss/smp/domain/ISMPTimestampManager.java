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
package com.helger.phoss.smp.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.jspecify.annotations.NonNull;

import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.datetime.zone.PDTConfig;

/**
 * Interface for providing time stamps. This is the single source of the current date and time
 * within the SMP, so that it can be replaced (e.g. for testing) via
 * {@link ISMPManagerProvider#createTimestampMgr()}.<br>
 * The precision of all the methods in this class is milliseconds, so that the values are the same
 * in all backends and stay compatible to XML serialization.
 *
 * @author Philip Helger
 * @since 8.9.0
 */
public interface ISMPTimestampManager
{
  /**
   * @return The current date and time in the current time zone. Never <code>null</code>.
   */
  @NonNull
  OffsetDateTime getCurrentDateTime ();

  /**
   * @return The current date and time in UTC. Never <code>null</code>.
   */
  @NonNull
  default OffsetDateTime getCurrentDateTimeUTC ()
  {
    return getCurrentDateTime ().withOffsetSameInstant (ZoneOffset.UTC);
  }

  /**
   * @return The current date and time in the default time zone of {@link PDTConfig}. Never
   *         <code>null</code>.
   */
  @NonNull
  default ZonedDateTime getCurrentZonedDateTime ()
  {
    return getCurrentDateTime ().atZoneSameInstant (PDTConfig.getDefaultZoneId ());
  }

  /**
   * @return The current date and time in UTC. Never <code>null</code>.
   */
  @NonNull
  default ZonedDateTime getCurrentZonedDateTimeUTC ()
  {
    return getCurrentDateTime ().atZoneSameInstant (ZoneOffset.UTC);
  }

  /**
   * @return The current date and time in the current time zone, without the offset. Never
   *         <code>null</code>.
   */
  @NonNull
  default LocalDateTime getCurrentLocalDateTime ()
  {
    return getCurrentDateTime ().toLocalDateTime ();
  }

  /**
   * @return The current date and time in the current time zone for XML processing. Never
   *         <code>null</code>.
   */
  @NonNull
  default XMLOffsetDateTime getCurrentXMLDateTime ()
  {
    return XMLOffsetDateTime.of (getCurrentDateTime ());
  }

  /**
   * @return The current date in the current time zone. Never <code>null</code>.
   */
  @NonNull
  default LocalDate getCurrentDate ()
  {
    return getCurrentDateTime ().toLocalDate ();
  }

  /**
   * @return The default implementation, based on the system clock and the default time zone of
   *         {@link PDTFactory}. Never <code>null</code>.
   */
  @NonNull
  static ISMPTimestampManager createDefaultInstance ()
  {
    // Limited to milliseconds only
    return () -> PDTFactory.getWithMillisOnly (PDTFactory.getCurrentOffsetDateTime ());
  }
}
