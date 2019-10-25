/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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
package com.helger.phoss.smp.app;

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
 * <li>Check for the value of the environment variable
 * <code>SMP_WEBAPP_CONFIG</code> (since 5.1.0)</li>
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
public final class SMPWebAppConfiguration extends AbstractGlobalSingleton
{
  public static final String PATH_WEBAPP_PROPERTIES = "webapp.properties";
  public static final String WEBAPP_KEY_GLOBAL_DEBUG = "global.debug";
  public static final String WEBAPP_KEY_GLOBAL_PRODUCTION = "global.production";

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPWebAppConfiguration.class);

  private static final ConfigFile s_aConfigFile;

  static
  {
    final ConfigFileBuilder aCFB = new ConfigFileBuilder ().addPathFromEnvVar ("SMP_WEBAPP_CONFIG")
                                                           .addPathFromSystemProperty ("peppol.smp.webapp.properties.path")
                                                           .addPathFromSystemProperty ("smp.webapp.properties.path")
                                                           .addPath ("private-" + PATH_WEBAPP_PROPERTIES)
                                                           .addPath (PATH_WEBAPP_PROPERTIES);

    s_aConfigFile = aCFB.build ();
    if (!s_aConfigFile.isRead ())
      throw new IllegalStateException ("Failed to read PEPPOL SMP UI properties from " + aCFB.getAllPaths ());
    LOGGER.info ("Read PEPPOL SMP UI properties from " + s_aConfigFile.getReadResource ().getPath ());
  }

  @Deprecated
  @UsedViaReflection
  private SMPWebAppConfiguration ()
  {}

  /**
   * @return The web application (UI) configuration file for the SMP server.
   *         Never <code>null</code>.
   */
  @Nonnull
  public static ConfigFile getConfigFile ()
  {
    return s_aConfigFile;
  }

  @Nonnull
  public static ISettings getSettingsObject ()
  {
    return getConfigFile ().getSettings ();
  }

  @Nonnull
  public static IReadableResource getSettingsResource ()
  {
    return getConfigFile ().getReadResource ();
  }

  /**
   * @return <code>true</code> if global debug is enabled. Should be turned off
   *         in production systems!
   */
  @Nullable
  public static String getGlobalDebug ()
  {
    return getConfigFile ().getAsString (WEBAPP_KEY_GLOBAL_DEBUG);
  }

  /**
   * @return <code>true</code> if global production mode is enabled. Should only
   *         be turned on in production systems!
   */
  @Nullable
  public static String getGlobalProduction ()
  {
    return getConfigFile ().getAsString (WEBAPP_KEY_GLOBAL_PRODUCTION);
  }

  /**
   * @return <code>true</code> if global JAX WS debugging should be enabled,
   *         <code>false</code> if not. Default is <code>false</code>.
   * @since 5.0.7
   */
  @Nullable
  public static boolean isGlobalDebugJaxWS ()
  {
    return getConfigFile ().getAsBoolean ("global.debugjaxws", false);
  }

  /**
   * @return The path where the application stores its data. Should be an
   *         absolute path.
   */
  @Nullable
  public static String getDataPath ()
  {
    return getConfigFile ().getAsString ("webapp.datapath");
  }

  public static boolean isCheckFileAccess ()
  {
    return getConfigFile ().getAsBoolean ("webapp.checkfileaccess", true);
  }

  /**
   * @return <code>true</code> if this is a public testable version,
   *         <code>false</code> if not.
   */
  public static boolean isTestVersion ()
  {
    return getConfigFile ().getAsBoolean ("webapp.testversion", GlobalDebug.isDebugMode ());
  }

  /**
   * This method has only effect, if participants are shown on the start page.
   *
   * @return <code>true</code> if the start page should show a dynamic table
   * @since 5.0.2
   */
  public static boolean isStartPageDynamicTable ()
  {
    return getConfigFile ().getAsBoolean ("webapp.startpage.dynamictable", false);
  }

  /**
   * @return <code>true</code> to show no participants on the start page.
   *         Default is <code>false</code>.
   * @since 5.0.4
   */
  public static boolean isStartPageParticipantsNone ()
  {
    return getConfigFile ().getAsBoolean ("webapp.startpage.participants.none", false);
  }

  /**
   * @return <code>true</code> to show extension details on the public start
   *         page, <code>false</code> to just show a yes or no indicator.
   *         Default is <code>false</code>.
   * @since 5.1.0
   */
  public static boolean isStartPageExtensionsShow ()
  {
    return getConfigFile ().getAsBoolean ("webapp.startpage.extensions.show", false);
  }

  /**
   * @return Name of the Directory. Usually "Peppol Directory" but maybe "TOOP
   *         Directory" as well.
   * @since 5.0.7
   */
  @Nonnull
  public static String getDirectoryName ()
  {
    return getConfigFile ().getAsString ("webapp.directory.name", "Peppol Directory");
  }

  /**
   * @return <code>true</code> to show extension details in the secure service
   *         group list, <code>false</code> to just show a yes or no indicator.
   *         Default is <code>false</code>.
   * @since 5.1.0
   */
  public static boolean isServiceGroupsExtensionsShow ()
  {
    return getConfigFile ().getAsBoolean ("webapp.servicegroups.extensions.show", false);
  }

  /**
   * Settings for issue #102
   *
   * @return <code>true</code> if the Login UI elements should be shown,
   *         <code>false</code> to to not show them. Default is
   *         <code>true</code>.
   * @since 5.1.2
   */
  public static boolean isPublicLoginEnabled ()
  {
    return getConfigFile ().getAsBoolean ("webapp.public.login.enabled", true);
  }
}
