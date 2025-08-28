/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.rest;

import java.net.URI;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringParser;
import com.helger.base.url.URLHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.http.header.specific.HttpForwardedHeaderHop;
import com.helger.http.header.specific.HttpForwardedHeaderParser;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.servlet.ServletHelper;
import com.helger.servlet.StaticServerInfo;
import com.helger.servlet.request.RequestHelper;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

/**
 * {@link ISMPServerAPIDataProvider} implementation based on {@link IRequestWebScopeWithoutResponse}
 * data.
 *
 * @author Philip Helger
 */
public class SMPRestDataProvider implements ISMPServerAPIDataProvider
{
  enum EServerNameMode implements IHasID <String>
  {
    /**
     * Use the information from the {@link StaticServerInfo}
     */
    STATIC_SERVER_INFO ("default"),
    /**
     * Use the information from the {@link IRequestWebScopeWithoutResponse}
     */
    REQUEST_SCOPE ("request"),
    /**
     * Use the information from the "X-Forwarded-*" HTTP headers including fallback to
     * {@link IRequestWebScopeWithoutResponse} data
     * 
     * @since 7.2.8
     */
    X_FORWARDED_HEADER ("x-forwarded-header"),
    /**
     * Use the information from the "Forwarded" HTTP header including fallback to
     * {@link IRequestWebScopeWithoutResponse} data
     * 
     * @since 7.2.8
     */
    FORWARDED_HEADER ("forwarded-header");

    public static final EServerNameMode DEFAULT = STATIC_SERVER_INFO;
    public static final EServerNameMode FALLBACK = REQUEST_SCOPE;

    private final String m_sID;

    EServerNameMode (@Nonnull @Nonempty final String sID)
    {
      m_sID = sID;
    }

    @Nonnull
    @Nonempty
    public String getID ()
    {
      return m_sID;
    }

    @Nonnull
    public static EServerNameMode getFromIDOrDefault (@Nullable final String sID)
    {
      return EnumHelper.getFromIDOrDefault (EServerNameMode.class, sID, DEFAULT);
    }
  }

  private static final String HTTP_X_FORWARDED_PROTO = "X-Forwarded-Proto";
  private static final String HTTP_X_FORWARDED_HOST = "X-Forwarded-Host";
  private static final String HTTP_X_FORWARDED_PORT = "X-Forwarded-Port";

  private final EServerNameMode m_eServerNameMode;
  private final IRequestWebScopeWithoutResponse m_aRequestScope;
  private final String m_sQueryPathPrefix;

  public SMPRestDataProvider (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    ValueEnforcer.notNull (aRequestScope, "RequestScope");
    m_eServerNameMode = EServerNameMode.getFromIDOrDefault (SMPServerConfiguration.getPublicServerURLMode ());

    m_aRequestScope = aRequestScope;
    m_sQueryPathPrefix = SMPServerConfiguration.getRESTType ().getQueryPathPrefix ();
  }

  @Nonnull
  private EServerNameMode _getEffectiveServerNameMode ()
  {
    if (m_eServerNameMode == EServerNameMode.STATIC_SERVER_INFO)
    {
      // Check if initialized
      if (!StaticServerInfo.isSet ())
        return EServerNameMode.FALLBACK;
    }

    // As is
    return m_eServerNameMode;
  }

  @Nonnull
  private StringBuilder _getXForwardedHeaderBasedHostName ()
  {
    // Try to use as many "X-Forwarded-*" header values as possible. The parts not present will
    // be replaced with
    final HttpServletRequest aHttpRequest = m_aRequestScope.getRequest ();
    boolean bFallbackToLocalPort = false;

    // Scheme
    String sScheme = m_aRequestScope.headers ().getFirstHeaderValue (HTTP_X_FORWARDED_PROTO);
    if (StringHelper.isNotEmpty (sScheme))
      bFallbackToLocalPort = false;
    else
      sScheme = ServletHelper.getRequestScheme (aHttpRequest);

    // Host
    String sHost = m_aRequestScope.headers ().getFirstHeaderValue (HTTP_X_FORWARDED_HOST);
    if (StringHelper.isNotEmpty (sHost))
      bFallbackToLocalPort = false;
    else
      sHost = ServletHelper.getRequestServerName (aHttpRequest);

    // Port
    int nPort = StringParser.parseInt (m_aRequestScope.headers ().getFirstHeaderValue (HTTP_X_FORWARDED_PORT), -1);
    // If no fallback to local port should be performed, the default port of the selected scheme
    // is used
    if (nPort < 0 && bFallbackToLocalPort)
      nPort = ServletHelper.getRequestServerPort (aHttpRequest);

    // Build result string
    return RequestHelper.getFullServerName (sScheme, sHost, nPort);
  }

