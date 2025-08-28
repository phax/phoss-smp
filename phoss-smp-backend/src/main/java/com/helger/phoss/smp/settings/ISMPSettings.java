/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.settings;

import com.helger.base.type.ITypedObject;
import com.helger.peppol.sml.ISMLInfo;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
   * @return <code>true</code> if the Directory is required and warnings should
   *         be emitted if it is disabled, <code>false</code> if not. Default is
   *         <code>true</code>.
   * @see #isDirectoryIntegrationEnabled()
   * @since 5.1.0
   */
  boolean isDirectoryIntegrationRequired ();

  /**
   * Check if the Directory integration (offering the /businesscard API) is
   * enabled.
   *
   * @return <code>true</code> if it is enabled, <code>false</code> otherwise.
   *         By default it is disabled.
   */
  boolean isDirectoryIntegrationEnabled ();

  /**
   * If the Peppol Directory integration is enabled, should the changes be
   * pushed automatically?
   *
   * @return <code>true</code> if it is enabled, <code>false</code> otherwise.
   *         By default it is disabled.
   */
  boolean isDirectoryIntegrationAutoUpdate ();

  /**
   * @return The host name of the Peppol Directory server. Never
   *         <code>null</code>.
   */
  @Nonnull
  String getDirectoryHostName ();

  /**
   * @return <code>true</code> if the SML is required and warnings should be
   *         emitted if it is disabled, <code>false</code> if not. Default is
   *         <code>true</code>.
   * @see #isSMLEnabled()
   * @since 5.0.4
   */
  boolean isSMLRequired ();

  /**
   * @return <code>true</code> if the SML connection is active,
   *         <code>false</code> if not.
   * @since 5.2.0 - renamed
   */
  boolean isSMLEnabled ();

  /**
   * @return The SML information object to be used. May be <code>null</code>.
   *         Only relevant when {@link #isSMLEnabled()} is <code>true</code>.
   * @since 5.0.7
   */
  @Nullable
  ISMLInfo getSMLInfo ();

  /**
   * @return The ID of SML information object to be used. May be
   *         <code>null</code>. Only relevant when {@link #isSMLEnabled()} is
   *         <code>true</code>.
   * @since 5.0.7
   */
  @Nullable
  String getSMLInfoID ();

  /**
   * @return The DNS zone in which the SML operates. May be <code>null</code> if
   *         the SML URL is <code>null</code>.
   * @since 5.0.7
   */
  @Nullable
  default String getSMLDNSZone ()
  {
    final ISMLInfo aSMLInfo = getSMLInfo ();
    return aSMLInfo == null ? null : aSMLInfo.getDNSZone ();
  }
}
