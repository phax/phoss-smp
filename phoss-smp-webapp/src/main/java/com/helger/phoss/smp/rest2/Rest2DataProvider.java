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
package com.helger.phoss.smp.rest2;

import java.net.URI;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.url.URLHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.servlet.StaticServerInfo;
import com.helger.servlet.request.RequestHelper;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * {@link ISMPServerAPIDataProvider} implementation based on
 * {@link IRequestWebScopeWithoutResponse} data.
 *
 * @author Philip Helger
 */
public class Rest2DataProvider implements ISMPServerAPIDataProvider
{
  private final IRequestWebScopeWithoutResponse m_aRequestScope;
  private final boolean m_bUseStaticServerInfo;

  public Rest2DataProvider (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    this (aRequestScope, true);
  }

  public Rest2DataProvider (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                            final boolean bUseStaticServerInfo)
  {
    m_aRequestScope = ValueEnforcer.notNull (aRequestScope, "UriInfo");
    m_bUseStaticServerInfo = bUseStaticServerInfo;
  }

  public final boolean isUseStaticServerInfo ()
  {
    return m_bUseStaticServerInfo;
  }

  @Nonnull
  public URI getCurrentURI ()
  {
    String sRet;
    if (m_bUseStaticServerInfo && StaticServerInfo.isSet ())
    {
      // Do not decode params - '#' lets URI parser fail!
      sRet = StaticServerInfo.getInstance ().getFullContextPath () + m_aRequestScope.getRequestURI ();
    }
    else
      sRet = m_aRequestScope.getRequestURL ().toString ();
    return URLHelper.getAsURI (sRet);
  }

  /**
   * @return An UriBuilder that contains the full server name, port and context
   *         path!
   */
  @Nonnull
  protected String getBaseUriBuilder ()
  {
    String ret;
    if (m_bUseStaticServerInfo && StaticServerInfo.isSet ())
    {
      if (SMPServerConfiguration.isForceRoot ())
        ret = StaticServerInfo.getInstance ().getFullServerPath ();
      else
        ret = StaticServerInfo.getInstance ().getFullContextPath ();
    }
    else
    {
      if (SMPServerConfiguration.isForceRoot ())
        ret = RequestHelper.getFullServerName (m_aRequestScope.getRequest ()).toString ();
      else
        ret = m_aRequestScope.getRequestURL ().toString ();
    }
    return ret;
  }

  @Nonnull
  public String getServiceGroupHref (@Nonnull final IParticipantIdentifier aServiceGroupID)
  {
    return getBaseUriBuilder () + "/" + aServiceGroupID.getURIPercentEncoded ();
  }

  @Nonnull
  public String getServiceMetadataReferenceHref (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                                 @Nonnull final IDocumentTypeIdentifier aDocTypeID)
  {
    return getBaseUriBuilder () +
           "/" +
           aServiceGroupID.getURIPercentEncoded () +
           "/services/" +
           aDocTypeID.getURIPercentEncoded ();
  }
}
