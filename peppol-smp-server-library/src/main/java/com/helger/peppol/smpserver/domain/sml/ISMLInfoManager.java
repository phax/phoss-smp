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
package com.helger.peppol.smpserver.domain.sml;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
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
   *        The "shorthand" display name like "SML" or "SMK". May neither be
   *        <code>null</code> nor empty.
   * @param sDNSZone
   *        The DNS zone on which this SML is operating. May not be
   *        <code>null</code>. It must be ensured that the value consists only
   *        of lower case characters for comparability! Example:
   *        <code>sml.peppolcentral.org</code>
   * @param sManagementServiceURL
   *        The service URL where the management application is running on incl.
   *        the host name. May not be <code>null</code>. The difference to the
   *        host name is the eventually present context path.
   * @param bClientCertificateRequired
   *        <code>true</code> if this SML requires a client certificate for
   *        access, <code>false</code> otherwise.<br>
   *        Both PEPPOL production SML and SMK require a client certificate.
   *        Only a locally running SML software may not require a client
   *        certificate.
   * @return Never <code>null</code>.
   */
  @Nonnull
  ISMLInfo createSMLInfo (@Nonnull @Nonempty String sDisplayName,
                          @Nonnull @Nonempty String sDNSZone,
                          @Nonnull @Nonempty String sManagementServiceURL,
                          boolean bClientCertificateRequired);

  /**
   * Update an existing SML information.
   *
   * @param sSMLInfoID
   *        The ID of the SML information to be updated. May be
   *        <code>null</code>.
   * @param sDisplayName
   *        The "shorthand" display name like "SML" or "SMK". May neither be
   *        <code>null</code> nor empty.
   * @param sDNSZone
   *        The DNS zone on which this SML is operating. May not be
   *        <code>null</code>. It must be ensured that the value consists only
   *        of lower case characters for comparability! Example:
   *        <code>sml.peppolcentral.org</code>
   * @param sManagementServiceURL
   *        The service URL where the management application is running on incl.
   *        the host name. May not be <code>null</code>. The difference to the
   *        host name is the eventually present context path.
   * @param bClientCertificateRequired
   *        <code>true</code> if this SML requires a client certificate for
   *        access, <code>false</code> otherwise.<br>
   *        Both PEPPOL production SML and SMK require a client certificate.
   *        Only a locally running SML software may not require a client
   *        certificate.
   * @return {@link EChange#CHANGED} if something was changed.
   */
  @Nonnull
  EChange updateSMLInfo (@Nullable String sSMLInfoID,
                         @Nonnull @Nonempty String sDisplayName,
                         @Nonnull @Nonempty String sDNSZone,
                         @Nonnull @Nonempty String sManagementServiceURL,
                         boolean bClientCertificateRequired);

  /**
   * Delete an existing SML information.
   *
   * @param sSMLInfoID
   *        The ID of the SML information to be deleted. May be
   *        <code>null</code>.
   * @return {@link EChange#CHANGED} if the removal was successful.
   */
  @Nullable
  EChange removeSMLInfo (@Nullable String sSMLInfoID);

  /**
   * @return An unsorted collection of all contained SML information. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
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
   * Find the first SML information that matches the provided predicate.
   *
   * @param aFilter
   *        The predicate to be applied for searching. May not be
   *        <code>null</code>.
   * @return <code>null</code> if no such SML information exists.
   */
  @Nullable
  ISMLInfo findFirst (@Nullable Predicate <? super ISMLInfo> aFilter);

  /**
   * Check if a SML information with the passed ID is contained.
   *
   * @param sID
   *        The ID of the SML information to be checked. May be
   *        <code>null</code>.
   * @return <code>true</code> if the ID is contained, <code>false</code>
   *         otherwise.
   */
  boolean containsSMLInfoWithID (@Nullable String sID);
}