  @Nonnull
  private StringBuilder _getForwardedHeaderBasedHostName ()
  {
    // Get the hops in reverse order, so that the last hop is in front and we can iterate forward
    // based
    final ICommonsList <HttpForwardedHeaderHop> aMultiHops = HttpForwardedHeaderParser.parseMultipleHops (m_aRequestScope.headers ()
                                                                                                                         .getFirstHeaderValue (HttpForwardedHeaderParser.HTTP_HEADER_FORWARDED))
                                                                                      .reverse ();

    final HttpServletRequest aHttpRequest = m_aRequestScope.getRequest ();

    // Scheme
    String sScheme = null;
    for (final HttpForwardedHeaderHop aHop : aMultiHops)
      if (aHop.containsProto ())
      {
        sScheme = aHop.getProto ();
        if (StringHelper.isNotEmpty (sScheme))
          break;
      }
    if (!StringHelper.isNotEmpty (sScheme))
      sScheme = ServletHelper.getRequestScheme (aHttpRequest);

    // Host and port
    String sHost = null;
    int nPort = -1;
    for (final HttpForwardedHeaderHop aHop : aMultiHops)
      if (aHop.containsHost ())
      {
        // Host may contain port number as well
        final String sFullHost = aHop.getHost ();

        final int nPortSep = sFullHost.indexOf (':');
        if (nPortSep > 0)
        {
          sHost = sFullHost.substring (0, nPortSep);
          nPort = StringParser.parseInt (sFullHost.substring (1), -1);
        }
        else
          sHost = sFullHost;
        if (StringHelper.isNotEmpty (sHost))
          break;
      }
    if (!StringHelper.isNotEmpty (sHost))
    {
      sHost = ServletHelper.getRequestServerName (aHttpRequest);
      nPort = ServletHelper.getRequestServerPort (aHttpRequest);
    }

    // Build result string
    return RequestHelper.getFullServerName (sScheme, sHost, nPort);
  }

  @Nonnull
  public URI getCurrentURI ()
  {
    final String ret;
    switch (_getEffectiveServerNameMode ())
    {
      case STATIC_SERVER_INFO:
        // Do not decode params - '#' lets URI parser fail!
        // getRequestURIEncoded contains the context path
        ret = StaticServerInfo.getInstance ().getFullServerPath () + m_aRequestScope.getRequestURIEncoded ();
        break;
      case REQUEST_SCOPE:
        ret = m_aRequestScope.getRequestURLEncoded ().toString ();
        break;
      case X_FORWARDED_HEADER:
        ret = _getXForwardedHeaderBasedHostName ().append (m_aRequestScope.getRequestURIEncoded ()).toString ();
        break;
      case FORWARDED_HEADER:
        ret = _getForwardedHeaderBasedHostName ().append (m_aRequestScope.getRequestURIEncoded ()).toString ();
        break;
      default:
        throw new IllegalStateException ("Unhandled server name mode");
    }
    return URLHelper.getAsURI (ret);
  }

  /**
   * @return An UriBuilder that contains the full server name, port and context path!
   */
  @Nonnull
  protected String getBaseUriBuilder ()
  {
    final boolean bIsForceRoot = SMPServerConfiguration.isForceRoot ();

    final String ret;
    switch (_getEffectiveServerNameMode ())
    {
      case STATIC_SERVER_INFO:
        ret = StaticServerInfo.getInstance ().getFullServerPath () +
              (bIsForceRoot ? "" : m_aRequestScope.getContextPath ());
        break;
      case REQUEST_SCOPE:
        ret = m_aRequestScope.getFullServerPath () + (bIsForceRoot ? "" : m_aRequestScope.getContextPath ());
        break;
      case X_FORWARDED_HEADER:
        ret = _getXForwardedHeaderBasedHostName ().append (bIsForceRoot ? "" : m_aRequestScope.getContextPath ())
                                                  .toString ();
        break;
      case FORWARDED_HEADER:
        ret = _getForwardedHeaderBasedHostName ().append (bIsForceRoot ? "" : m_aRequestScope.getContextPath ())
                                                 .toString ();
        break;
      default:
        throw new IllegalStateException ("Unhandled server name mode");
    }
    return ret;
  }

  @Nonnull
  public String getServiceGroupHref (@Nonnull final IParticipantIdentifier aServiceGroupID)
  {
    return getBaseUriBuilder () + "/" + m_sQueryPathPrefix + aServiceGroupID.getURIPercentEncoded ();
  }

  @Nonnull
  public String getServiceMetadataReferenceHref (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                                 @Nonnull final IDocumentTypeIdentifier aDocTypeID)
  {
    return getBaseUriBuilder () +
           "/" +
           m_sQueryPathPrefix +
           aServiceGroupID.getURIPercentEncoded () +
           SMPRestFilter.PATH_SERVICES +
           aDocTypeID.getURIPercentEncoded ();
  }
}
