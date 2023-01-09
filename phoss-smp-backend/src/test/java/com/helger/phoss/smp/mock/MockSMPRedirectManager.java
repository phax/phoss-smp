/*
 * Copyright (C) 2015-2023 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.mock;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectCallback;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;

/**
 * Mock implementation of {@link ISMPRedirectManager}.
 *
 * @author Philip Helger
 */
final class MockSMPRedirectManager implements ISMPRedirectManager
{
  private final CallbackList <ISMPRedirectCallback> m_aCallbacks = new CallbackList <> ();

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPRedirectCallback> redirectCallbacks ()
  {
    return m_aCallbacks;
  }

  public ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (final ISMPServiceGroup aServiceGroup,
                                                                   final IDocumentTypeIdentifier aDocTypeID)
  {
    throw new UnsupportedOperationException ();
  }

  public long getSMPRedirectCount ()
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
                                                 final X509Certificate aCertificate,
                                                 final String sExtension)
  {
    throw new UnsupportedOperationException ();
  }
}
