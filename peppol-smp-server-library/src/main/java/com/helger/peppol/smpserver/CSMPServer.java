/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.smpserver;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.settings.ISettings;
import com.helger.settings.exchange.properties.SettingsPersistenceProperties;

/**
 * This class contains global SMP server constants.
 *
 * @author Philip Helger
 */
public final class CSMPServer extends AbstractGlobalSingleton
{
  public static final String SMP_SERVER_VERSION_FILENAME = "smp-server-version.properties";

  private static final String s_sVersionNumber;

  static
  {
    // Read version number
    final SettingsPersistenceProperties aSPP = new SettingsPersistenceProperties ();
    final ISettings aVersionProps = aSPP.readSettings (new ClassPathResource (SMP_SERVER_VERSION_FILENAME));
    s_sVersionNumber = aVersionProps.getStringValue ("smp.version");
    if (s_sVersionNumber == null)
      throw new InitializationException ("Error determining SMP version number!");
  }

  @Deprecated
  @UsedViaReflection
  private CSMPServer ()
  {}

  /**
   * @return The version number of the SMP server. Never <code>null</code>.
   */
  @Nonnull
  public static String getVersionNumber ()
  {
    return s_sVersionNumber;
  }
}
