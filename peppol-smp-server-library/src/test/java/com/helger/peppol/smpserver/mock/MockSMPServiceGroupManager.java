/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.mock;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.ext.ICommonsList;
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
  private final CallbackList <ISMPServiceGroupCallback> m_aCBs = new CallbackList<> ();

  @Nonnull
  @ReturnsMutableObject ("by design")
  public CallbackList <ISMPServiceGroupCallback> getServiceGroupCallbacks ()
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
