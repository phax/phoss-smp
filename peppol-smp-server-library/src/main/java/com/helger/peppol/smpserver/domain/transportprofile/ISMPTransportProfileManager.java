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
package com.helger.peppol.smpserver.domain.transportprofile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppol.smp.ISMPTransportProfile;

/**
 * Base interface for a manager that handles {@link ISMPTransportProfile}
 * objects.
 *
 * @author Philip Helger
 */
public interface ISMPTransportProfileManager
{
  /**
   * Create a new transport profile.
   *
   * @param sID
   *        The ID to use. May neither be <code>null</code> nor empty.
   * @param sName
   *        The display name of the transport profile. May neither be
   *        <code>null</code> nor empty.
   * @return <code>null</code> if another transport profile with the same ID
   *         already exists.
   */
  @Nullable
  ISMPTransportProfile createSMPTransportProfile (@Nonnull @Nonempty String sID, @Nonnull @Nonempty String sName);

  /**
   * Update an existing transport profile.
   *
   * @param sSMPTransportProfileID
   *        The ID of the transport profile to be updated. May be
   *        <code>null</code>.
   * @param sName
   *        The new name of the transport profile. May neither be
   *        <code>null</code> nor empty.
   * @return {@link EChange#CHANGED} if something was changed.
   */
  @Nonnull
  EChange updateSMPTransportProfile (@Nullable String sSMPTransportProfileID, @Nonnull @Nonempty String sName);

  /**
   * Delete an existing transport profile.
   *
   * @param sSMPTransportProfileID
   *        The ID of the transport profile to be deleted. May be
   *        <code>null</code>.
   * @return {@link EChange#CHANGED} if the removal was successful.
   */
  @Nullable
  EChange removeSMPTransportProfile (@Nullable String sSMPTransportProfileID);

  /**
   * @return An unsorted collection of all contained transport profile. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPTransportProfile> getAllSMPTransportProfiles ();

  /**
   * Get the transport profile with the passed ID.
   *
   * @param sID
   *        The ID to be resolved. May be <code>null</code>.
   * @return <code>null</code> if no such transport profile exists.
   */
  @Nullable
  ISMPTransportProfile getSMPTransportProfileOfID (@Nullable String sID);

  /**
   * Check if a transport profile with the passed ID is contained.
   *
   * @param sID
   *        The ID of the transport profile to be checked. May be
   *        <code>null</code>.
   * @return <code>true</code> if the ID is contained, <code>false</code>
   *         otherwise.
   */
  boolean containsSMPTransportProfileWithID (@Nullable String sID);
}
