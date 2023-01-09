/*
 * Copyright (C) 2015-2023 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.locale.LocaleCache;
import com.helger.settings.ISettings;
import com.helger.settings.exchange.properties.SettingsPersistenceProperties;

/**
 * This class contains global SMP server constants.
 *
 * @author Philip Helger
 */
@Immutable
public final class CSMPServer
{
  public static final Locale DEFAULT_LOCALE = LocaleCache.getInstance ().getLocale ("en", "US");

  /** A regular expression pattern to validate the SMP ID. */
  public static final String PATTERN_SMP_ID = "[a-zA-Z0-9\\-\\.]+";

  /**
   * The default time zone is (for historical reasons) UTC, but
   * https://github.com/phax/phoss-smp/issues/167 asked to make this
   * customizable
   */
  public static final String DEFAULT_TIMEZONE = "UTC";

  public static final String LOG_SUFFIX_NO_SML_INTERACTION = " (no SML interaction)";

  public static final String SMP_SERVER_VERSION_FILENAME = "smp-server-version.properties";

  private static final String VERSION_NUMBER;
  private static final String TIMESTAMP;

  static
  {
    // Read version number
    final SettingsPersistenceProperties aSPP = new SettingsPersistenceProperties ();
    final ISettings aVersionProps = aSPP.readSettings (new ClassPathResource (SMP_SERVER_VERSION_FILENAME));
    VERSION_NUMBER = aVersionProps.getAsString ("smp.version");
    if (VERSION_NUMBER == null)
      throw new InitializationException ("Error determining SMP version number!");
    TIMESTAMP = aVersionProps.getAsString ("timestamp");
    if (TIMESTAMP == null)
      throw new InitializationException ("Error determining SMP build timestamp!");
  }

  private CSMPServer ()
  {}

  /**
   * @return The version number of the SMP server read from the internal
   *         properties file. Never <code>null</code>.
   */
  @Nonnull
  public static String getVersionNumber ()
  {
    return VERSION_NUMBER;
  }

  /**
   * @return The build timestamp of the SMP server read from the internal
   *         properties file. Never <code>null</code>.
   */
  @Nonnull
  public static String getBuildTimestamp ()
  {
    return TIMESTAMP;
  }
}
