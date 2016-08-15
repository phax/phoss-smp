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
package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.type.ITypedObject;

/**
 * Runtime settings for this SMP server instance.
 *
 * @author Philip Helger
 */
public interface ISMPSettings extends ITypedObject <String>
{
  /**
   * Check if the writable parts of the REST API are disabled. If this is the
   * case, only the read-only part of the API can be used. The writable REST API
   * will return an HTTP 404 error.
   *
   * @return <code>true</code> if it is disabled, <code>false</code> if it is
   *         enabled. By the default the writable API is enabled.
   */
  boolean isRESTWritableAPIDisabled ();

  /**
   * Check if the PEPPOL Directory integration (offering the /businesscard API)
   * is enabled.
   *
   * @return <code>true</code> if it is enabled, <code>false</code> otherwise.
   *         By default it is disabled.
   */
  boolean isPEPPOLDirectoryIntegrationEnabled ();

  /**
   * If the PEPPOL Directory integration is enabled, should the changes be
   * pushed automatically?
   *
   * @return <code>true</code> if it is enabled, <code>false</code> otherwise.
   *         By default it is disabled.
   */
  boolean isPEPPOLDirectoryIntegrationAutoUpdate ();

  /**
   * @return The host name of the PEPPOL Directory server. Never
   *         <code>null</code>.
   */
  @Nonnull
  String getPEPPOLDirectoryHostName ();

  /**
   * @return <code>true</code> if the SML connection is active,
   *         <code>false</code> if not.
   */
  boolean isWriteToSML ();

  /**
   * @return The SML URL to use (the manage participant endpoint - e.g.
   *         <code>https://edelivery.tech.ec.europa.eu/edelivery-sml/manageparticipantidentifier</code>).
   *         Only relevant when {@link #isWriteToSML()} is <code>true</code>.
   */
  @Nullable
  String getSMLURL ();
}
