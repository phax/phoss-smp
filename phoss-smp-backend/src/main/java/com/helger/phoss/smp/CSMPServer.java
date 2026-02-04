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
package com.helger.phoss.smp;

import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.exception.InitializationException;
import com.helger.io.resource.ClassPathResource;
import com.helger.settings.ISettings;
import com.helger.settings.exchange.properties.SettingsPersistenceProperties;
import com.helger.text.locale.LocaleCache;

/**
 * This class contains global SMP server constants.
 *
 * @author Philip Helger
 */
@Immutable
public final class CSMPServer
{
  public static final Locale DEFAULT_LOCALE = LocaleCache.getInstance ().getLocale ("en", "US");

  /**
   * A regular expression pattern to validate the SMP ID.<br>
   * Update 8.0.9: allow "." as for "smptest.intercenter.it". But not at the start or the end and
   * not more then one consecutive dot.
   */
  public static final String PATTERN_SMP_ID = "^[a-zA-Z0-9\\-]+(\\.[a-zA-Z0-9\\-]+)*$";

  /** A regular expression pattern to validate HR OIBs. */
  public static final String PATTERN_HR_OIB = "[0-9]{11}";

  public static final String HR_EXTENSION_DEFAULT_PREFIX = "hrext";
  public static final String HR_EXTENSION_NAMESPACE_URI = "http://porezna-uprava.hr/mps/extension";

  /**
   * The default time zone is (for historical reasons) UTC, but
   * https://github.com/phax/phoss-smp/issues/167 asked to make this customizable
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
   * @return The version number of the SMP server read from the internal properties file. Never
   *         <code>null</code>.
   */
  @NonNull
  public static String getVersionNumber ()
  {
    return VERSION_NUMBER;
  }

  /**
   * @return The build timestamp of the SMP server read from the internal properties file. Never
   *         <code>null</code>.
   */
  @NonNull
  public static String getBuildTimestamp ()
  {
    return TIMESTAMP;
  }
}
