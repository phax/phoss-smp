/*
 * Copyright (C) 2014-2024 Philip Helger and contributors
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

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.URLHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.servlet.StaticServerInfo;
import com.helger.smpclient.url.IBDXLURLProvider;
import com.helger.smpclient.url.IPeppolURLProvider;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * {@link ISMPServerAPIDataProvider} implementation based on
 * {@link IRequestWebScopeWithoutResponse} data.
 *
 * @author Philip Helger
 */
public class SMPRestDataProvider implements ISMPServerAPIDataProvider
{
  enum EServerNameMode implements IHasID <String>
  {
    STATIC_SERVER_INFO ("default"),
    REQUEST_SCOPE ("request"),
    DYNAMIC_PARTICIPANT_URL ("dynamic-participant");

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
    m_aRequestScope = aRequestScope;
    m_aParticipantID = SMPMetaManager.getIdentifierFactory ().parseParticipantIdentifier (sServiceGroupID);
    m_sSMLZoneName = SMPMetaManager.getSettings ().getSMLDNSZone ();
    m_sQueryPathPrefix = SMPServerConfiguration.getRESTType ().getQueryPathPrefix ();
  }

  @Nonnull
  private EServerNameMode _getServerNameMode ()
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
  public URI getCurrentURI ()
  {
    final String ret;
    switch (_getServerNameMode ())
    {
      case STATIC_SERVER_INFO:
        // Do not decode params - '#' lets URI parser fail!
        // getRequestURIEncoded contains the context path
        ret = StaticServerInfo.getInstance ().getFullServerPath () + m_aRequestScope.getRequestURIEncoded ();
        break;
      case REQUEST_SCOPE:
        ret = m_aRequestScope.getRequestURLEncoded ().toString ();
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
   * @return An UriBuilder that contains the full server name, port and context
   *         path!
   */
  @Nonnull
  protected String getBaseUriBuilder ()
  {
    final boolean bIsForceRoot = SMPServerConfiguration.isForceRoot ();

    final String ret;
    switch (_getServerNameMode ())
    {
      case STATIC_SERVER_INFO:
        if (bIsForceRoot)
          ret = StaticServerInfo.getInstance ().getFullServerPath ();
        else
          ret = StaticServerInfo.getInstance ().getFullContextPath ();
        break;
      case REQUEST_SCOPE:
        if (bIsForceRoot)
          ret = m_aRequestScope.getFullServerPath ();
        else
          ret = m_aRequestScope.getFullContextPath ();
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
