/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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

import java.net.URL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.commons.url.URLHelper;
import com.helger.config.IConfig;
import com.helger.phoss.smp.SMPConfigSource;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * This class provides access to the web application settings.
 *
 * @author Philip Helger
 */
public final class SMPWebAppConfiguration extends AbstractGlobalSingleton
{
  public static final String WEBAPP_KEY_GLOBAL_DEBUG = "global.debug";
  public static final String WEBAPP_KEY_GLOBAL_PRODUCTION = "global.production";

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated
  @UsedViaReflection
  private SMPWebAppConfiguration ()
  {}

  /**
   * @return The web application (UI) configuration for the SMP server. Never
   *         <code>null</code>.
   */
  @Nonnull
  private static IConfig _getConfig ()
  {
    return SMPConfigSource.getConfig ();
  }

  /**
   * @return <code>true</code> if global debug is enabled. Should be turned off
   *         in production systems!
   */
  @Nullable
  public static String getGlobalDebug ()
  {
    return _getConfig ().getAsString (WEBAPP_KEY_GLOBAL_DEBUG);
  }

  /**
   * @return <code>true</code> if global production mode is enabled. Should only
   *         be turned on in production systems!
   */
  @Nullable
  public static String getGlobalProduction ()
  {
    return _getConfig ().getAsString (WEBAPP_KEY_GLOBAL_PRODUCTION);
  }

  /**
   * @return <code>true</code> if global JAX WS debugging should be enabled,
   *         <code>false</code> if not. Default is <code>false</code>.
   * @since 5.0.7
   */
  public static boolean isGlobalDebugJaxWS ()
  {
    return _getConfig ().getAsBoolean ("global.debugjaxws", false);
  }

  /**
   * @return The path where the application stores its data. Should be an
   *         absolute path.
   */
  @Nullable
  public static String getDataPath ()
  {
    return _getConfig ().getAsString ("webapp.datapath");
  }

  public static boolean isCheckFileAccess ()
  {
    return _getConfig ().getAsBoolean ("webapp.checkfileaccess", true);
  }

  /**
   * @return <code>true</code> if this is a public testable version,
   *         <code>false</code> if not.
   */
  public static boolean isTestVersion ()
  {
    return _getConfig ().getAsBoolean ("webapp.testversion", GlobalDebug.isDebugMode ());
  }

  /**
   * @return <code>true</code> to persist statistics on end, <code>false</code>
   *         to not do it. Default is <code>true</code>.
   * @since 5.2.4
   */
  public static boolean isPersistStatisticsOnEnd ()
  {
    return _getConfig ().getAsBoolean ("webapp.statistics.persist", true);
  }

  /**
   * This method has only effect, if participants are shown on the start page.
   *
   * @return <code>true</code> if the start page should show a dynamic table
   * @since 5.0.2
   */
  public static boolean isStartPageDynamicTable ()
  {
    return _getConfig ().getAsBoolean ("webapp.startpage.dynamictable", false);
  }

  /**
   * @return <code>true</code> to show no participants on the start page.
   *         Default is <code>false</code>.
   * @since 5.0.4
   */
  public static boolean isStartPageParticipantsNone ()
  {
    return _getConfig ().getAsBoolean ("webapp.startpage.participants.none", false);
  }

  /**
   * @return <code>true</code> to show extension details on the public start
   *         page, <code>false</code> to just show a yes or no indicator.
   *         Default is <code>false</code>.
   * @since 5.1.0
   */
  public static boolean isStartPageExtensionsShow ()
  {
    return _getConfig ().getAsBoolean ("webapp.startpage.extensions.show", false);
  }

  /**
   * @return Name of the Directory. Usually "Peppol Directory" but maybe "TOOP
   *         Directory" as well.
   * @since 5.0.7
   */
  @Nonnull
  public static String getDirectoryName ()
  {
    return _getConfig ().getAsString ("webapp.directory.name", "Peppol Directory");
  }

  /**
   * @return <code>true</code> to show extension details in the secure service
   *         group list, <code>false</code> to just show a yes or no indicator.
   *         Default is <code>false</code>.
   * @since 5.1.0
   */
  public static boolean isServiceGroupsExtensionsShow ()
  {
    return _getConfig ().getAsBoolean ("webapp.servicegroups.extensions.show", false);
  }

  /**
   * @return <code>true</code> if the details of login failures (like "User not
   *         existing" or "Invalid password") should be shown or not.
   * @since 6.0.0
   */
  public static boolean isSecurityLoginShowErrorDetails ()
  {
    return _getConfig ().getAsBoolean ("webapp.security.login.errordetails", true);
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
    return _getConfig ().getAsBoolean ("webapp.public.login.enabled", true);
  }

  /**
   * The inline version has precedence over the external URL and the internal
   * URL version.
   *
   * @return An inline data URL that represents an image and will be used as the
   *         logo on the public page part. Must start with "data:".
   * @see #getPublicLogoExternalUrl()
   * @see #getPublicLogoInternalUrl()
   * @since 6.0.0
   */
  @Nullable
  public static String getPublicLogoInline ()
  {
    return _getConfig ().getAsString ("webapp.public.logo.inline");
  }

