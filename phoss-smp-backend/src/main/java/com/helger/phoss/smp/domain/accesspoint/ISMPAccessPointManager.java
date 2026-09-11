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
package com.helger.phoss.smp.domain.accesspoint;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;

/**
 * Manager for {@link ISMPAccessPoint} objects. Access Points are shared between all SMP endpoints
 * that use the very same endpoint reference URL and certificate, so that this - potentially large -
 * data is only stored once.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public interface ISMPAccessPointManager
{
  /** Maximum length of the endpoint reference URL as stored in the DB backends */
  int ENDPOINT_REFERENCE_MAX_LENGTH = 256;

  /**
   * Find the Access Point with the exact provided endpoint reference and certificate.
   *
   * @param sEndpointReference
   *        The endpoint reference URL to search. May be <code>null</code>.
   * @param sCertificate
   *        The certificate to search. May be <code>null</code>.
   * @return <code>null</code> if no such Access Point exists.
   */
  @Nullable
  ISMPAccessPoint findAccessPoint (@Nullable String sEndpointReference, @Nullable String sCertificate);

  /**
   * Get the existing Access Point with the provided endpoint reference and certificate or create a
   * new one, if no such Access Point exists yet. This is the main entry point for the backends when
   * saving service information.
   *
   * @param sEndpointReference
   *        The endpoint reference URL. May be <code>null</code>.
   * @param sCertificate
   *        The certificate. May be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  ISMPAccessPoint getOrCreateAccessPoint (@Nullable String sEndpointReference, @Nullable String sCertificate);

  /**
   * Get the Access Point with the provided ID.
   *
   * @param sID
   *        The ID to search. May be <code>null</code>.
   * @return <code>null</code> if no such Access Point exists.
   */
  @Nullable
  ISMPAccessPoint getAccessPointOfID (@Nullable String sID);

  /**
   * @return All contained Access Points. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISMPAccessPoint> getAllAccessPoints ();

  /**
   * @return The total number of contained Access Points. Always &ge; 0.
   */
  @Nonnegative
  long getAccessPointCount ();

  /**
   * Delete the Access Point with the provided ID.
   *
   * @param sID
   *        The ID to be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if something was deleted.
   */
  @NonNull
  EChange deleteAccessPoint (@Nullable String sID);

  /**
   * Delete all Access Points whose IDs are not contained in the provided set of used IDs. This is
   * the garbage collection for Access Points that are no longer referenced by any endpoint.
   *
   * @param aUsedIDs
   *        The set of all Access Point IDs that are still in use. May not be <code>null</code>.
   * @return The number of deleted Access Points. Always &ge; 0.
   */
  @Nonnegative
  long deleteAllUnusedAccessPoints (@NonNull ICommonsSet <String> aUsedIDs);
}
