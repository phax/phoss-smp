/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.domain.sml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
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
                         @Nonnull @Nonempty final String sDisplayName,
                         @Nonnull @Nonempty final String sDNSZone,
                         @Nonnull @Nonempty final String sManagementServiceURL,
                         final boolean bClientCertificateRequired);

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
