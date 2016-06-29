/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
