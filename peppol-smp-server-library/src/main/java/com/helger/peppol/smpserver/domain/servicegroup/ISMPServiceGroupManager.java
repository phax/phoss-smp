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
package com.helger.peppol.smpserver.domain.servicegroup;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;

/**
 * Base interface for a manager for {@link ISMPServiceGroup} objects.
 *
 * @author Philip Helger
 */
public interface ISMPServiceGroupManager extends ISMPServiceGroupProvider
{
  /**
   * @return A non-<code>null</code> mutable list of callbacks.
   */
  @Nonnull
  @ReturnsMutableObject
  CallbackList <ISMPServiceGroupCallback> serviceGroupCallbacks ();

  /**
   * Create a new service group. The implementation of this class is responsible
   * for creating the service group in the SML!
   *
   * @param sOwnerID
   *        The ID of the owning user. May neither be <code>null</code> nor
   *        empty.
   * @param aParticipantIdentifier
   *        The underlying participant identifier. May not be <code>null</code>.
   * @param sExtension
   *        The optional extension element that must be either a well-formed XML
   *        string (for PEPPOL SMP) or a valid JSON string (for BDXR SMP).
   * @return The created service group object. Never <code>null</code>.
   * @see com.helger.peppol.smp.SMPExtensionConverter
   * @see com.helger.peppol.bdxr.BDXRExtensionConverter
   */
  @Nonnull
  ISMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty String sOwnerID,
                                          @Nonnull IParticipantIdentifier aParticipantIdentifier,
                                          @Nullable String sExtension);

  /**
   * Update an existing service group. Note: the participant ID of a service
   * group cannot be changed.
   *
   * @param sSMPServiceGroupID
   *        The ID of the service group to modify. Maybe <code>null</code>.
   * @param sOwnerID
   *        The ID of the (new) owning user. May neither be <code>null</code>
   *        nor empty.
   * @param sExtension
   *        The optional (new) extension element that must be well-formed XML if
   *        present.
   * @return {@link EChange#CHANGED} if the passed service group is contained
   *         and at least one field was changed, {@link EChange#UNCHANGED}
   *         otherwise.
   */
  @Nonnull
  EChange updateSMPServiceGroup (@Nullable String sSMPServiceGroupID,
                                 @Nonnull @Nonempty String sOwnerID,
                                 @Nullable String sExtension);

  /**
   * Delete an existing service group. If the service group exists and can be
   * deleted, the implementation of this method is responsible to remove all
   * related service information and redirects of the service group. The
   * implementation of this class is responsible for deleting the service group
   * in the SML!
   *
   * @param aParticipantIdentifier
   *        The participant identifier to be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if the passed service group is contained
   *         and was successfully deleted, {@link EChange#UNCHANGED} otherwise.
   * @see ISMPServiceInformationManager#deleteAllSMPServiceInformationOfServiceGroup(ISMPServiceGroup)
   * @see ISMPRedirectManager#deleteAllSMPRedirectsOfServiceGroup(ISMPServiceGroup)
   */
  @Nonnull
  EChange deleteSMPServiceGroup (@Nullable IParticipantIdentifier aParticipantIdentifier);

  /**
   * @return A non-<code>null</code> but maybe empty list of all contained
   *         service groups.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPServiceGroup> getAllSMPServiceGroups ();

  /**
   * Get all service groups that belong to the passed owner ID.
   *
   * @param sOwnerID
   *        The owner ID to search. May be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list of all contained
   *         service groups of the passed owner.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (@Nonnull String sOwnerID);

  /**
   * Get the number of service groups owned by the passed owner.
   *
   * @param sOwnerID
   *        The owner ID to search. May be <code>null</code>.
   * @return A non-negative count. 0 if the passed owner ID is unknown.
   */
  @Nonnegative
  int getSMPServiceGroupCountOfOwner (@Nonnull String sOwnerID);

  /**
   * Check if a service group with the passed participant identifier is
   * contained.
   *
   * @param aParticipantIdentifier
   *        The participant identifier to search. May be <code>null</code>.
   * @return <code>true</code> if the participant identifier is not
   *         <code>null</code> and contained.
   */
  boolean containsSMPServiceGroupWithID (@Nullable IParticipantIdentifier aParticipantIdentifier);

  /**
   * @return The total number of contained service groups. Always &ge; 0.
   */
  @Nonnegative
  int getSMPServiceGroupCount ();
}
