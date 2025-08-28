/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.config;

import java.net.Proxy;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.network.proxy.settings.IProxySettings;
import com.helger.network.proxy.settings.ProxySettings;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This class provides easy access to certain configuration properties using
 * {@link SMPConfigProvider}.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class SMPHttpConfiguration
{
  private SMPHttpConfiguration ()
  {}

  /**
   * @return The configuration file. Never <code>null</code>.
   */
  @Nonnull
  private static IConfigWithFallback _getConfig ()
  {
    return SMPConfigProvider.getConfig ();
  }

  /**
   * @return The proxy host to be used for "http" calls. May be <code>null</code>.
   * @see #getHttpsProxyHost()
   * @since 5.0.7
   */
  @Nullable
  public static String getHttpProxyHost ()
  {
    return _getConfig ().getAsString ("http.proxyHost");
  }

  /**
   * @return The proxy port to be used for "http" calls. Defaults to 0.
   * @see #getHttpsProxyPort()
   * @since 5.0.7
   */
  public static int getHttpProxyPort ()
  {
    return _getConfig ().getAsInt ("http.proxyPort", 0);
  }

  /**
   * @return The proxy host to be used for "https" calls. May be <code>null</code>.
   * @see #getHttpProxyHost()
   * @since 5.0.7
   */
  @Nullable
  public static String getHttpsProxyHost ()
  {
    return _getConfig ().getAsString ("https.proxyHost");
  }

  /**
   * @return The proxy port to be used for "https" calls. Defaults to 0.
   * @see #getHttpProxyPort()
   * @since 5.0.7
   */
  public static int getHttpsProxyPort ()
  {
    return _getConfig ().getAsInt ("https.proxyPort", 0);
  }

  /**
   * @return The username for proxy calls. Valid for https and https proxy. May be
   *         <code>null</code>.
   * @since 5.0.7
   */
  @Nullable
  public static String getProxyUsername ()
  {
    return _getConfig ().getAsString ("proxy.username");
  }

  /**
   * @return The password for proxy calls. Valid for https and https proxy. May be
   *         <code>null</code>.
   * @since 5.0.7
   */
  @Nullable
  public static String getProxyPassword ()
  {
    return _getConfig ().getAsString ("proxy.password");
  }

  /**
   * @return A single object for all http (but not https) proxy settings. May be <code>null</code>.
   * @see #getAsHttpsProxySettings()
   * @since 5.0.7
   */
  @Nullable
  public static IProxySettings getAsHttpProxySettings ()
  {
    final String sHostname = getHttpProxyHost ();
    final int nPort = getHttpProxyPort ();
    if (sHostname != null && nPort > 0)
      return new ProxySettings (Proxy.Type.HTTP, sHostname, nPort, getProxyUsername (), getProxyPassword ());
    return null;
  }

  /**
   * @return A single object for all https (but not http) proxy settings. May be <code>null</code>.
   * @see #getAsHttpProxySettings()
   * @since 5.0.7
   */
  @Nullable
  public static IProxySettings getAsHttpsProxySettings ()
  {
    final String sHostname = getHttpsProxyHost ();
    final int nPort = getHttpsProxyPort ();
    if (sHostname != null && nPort > 0)
      return new ProxySettings (Proxy.Type.HTTP, sHostname, nPort, getProxyUsername (), getProxyPassword ());
    return null;
  }
}
