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

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupCallback;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;

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

  public EChange updateSMPServiceGroup (final String sSMPServiceGroupID, final String sOwnerID, final String sExtension)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPServiceGroup getSMPServiceGroupOfID (final IParticipantIdentifier aParticipantIdentifier)
  {
    throw new UnsupportedOperationException ();
  }

  public int getSMPServiceGroupCountOfOwner (final String sOwnerID)
  {
    return 0;
  }

  public int getSMPServiceGroupCount ()
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

  public EChange deleteSMPServiceGroup (final IParticipantIdentifier aParticipantIdentifier)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPServiceGroup createSMPServiceGroup (final String sOwnerID,
                                                 final IParticipantIdentifier aParticipantIdentifier,
                                                 final String sExtension)
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsSMPServiceGroupWithID (final IParticipantIdentifier aParticipantIdentifier)
  {
    return false;
  }
}
