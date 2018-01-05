/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.mock;

import com.helger.commons.collection.impl.ICommonsList;
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

  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (final ISMPServiceGroup aServiceGroup)
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsList <ISMPRedirect> getAllSMPRedirects ()
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
