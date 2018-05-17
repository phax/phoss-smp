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
package com.helger.peppol.smpserver.domain.transportprofile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
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
   * @param bIsDeprecated
   *        <code>true</code> if the profile is deprecated, <code>false</code>
   *        if not
   * @return <code>null</code> if another transport profile with the same ID
   *         already exists.
   */
  @Nullable
  ISMPTransportProfile createSMPTransportProfile (@Nonnull @Nonempty String sID,
                                                  @Nonnull @Nonempty String sName,
                                                  boolean bIsDeprecated);

  /**
   * Update an existing transport profile.
   *
   * @param sSMPTransportProfileID
   *        The ID of the transport profile to be updated. May be
   *        <code>null</code>.
   * @param sName
   *        The new name of the transport profile. May neither be
   *        <code>null</code> nor empty.
   * @param bIsDeprecated
   *        <code>true</code> if the profile is deprecated, <code>false</code>
   *        if not
   * @return {@link EChange#CHANGED} if something was changed.
   */
  @Nonnull
  EChange updateSMPTransportProfile (@Nullable String sSMPTransportProfileID,
                                     @Nonnull @Nonempty String sName,
                                     boolean bIsDeprecated);

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
