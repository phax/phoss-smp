/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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
import com.helger.commons.io.resource.IReadableResource;
import com.helger.scope.singleton.AbstractGlobalSingleton;
import com.helger.settings.ISettings;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

/**
 * This class provides access to the web application settings. The order of the
 * properties file resolving is as follows:
 * <ol>
 * <li>Check for the value of the system property
 * <code>peppol.smp.webapp.properties.path</code></li>
 * <li>Check for the value of the system property
 * <code>smp.webapp.properties.path</code></li>
 * <li>The filename <code>private-webapp.properties</code> in the root of the
 * classpath</li>
 * <li>The filename <code>webapp.properties</code> in the root of the
 * classpath</li>
 * </ol>
 *
 * @author Philip Helger
 */
public final class AppConfiguration extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AppConfiguration.class);

  private static final ConfigFile s_aConfigFile;

  static
  {
    final ConfigFileBuilder aCFB = new ConfigFileBuilder ().addPathFromSystemProperty ("peppol.smp.webapp.properties.path")
                                                           .addPathFromSystemProperty ("smp.webapp.properties.path")
                                                           .addPath ("private-webapp.properties")
                                                           .addPath ("webapp.properties");

    s_aConfigFile = aCFB.build ();
    if (!s_aConfigFile.isRead ())
      throw new IllegalStateException ("Failed to read PEPPOL SMP UI properties from " + aCFB.getAllPaths ());
    LOGGER.info ("Read PEPPOL SMP UI properties from " + s_aConfigFile.getReadResource ().getPath ());
  }

  @Deprecated
  @UsedViaReflection
  private AppConfiguration ()
  {}

  @Nonnull
  public static ISettings getSettingsObject ()
  {
    return s_aConfigFile.getSettings ();
  }

  @Nonnull
  public static IReadableResource getSettingsResource ()
  {
    return s_aConfigFile.getReadResource ();
  }

  /**
   * @return <code>true</code> if global debug is enabled. Should be turned off
   *         in production systems!
   */
  @Nullable
  public static String getGlobalDebug ()
  {
    return s_aConfigFile.getAsString ("global.debug");
  }

  /**
   * @return <code>true</code> if global production mode is enabled. Should only
   *         be turned on in production systems!
   */
  @Nullable
  public static String getGlobalProduction ()
  {
    return s_aConfigFile.getAsString ("global.production");
  }

  /**
   * @return <code>true</code> if global JAX WS debugging should be enabled,
   *         <code>false</code> if not. Default is <code>false</code>.
   * @since 5.0.7
   */
  @Nullable
  public static boolean isGlobalDebugJaxWS ()
  {
    return s_aConfigFile.getAsBoolean ("global.debugjaxws", false);
  }

  /**
   * @return The path where the application stores its data. Should be an
   *         absolute path.
   */
  @Nullable
  public static String getDataPath ()
  {
    return s_aConfigFile.getAsString ("webapp.datapath");
  }

  public static boolean isCheckFileAccess ()
  {
    return s_aConfigFile.getAsBoolean ("webapp.checkfileaccess", true);
  }

  /**
   * @return <code>true</code> if this is a public testable version,
   *         <code>false</code> if not.
   */
  public static boolean isTestVersion ()
  {
    return s_aConfigFile.getAsBoolean ("webapp.testversion", GlobalDebug.isDebugMode ());
  }

  /**
   * This method has only effect, if participants are shown on the start page.
   *
   * @return <code>true</code> if the start page should show a dynamic table
   * @since 5.0.2
   */
  public static boolean isStartPageDynamicTable ()
  {
    return s_aConfigFile.getAsBoolean ("webapp.startpage.dynamictable", false);
  }

  /**
   * @return <code>true</code> to show no participants on the start page.
   *         Default is <code>false</code>.
   * @since 5.0.4
   */
  public static boolean isStartPageParticipantsNone ()
  {
    return s_aConfigFile.getAsBoolean ("webapp.startpage.participants.none", false);
  }

  /**
   * @return Name of the Directory. Usually "PEPPOL Directory" but maybe "TOOP
   *         Directory" as well.
   * @since 5.0.7
   */
  @Nonnull
  public static String getDirectoryName ()
  {
    return s_aConfigFile.getAsString ("webapp.directory.name", "PEPPOL Directory");
  }
}
