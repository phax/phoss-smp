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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.StringParser;
import com.helger.commons.url.URLHelper;
import com.helger.http.HttpForwardedHeaderHop;
import com.helger.http.HttpForwardedHeaderParser;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.servlet.ServletHelper;
import com.helger.servlet.StaticServerInfo;
import com.helger.servlet.request.RequestHelper;
import com.helger.smpclient.url.IBDXLURLProvider;
import com.helger.smpclient.url.IPeppolURLProvider;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.smpclient.url.PeppolConfigurableURLProvider;
import com.helger.smpclient.url.PeppolURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

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
    FORWARDED_HEADER ("forwarded-header"),
    /**
     * Use the Peppol CNAME record. Does not work with NAPTR records
     */
    @Deprecated (forRemoval = true, since = "7.2.7")
    DYNAMIC_PARTICIPANT_URL("dynamic-participant");

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

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPRestDataProvider.class);
  private static final String HTTP_X_FORWARDED_PROTO = "X-Forwarded-Proto";
  private static final String HTTP_X_FORWARDED_HOST = "X-Forwarded-Host";
  private static final String HTTP_X_FORWARDED_PORT = "X-Forwarded-Port";

  private final EServerNameMode m_eServerNameMode;
  private final IRequestWebScopeWithoutResponse m_aRequestScope;
  private final IParticipantIdentifier m_aParticipantID;
  private final String m_sSMLZoneName;
  private final String m_sQueryPathPrefix;

  public SMPRestDataProvider (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                              @Nullable final String sServiceGroupID)
  {
    ValueEnforcer.notNull (aRequestScope, "RequestScope");
    m_eServerNameMode = EServerNameMode.getFromIDOrDefault (SMPServerConfiguration.getPublicServerURLMode ());
    if (m_eServerNameMode == EServerNameMode.DYNAMIC_PARTICIPANT_URL)
    {
      LOGGER.warn ("The public URL mode '" +
                   m_eServerNameMode.getID () +
                   "' is deprecated and will be removed soon, because it does not work with NAPTR based lookup");
    }

    m_aRequestScope = aRequestScope;
    m_aParticipantID = SMPMetaManager.getIdentifierFactory ().parseParticipantIdentifier (sServiceGroupID);
    m_sSMLZoneName = SMPMetaManager.getSettings ().getSMLDNSZone ();
    m_sQueryPathPrefix = SMPServerConfiguration.getRESTType ().getQueryPathPrefix ();
  }

  @SuppressWarnings ("removal")
  @Nonnull
  private EServerNameMode _getEffectiveServerNameMode ()
  {
    if (m_eServerNameMode == EServerNameMode.STATIC_SERVER_INFO)
    {
      // Check if initialized
      if (!StaticServerInfo.isSet ())
        return EServerNameMode.FALLBACK;
    }
    else
      if (m_eServerNameMode == EServerNameMode.DYNAMIC_PARTICIPANT_URL)
      {
        // Check if feasible
        if (m_aParticipantID == null || StringHelper.hasNoText (m_sSMLZoneName))
          return EServerNameMode.FALLBACK;

        // This only works with Peppol CNAME URL provider
        final ISMPURLProvider aURLProvider = SMPMetaManager.getSMPURLProvider ();
        if (aURLProvider instanceof PeppolURLProvider ||
            (aURLProvider instanceof PeppolConfigurableURLProvider && !PeppolConfigurableURLProvider.USE_NATPR.get ()))
        {
          // continue
        }
        else
          return EServerNameMode.FALLBACK;
      }

    // As is
    return m_eServerNameMode;
  }

  @Nonnull
  private String _getDynamicParticipantURLHostName ()
  {
    String ret = null;
    final ISMPURLProvider aURLProvider = SMPMetaManager.getSMPURLProvider ();
    try
    {
      if (aURLProvider instanceof IPeppolURLProvider)
        ret = ((IPeppolURLProvider) aURLProvider).getDNSNameOfParticipant (m_aParticipantID, m_sSMLZoneName);
      else
        if (aURLProvider instanceof IBDXLURLProvider)
        {
          // Fallback by not resolving the NAPTR
          ret = ((IBDXLURLProvider) aURLProvider).getDNSNameOfParticipant (m_aParticipantID, m_sSMLZoneName);
        }
    }
    catch (final SMPDNSResolutionException ex)
    {
      // Ignore
    }

    if (ret == null)
      throw new IllegalStateException ("Failed to resolve host name for '" +
                                       m_aParticipantID.getURIEncoded () +
                                       "' and SML zone name '" +
                                       m_sSMLZoneName +
                                       "'");
    return ret;
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
    if (StringHelper.hasText (sScheme))
      bFallbackToLocalPort = false;
    else
      sScheme = ServletHelper.getRequestScheme (aHttpRequest);

    // Host
    String sHost = m_aRequestScope.headers ().getFirstHeaderValue (HTTP_X_FORWARDED_HOST);
    if (StringHelper.hasText (sHost))
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
        if (StringHelper.hasText (sScheme))
          break;
      }
    if (!StringHelper.hasText (sScheme))
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
        if (StringHelper.hasText (sHost))
          break;
      }
    if (!StringHelper.hasText (sHost))
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
      case DYNAMIC_PARTICIPANT_URL:
        ret = m_aRequestScope.getScheme () +
              "://" +
              _getDynamicParticipantURLHostName () +
              m_aRequestScope.getRequestURIEncoded ();
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
      case DYNAMIC_PARTICIPANT_URL:
        ret = m_aRequestScope.getScheme () +
              "://" +
              _getDynamicParticipantURLHostName () +
              (bIsForceRoot ? "" : m_aRequestScope.getContextPath ());
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
