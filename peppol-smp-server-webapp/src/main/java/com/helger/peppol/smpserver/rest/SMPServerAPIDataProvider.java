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
package com.helger.peppol.smpserver.rest;

import java.net.URI;

import javax.annotation.Nonnull;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.helger.commons.ValueEnforcer;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;

/**
 * {@link ISMPServerAPIDataProvider} implementation based on {@link UriInfo}
 * data.
 *
 * @author Philip Helger
 */
final class SMPServerAPIDataProvider implements ISMPServerAPIDataProvider
{
  private final UriInfo m_aUriInfo;

  public SMPServerAPIDataProvider (@Nonnull final UriInfo aUriInfo)
  {
    m_aUriInfo = ValueEnforcer.notNull (aUriInfo, "UriInfo");
  }

  @Nonnull
  public URI getCurrentURI ()
  {
    return m_aUriInfo.getAbsolutePath ();
  }

  @Nonnull
  public String getServiceGroupHref (@Nonnull final IParticipantIdentifier aServiceGroupID)
  {
    UriBuilder aBuilder = m_aUriInfo.getBaseUriBuilder ();
    if (SMPServerConfiguration.isForceRoot ())
    {
      // Ensure that no context is emitted by using "replacePath" first!
      aBuilder = aBuilder.replacePath ("");
    }
    return aBuilder.path (ServiceGroupInterface.class)
                   .buildFromEncoded (IdentifierHelper.getIdentifierURIPercentEncoded (aServiceGroupID))
                   .toString ();
  }

  @Nonnull
  public String getServiceMetadataReferenceHref (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                                 @Nonnull final IDocumentTypeIdentifier aDocTypeID)
  {
    UriBuilder aBuilder = m_aUriInfo.getBaseUriBuilder ();
    if (SMPServerConfiguration.isForceRoot ())
    {
      // Ensure that no context is emitted by using "replacePath" first!
      aBuilder = aBuilder.replacePath ("");
    }
    return aBuilder.path (ServiceMetadataInterface.class)
                   .buildFromEncoded (IdentifierHelper.getIdentifierURIPercentEncoded (aServiceGroupID),
                                      IdentifierHelper.getIdentifierURIPercentEncoded (aDocTypeID))
                   .toString ();
  }
}
