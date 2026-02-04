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
package com.helger.phoss.smp.domain.sml;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.sml.ISMLInfo;

/**
 * Base interface for a manager that handles {@link ISMLInfo} objects.
 *
 * @author Philip Helger
 */
public interface ISMLInfoManager
{
  /**
   * Create a new SML information.
   *
   * @param sDisplayName
   *        The "shorthand" display name like "SML" or "SMK". May neither be <code>null</code> nor
   *        empty.
   * @param sDNSZone
   *        The DNS zone on which this SML is operating. May not be <code>null</code>. It must be
   *        ensured that the value consists only of lower case characters for comparability!
   *        Example: <code>acc.edelivery.tech.ec.europa.eu</code>
   * @param sManagementServiceURL
   *        The service URL where the management application is running on incl. the host name. May
   *        not be <code>null</code>. The difference to the host name is the eventually present
   *        context path.
   * @param sURLSuffixManageSMP
   *        The sub-path to be used for managing SMP data in the SML. May not be <code>null</code>,
   *        may be empty. If not empty it must start with a slash ("/").
   * @param sURLSuffixManageParticipant
   *        The sub-path to be used for managing participant data in the SML. May not be
   *        <code>null</code>, may be empty. If not empty it must start with a slash ("/").
   * @param bClientCertificateRequired
   *        <code>true</code> if this SML requires a client certificate for access,
   *        <code>false</code> otherwise.<br>
   *        Both Peppol production SML and SMK require a client certificate. Only a locally running
   *        SML software may not require a client certificate.
   * @return Never <code>null</code>.
   */
  @NonNull
  ISMLInfo createSMLInfo (@NonNull @Nonempty String sDisplayName,
                          @NonNull @Nonempty String sDNSZone,
                          @NonNull @Nonempty String sManagementServiceURL,
                          @NonNull String sURLSuffixManageSMP,
                          @NonNull String sURLSuffixManageParticipant,
                          boolean bClientCertificateRequired);

  /**
   * Update an existing SML information.
   *
   * @param sSMLInfoID
   *        The ID of the SML information to be updated. May be <code>null</code>.
   * @param sDisplayName
   *        The "shorthand" display name like "SML" or "SMK". May neither be <code>null</code> nor
   *        empty.
   * @param sDNSZone
   *        The DNS zone on which this SML is operating. May not be <code>null</code>. It must be
   *        ensured that the value consists only of lower case characters for comparability!
   *        Example: <code>acc.edelivery.tech.ec.europa.eu</code>
   * @param sManagementServiceURL
   *        The service URL where the management application is running on incl. the host name. May
   *        not be <code>null</code>. The difference to the host name is the eventually present
   *        context path.
   * @param sURLSuffixManageSMP
   *        The sub-path to be used for managing SMP data in the SML. May not be <code>null</code>,
   *        may be empty. If not empty it must start with a slash ("/").
   * @param sURLSuffixManageParticipant
   *        The sub-path to be used for managing participant data in the SML. May not be
   *        <code>null</code>, may be empty. If not empty it must start with a slash ("/").
   * @param bClientCertificateRequired
   *        <code>true</code> if this SML requires a client certificate for access,
   *        <code>false</code> otherwise.<br>
   *        Both Peppol production SML and SMK require a client certificate. Only a locally running
   *        SML software may not require a client certificate.
   * @return {@link EChange#CHANGED} if something was changed.
   */
  @NonNull
  EChange updateSMLInfo (@Nullable String sSMLInfoID,
                         @NonNull @Nonempty String sDisplayName,
                         @NonNull @Nonempty String sDNSZone,
                         @NonNull @Nonempty String sManagementServiceURL,
                         @NonNull String sURLSuffixManageSMP,
                         @NonNull String sURLSuffixManageParticipant,
                         boolean bClientCertificateRequired);

  /**
   * Delete an existing SML information.
   *
   * @param sSMLInfoID
   *        The ID of the SML information to be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if the removal was successful.
   */
  @Nullable
  EChange deleteSMLInfo (@Nullable String sSMLInfoID);

  /**
   * @return An unsorted collection of all contained SML information. Never <code>null</code> but
   *         maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISMLInfo> getAllSMLInfos ();

  /**
   * Get the SML information with the passed ID.
   *
   * @param sID
   *        The ID to be resolved. May be <code>null</code>.
   * @return <code>null</code> if no such SML information exists.
   */
  @Nullable
  ISMLInfo getSMLInfoOfID (@Nullable String sID);

  /**
   * Check if a SML information with the passed ID is contained.
   *
   * @param sID
   *        The ID of the SML information to be checked. May be <code>null</code>.
   * @return <code>true</code> if the ID is contained, <code>false</code> otherwise.
   */
  boolean containsSMLInfoWithID (@Nullable String sID);

  /**
   * Find the first SML information that contains the provided manage participant identifier
   * endpoint address.
   *
   * @param sAddress
   *        The address to search. May be <code>null</code>.
   * @return <code>null</code> if no such SML information exists.
   */
  @Nullable
  ISMLInfo findFirstWithManageParticipantIdentifierEndpointAddress (@Nullable String sAddress);
}
