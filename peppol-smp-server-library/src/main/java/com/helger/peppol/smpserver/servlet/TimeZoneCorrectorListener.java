/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.servlet;

import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.datetime.PDTConfig;
import com.helger.commons.exception.InitializationException;

/**
 * This class is used for setting the timezone so that dates saved to the
 * database are always in UTC.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Immutable
public final class TimeZoneCorrectorListener implements ServletContextListener
{
  public static final String DEFAULT_TIMEZONE = "UTC";

  private static final Logger LOGGER = LoggerFactory.getLogger (TimeZoneCorrectorListener.class);

  public void contextInitialized (@Nonnull final ServletContextEvent aServletContextEvent)
  {
    LOGGER.info ("SMP server started");

    // Check if the timezone is supported
    if (!ArrayHelper.contains (TimeZone.getAvailableIDs (), DEFAULT_TIMEZONE))
    {
      final String sErrorMsg = "The default time zone '" + DEFAULT_TIMEZONE + "' is not supported!";
      LOGGER.error (sErrorMsg);
      throw new InitializationException (sErrorMsg);
    }

    // Set the default timezone
    if (PDTConfig.setDefaultDateTimeZoneID (DEFAULT_TIMEZONE).isFailure ())
    {
      final String sErrorMsg = "Failed to set default time zone to '" + DEFAULT_TIMEZONE + "'!";
      LOGGER.error (sErrorMsg);
      throw new InitializationException (sErrorMsg);
    }
  }

  public void contextDestroyed (@Nonnull final ServletContextEvent aServletContextEvent)
  {
    LOGGER.info ("SMP server stopped");
  }
}
