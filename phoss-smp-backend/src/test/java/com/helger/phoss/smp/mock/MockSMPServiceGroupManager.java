/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.mock;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupCallback;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;

/**
 * Mock implementation of {@link ISMPServiceGroupManager}.
 *
 * @author Philip Helger
 */
final class MockSMPServiceGroupManager implements ISMPServiceGroupManager
{
  private final CallbackList <ISMPServiceGroupCallback> m_aCBs = new CallbackList <> ();

  @NonNull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceGroupCallback> serviceGroupCallbacks ()
  {
    return m_aCBs;
  }

  public ISMPServiceGroup createSMPServiceGroup (final String sOwnerID,
                                                 final IParticipantIdentifier aParticipantID,
                                                 final String sExtension,
                                                 final boolean bCreateInSML)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange updateSMPServiceGroup (final IParticipantIdentifier aParticipantID,
                                        final String sOwnerID,
                                        final String sExtension)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteSMPServiceGroup (final IParticipantIdentifier aParticipantID, final boolean bDeleteInSML)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPServiceGroup getSMPServiceGroupOfID (final IParticipantIdentifier aParticipantIdentifier)
  {
    throw new UnsupportedOperationException ();
  }

  public long getSMPServiceGroupCountOfOwner (final String sOwnerID)
  {
    return 0;
  }

  public long getSMPServiceGroupCount ()
  {
    return 0;
  }

  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsSet <String> getAllSMPServiceGroupIDs ()
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (final String sOwnerID)
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsSMPServiceGroupWithID (final IParticipantIdentifier aParticipantIdentifier)
  {
    return false;
  }
}
