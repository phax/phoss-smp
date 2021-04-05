/**
 * Copyright (C) 2015-2021 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.servicegroup;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.exception.SMPServerException;

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
   *        string (for Peppol SMP) or a valid JSON string (for BDXR SMP).
   * @param bCreateInSML
   *        <code>true</code> if the service group should also be created in the
   *        SML, <code>false</code> if not.
   * @return The created service group object. Never <code>null</code>.
   * @throws SMPServerException
   *         In case of error
   * @see com.helger.smpclient.peppol.utils.SMPExtensionConverter
   * @see com.helger.smpclient.bdxr1.utils.BDXR1ExtensionConverter
   */
  @Nonnull
  ISMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty String sOwnerID,
                                          @Nonnull IParticipantIdentifier aParticipantIdentifier,
                                          @Nullable String sExtension,
                                          boolean bCreateInSML) throws SMPServerException;

  /**
   * Update an existing service group. Note: the participant ID of a service
   * group cannot be changed.
   *
   * @param aParticipantIdentifier
   *        The ID of the service group to modify. May not be <code>null</code>.
   * @param sOwnerID
   *        The ID of the (new) owning user. May neither be <code>null</code>
   *        nor empty.
   * @param sExtension
   *        The optional (new) extension element that must be well-formed XML if
   *        present.
   * @throws SMPServerException
   *         In case of error
   * @return {@link EChange#CHANGED} if the passed service group is contained
   *         and at least one field was changed, {@link EChange#UNCHANGED}
   *         otherwise.
   */
  @Nonnull
  EChange updateSMPServiceGroup (@Nonnull IParticipantIdentifier aParticipantIdentifier,
                                 @Nonnull @Nonempty String sOwnerID,
                                 @Nullable String sExtension) throws SMPServerException;

  @Nonnull
  default EChange updateSMPServiceGroupNoEx (@Nonnull final IParticipantIdentifier aParticipantIdentifier,
                                             @Nonnull @Nonempty final String sOwnerID,
                                             @Nullable final String sExtension)
  {
    try
    {
      return updateSMPServiceGroup (aParticipantIdentifier, sOwnerID, sExtension);
    }
    catch (final SMPServerException ex)
    {
      return EChange.UNCHANGED;
    }
  }

  /**
   * Delete an existing service group. If the service group exists and can be
   * deleted, the implementation of this method is responsible to remove all
   * related service information and redirects of the service group. The
   * implementation of this class is responsible for deleting the service group
   * in the SML!
   *
   * @param aParticipantIdentifier
   *        The participant identifier to be deleted. May not be
   *        <code>null</code>.
   * @param bDeleteInSML
   *        <code>true</code> if the service group should also be deleted in the
   *        SML, <code>false</code> if not.
   * @return {@link EChange#CHANGED} if the passed service group is contained
   *         and was successfully deleted, {@link EChange#UNCHANGED} otherwise.
   * @throws SMPServerException
   *         In case of error
   * @see ISMPServiceInformationManager#deleteAllSMPServiceInformationOfServiceGroup(ISMPServiceGroup)
   * @see ISMPRedirectManager#deleteAllSMPRedirectsOfServiceGroup(ISMPServiceGroup)
   */
  @Nonnull
  EChange deleteSMPServiceGroup (@Nonnull IParticipantIdentifier aParticipantIdentifier, boolean bDeleteInSML) throws SMPServerException;

  /**
   * Delete the service group, and swallow all exceptions. This is only
   * recommended for unit tests.
   *
   * @param aParticipantIdentifier
   *        The participant identifier to be deleted. May not be
   *        <code>null</code>.
   * @param bDeleteInSML
   *        <code>true</code> if the service group should also be deleted in the
   *        SML, <code>false</code> if not.
   * @return {@link EChange#CHANGED} if the passed service group is contained
   *         and was successfully deleted, {@link EChange#UNCHANGED} otherwise.
   * @see #deleteSMPServiceGroup(IParticipantIdentifier, boolean)
   */
  @Nonnull
  default EChange deleteSMPServiceGroupNoEx (@Nonnull final IParticipantIdentifier aParticipantIdentifier, final boolean bDeleteInSML)
  {
    try
    {
      return deleteSMPServiceGroup (aParticipantIdentifier, bDeleteInSML);
    }
    catch (final SMPServerException ex)
    {
      return EChange.UNCHANGED;
    }
  }

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
  long getSMPServiceGroupCountOfOwner (@Nonnull String sOwnerID);

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
   * @return The total number of contained service groups. May be &lt; 0 in case
   *         there was an error querying (e.g. because of missing SQL backend).
   */
  @CheckForSigned
  long getSMPServiceGroupCount ();
}
