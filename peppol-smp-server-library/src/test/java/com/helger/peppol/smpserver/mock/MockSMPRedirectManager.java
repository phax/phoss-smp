package com.helger.peppol.smpserver.mock;

import com.helger.commons.collection.ext.ICommonsCollection;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * Mock implementation of {@link ISMPRedirectManager}.
 * 
 * @author Philip Helger
 */
final class MockSMPRedirectManager implements ISMPRedirectManager
{
  public ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (final ISMPServiceGroup aServiceGroup,
                                                                   final IDocumentTypeIdentifier aDocTypeID)
  {
    throw new UnsupportedOperationException ();
  }

  public int getSMPRedirectCount ()
  {
    return 0;
  }

  public ICommonsCollection <? extends ISMPRedirect> getAllSMPRedirectsOfServiceGroup (final ISMPServiceGroup aServiceGroup)
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsCollection <? extends ISMPRedirect> getAllSMPRedirects ()
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteSMPRedirect (final ISMPRedirect aSMPRedirect)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteAllSMPRedirectsOfServiceGroup (final ISMPServiceGroup aServiceGroup)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPRedirect createOrUpdateSMPRedirect (final ISMPServiceGroup aServiceGroup,
                                                 final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                 final String sTargetHref,
                                                 final String sSubjectUniqueIdentifier,
                                                 final String sExtension)
  {
    throw new UnsupportedOperationException ();
  }
}
