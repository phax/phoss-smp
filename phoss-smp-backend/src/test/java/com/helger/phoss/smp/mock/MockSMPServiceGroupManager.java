/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.mock;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
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

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceGroupCallback> serviceGroupCallbacks ()
  {
    return m_aCBs;
  }

  public ISMPServiceGroup createSMPServiceGroup (final String sOwnerID,
                                                 final IParticipantIdentifier aParticipantID,
                                                 final String sExtension)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange updateSMPServiceGroup (final IParticipantIdentifier aParticipantID, final String sOwnerID, final String sExtension)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteSMPServiceGroup (final IParticipantIdentifier aParticipantID)
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

  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (final String sOwnerID)
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsSMPServiceGroupWithID (final IParticipantIdentifier aParticipantIdentifier)
  {
    return false;
  }
}
