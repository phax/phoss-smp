/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.app;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.settings.ISettings;
import com.helger.settings.exchange.properties.SettingsPersistenceProperties;

/**
 * This class provides access to the web application settings. If the system
 * property <code>smp.webapp.properties.path</code> is defined, the
 * configuration file is read from the absolute path stated there. Otherwise (by
 * default) the configuration settings contained in the
 * <code>src/main/resources/webapp.properties</code> file are read.
 *
 * @author Philip Helger
 */
public final class AppSettings extends AbstractGlobalSingleton
{
  /** The name of the classpath resource containing the settings */
  public static final String FILENAME = "webapp.properties";

  private static final Logger s_aLogger = LoggerFactory.getLogger (AppSettings.class);

  private static final IReadableResource s_aRes;
  private static final ISettings s_aSettings;

  static
  {
    final String sPropertyPath = SystemProperties.getPropertyValue ("smp.webapp.properties.path");
    if (StringHelper.hasText (sPropertyPath))
      s_aRes = new FileSystemResource (sPropertyPath);
    else
      s_aRes = new ClassPathResource (FILENAME);

    s_aLogger.info ("Reading webapp.properties from " + s_aRes.getPath ());
    final SettingsPersistenceProperties aSPP = new SettingsPersistenceProperties ();
    s_aSettings = aSPP.readSettings (s_aRes);
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

  @Nonnull
  public static IReadableResource getSettingsResource ()
  {
    return s_aRes;
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
