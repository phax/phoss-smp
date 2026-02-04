/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectCallback;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;

/**
 * Mock implementation of {@link ISMPRedirectManager}.
 *
 * @author Philip Helger
 */
final class MockSMPRedirectManager implements ISMPRedirectManager
{
  private final CallbackList <ISMPRedirectCallback> m_aCallbacks = new CallbackList <> ();

  @NonNull
  @ReturnsMutableObject
  public CallbackList <ISMPRedirectCallback> redirectCallbacks ()
  {
    return m_aCallbacks;
  }

  public ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (final IParticipantIdentifier aParticipantID,
                                                                   final IDocumentTypeIdentifier aDocTypeID)
  {
    throw new UnsupportedOperationException ();
  }

  public long getSMPRedirectCount ()
  {
    return 0;
  }

  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (final IParticipantIdentifier aParticipantID)
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

  public EChange deleteAllSMPRedirectsOfServiceGroup (final IParticipantIdentifier aParticipantID)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPRedirect createOrUpdateSMPRedirect (final IParticipantIdentifier aParticipantID,
                                                 final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                 final String sTargetHref,
                                                 final String sSubjectUniqueIdentifier,
                                                 final X509Certificate aCertificate,
                                                 final String sExtension)
  {
    throw new UnsupportedOperationException ();
  }
}