  /**
   * The external URL version has precedence over the internal URL version.
   *
   * @return An absolute URL pointing to an image and will be used as the logo
   *         on the public page part. Should start with "http:" or "https:".
   *         Please consider CORS settings on the other side.
   * @see #getPublicLogoInline()
   * @see #getPublicLogoInternalUrl()
   * @since 6.0.0
   */
  @Nullable
  public static String getPublicLogoExternalUrl ()
  {
    return _getConfig ().getAsString ("webapp.public.logo.externalurl");
  }

  /**
   * @return A relative URL pointing to an image inside the same web server that
   *         will be used as the logo on the public page part. Should start with
   *         "/".
   * @see #getPublicLogoInline()
   * @see #getPublicLogoExternalUrl()
   * @since 6.0.0
   */
  @Nullable
  public static String getPublicLogoInternalUrl ()
  {
    return _getConfig ().getAsString ("webapp.public.logo.internalurl");
  }

  /**
   * @return <code>true</code> to show the author in the public area,
   *         <code>false</code> to not show it.
   * @since 5.5.0
   */
  public static boolean isPublicShowApplicationName ()
  {
    return _getConfig ().getAsBoolean ("webapp.public.showappname", true);
  }

  /**
   * @return <code>true</code> to show the author in the public area,
   *         <code>false</code> to not show it.
   * @since 5.5.0
   */
  public static boolean isPublicShowSource ()
  {
    return _getConfig ().getAsBoolean ("webapp.public.showsource", true);
  }

  /**
   * @return <code>true</code> to show the author in the public area,
   *         <code>false</code> to not show it.
   * @since 5.2.6
   */
  public static boolean isPublicShowAuthor ()
  {
    return _getConfig ().getAsBoolean ("webapp.public.showauthor", true);
  }

  /**
   * Setting for issue #132
   *
   * @return <code>true</code> if a custom imprint should be shown in the
   *         footer, <code>false</code> if not.
   * @since 5.2.4
   */
  public static boolean isImprintEnabled ()
  {
    return _getConfig ().getAsBoolean ("webapp.imprint.enabled", false);
  }

  /**
   * This method is only called if {@link #isImprintEnabled()} returns
   * <code>true</code>.
   *
   * @return The text used to reference the Imprint. May be <code>null</code>.
   * @since 5.2.4
   */
  @Nullable
  public static String getImprintText ()
  {
    return _getConfig ().getAsString ("webapp.imprint.text");
  }

  /**
   * This method is only called if {@link #isImprintEnabled()} returns
   * <code>true</code>.
   *
   * @return The HRef the Imprint should link to. May be <code>null</code> in
   *         which case only the text is rendered.
   * @since 5.2.4
   */
  @Nullable
  public static ISimpleURL getImprintHref ()
  {
    // Take only valid URLs
    final URL aHref = URLHelper.getAsURL (_getConfig ().getAsString ("webapp.imprint.href"), false);
    return aHref == null ? null : new SimpleURL (aHref);
  }

  /**
   * This method is only called if {@link #isImprintEnabled()} returns
   * <code>true</code> and if a imprint link is returned by
   * {@link #getImprintHref()}.
   *
   * @return The HTML link target to be used for the Imprint link. May be
   *         <code>null</code> in which case the link opens in the current
   *         window.
   * @since 5.2.4
   */
  @Nullable
  public static String getImprintTarget ()
  {
    return _getConfig ().getAsString ("webapp.imprint.target");
  }

  /**
   * This method is only called if {@link #isImprintEnabled()} returns
   * <code>true</code>.
   *
   * @return A String of whitespace separated CSS classes that should be applied
   *         on the Imprint HTML node (text or link). The supported classes
   *         depend on the user interface framework that is in use (currently
   *         Bootstrap 4).
   * @since 5.2.4
   */
  @Nullable
  public static String getImprintCSSClasses ()
  {
    return StringHelper.trim (_getConfig ().getAsString ("webapp.imprint.cssclasses"));
  }

  /**
   * @return <code>true</code> if support for the HTTP "OPTIONS" verb should not
   *         be provided.
   * @since 5.2.6
   */
  public static boolean isHttpOptionsDisabled ()
  {
    // Enable by default
    return _getConfig ().getAsBoolean ("http.method.options.disabled", false);
  }

  /**
   * @return <code>true</code> to enable CSP in general, <code>false</code> to
   *         disable it.
   * @since 5.2.6
   */
  public static boolean isCSPEnabled ()
  {
    return _getConfig ().getAsBoolean ("csp.enabled", true);
  }

  /**
   * @return <code>true</code> if CSP is enabled, errors should only be reported
   *         but the content should not be blocked, <code>false</code> to also
   *         block payload.
   * @since 5.2.6
   */
  public static boolean isCSPReportingOnly ()
  {
    return _getConfig ().getAsBoolean ("csp.reporting.only", false);
  }

  /**
   * @return <code>true</code> if CSP is enabled and it's not "reporting only"
   *         mode, errors should be reported , <code>false</code> to silently
   *         ignore them.
   * @since 5.2.6
   */
  public static boolean isCSPReportingEnabled ()
  {
    return _getConfig ().getAsBoolean ("csp.reporting.enabled", false);
  }
}
