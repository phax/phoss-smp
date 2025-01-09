/*
 * Copyright (C) 2015-2024 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.mock;

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;

/**
 * Mock implementation of {@link ISMPServiceInformationManager}.
 *
 * @author Philip Helger
 */
final class MockSMPServiceInformationManager implements ISMPServiceInformationManager
{
  private final CallbackList <ISMPServiceInformationCallback> m_aCBs = new CallbackList <> ();

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceInformationCallback> serviceInformationCallbacks ()
  {
    return m_aCBs;
  }

  @Nonnull
  public ESuccess mergeSMPServiceInformation (final ISMPServiceInformation aServiceInformation)
  {
    return ESuccess.SUCCESS;
  }

  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (final IParticipantIdentifier aParticipantIdentifier,
                                                                                       final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    throw new UnsupportedOperationException ();
  }

  public long getSMPServiceInformationCount ()
  {
    return 0;
  }

  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformationOfServiceGroup (final IParticipantIdentifier aParticipantID)
  {
    throw new UnsupportedOperationException ();
  }

  public void forEachSMPServiceInformation (@Nonnull final Consumer <? super ISMPServiceInformation> aConsumer)
  {
    throw new UnsupportedOperationException ();
  }

  @Nonnull
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation ()
  {
    // Avoid exceptions in test for system migration
    // "ensure-transport-profiles-128"
    return new CommonsArrayList <> ();
  }

  public ICommonsList <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (final IParticipantIdentifier aParticipantID)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPServiceInformation findServiceInformation (final ISMPServiceGroup aServiceGroup,
                                                        final IDocumentTypeIdentifier aDocTypeID,
                                                        final IProcessIdentifier aProcessID,
                                                        final ISMPTransportProfile aTransportProfile)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteSMPServiceInformation (final ISMPServiceInformation aSMPServiceInformation)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteAllSMPServiceInformationOfServiceGroup (final IParticipantIdentifier aParticipantID)
  {
    throw new UnsupportedOperationException ();
  }

  @Nonnull
  public EChange deleteSMPProcess (@Nullable final ISMPServiceInformation aSMPServiceInformation,
                                   @Nullable final ISMPProcess aProcess)
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsAnyEndpointWithTransportProfile (@Nullable final String sTransportProfileID)
  {
    return false;
  }
}
