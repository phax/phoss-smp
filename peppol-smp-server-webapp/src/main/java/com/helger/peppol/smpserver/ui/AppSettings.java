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
package com.helger.peppol.smpserver.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.settings.ISettings;
import com.helger.settings.exchange.properties.SettingsPersistenceProperties;

/**
 * This class provides access to the settings as contained in the
 * <code>webapp.properties</code> file.
 *
 * @author Philip Helger
 */
public final class AppSettings extends AbstractGlobalSingleton
{
  /** The name of the file containing the settings */
  public static final String FILENAME = "webapp.properties";
  private static final ISettings s_aSettings;

  static
  {
    s_aSettings = new SettingsPersistenceProperties ().readSettings (new ClassPathResource (FILENAME));
  }

  @Deprecated
  @UsedViaReflection
  private AppSettings ()
  {}

  @Nonnull
  public static ISettings getSettingsObject ()
  {
    return s_aSettings;
  }

  @Nullable
  public static String getGlobalDebug ()
  {
    return s_aSettings.getStringValue ("global.debug");
  }

  @Nullable
  public static String getGlobalProduction ()
  {
    return s_aSettings.getStringValue ("global.production");
  }

  @Nullable
  public static String getDataPath ()
  {
    return s_aSettings.getStringValue ("webapp.datapath");
  }

  public static boolean isCheckFileAccess ()
  {
    return s_aSettings.getBooleanValue ("webapp.checkfileaccess", true);
  }

  public static boolean isTestVersion ()
  {
    return s_aSettings.getBooleanValue ("webapp.testversion", GlobalDebug.isDebugMode ());
  }
}
