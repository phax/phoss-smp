package com.helger.peppol.smpserver.rest;

import java.net.URI;

import javax.annotation.Nonnull;
import javax.ws.rs.core.UriInfo;

import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IdentifierUtils;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;

final class SMPServerAPIDataProvider implements ISMPServerAPIDataProvider
{
  private final UriInfo m_aUriInfo;

  public SMPServerAPIDataProvider (@Nonnull final UriInfo aUriInfo)
  {
    m_aUriInfo = aUriInfo;
  }

  @Nonnull
  public URI getCurrentURI ()
  {
    return m_aUriInfo.getAbsolutePath ();
  }

  @Nonnull
  public String getServiceGroupHref (@Nonnull final IParticipantIdentifier aServiceGroupID)
  {
    // Ensure that no context is emitted by using "replacePath" first!
    return m_aUriInfo.getBaseUriBuilder ()
                     .replacePath ("")
                     .path (ServiceGroupInterface.class)
                     .buildFromEncoded (IdentifierUtils.getIdentifierURIPercentEncoded (aServiceGroupID))
                     .toString ();
  }

  @Nonnull
  public String getServiceMetadataReferenceHref (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                                 @Nonnull final IDocumentTypeIdentifier aDocTypeID)
  {
    // Ensure that no context is emitted by using "replacePath" first!
    return m_aUriInfo.getBaseUriBuilder ()
                     .replacePath ("")
                     .path (ServiceMetadataInterface.class)
                     .buildFromEncoded (IdentifierUtils.getIdentifierURIPercentEncoded (aServiceGroupID),
                                        IdentifierUtils.getIdentifierURIPercentEncoded (aDocTypeID))
                     .toString ();
  }
}
