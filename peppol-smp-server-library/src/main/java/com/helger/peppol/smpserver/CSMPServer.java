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
package com.helger.peppol.smpserver;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.ClassPathResource;
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
  public static final String SMP_SERVER_VERSION_FILENAME = "smp-server-version.properties";

  private static final String VERSION_NUMBER;

  static
  {
    // Read version number
    final SettingsPersistenceProperties aSPP = new SettingsPersistenceProperties ();
    final ISettings aVersionProps = aSPP.readSettings (new ClassPathResource (SMP_SERVER_VERSION_FILENAME));
    VERSION_NUMBER = aVersionProps.getAsString ("smp.version");
    if (VERSION_NUMBER == null)
      throw new InitializationException ("Error determining SMP version number!");
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
}
