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
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.phoss.smp.security.SMPCertificateHelper;

/**
 * Manager for {@link ISMPAccessPoint} objects. Access Points are shared between all SMP endpoints
 * that use the very same endpoint reference URL, so that this - potentially large - data is only
 * stored once.
 * <p>
 * An Access Point is identified by its endpoint reference URL only, because a physical Access Point
 * can technically only have one single public certificate. Changing the certificate of an Access
 * Point is therefore a single write that is immediately effective for all endpoints referencing it.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public interface ISMPAccessPointManager
{
  /** Maximum length of the endpoint reference URL as stored in the DB backends */
  int ENDPOINT_REFERENCE_MAX_LENGTH = 256;

  /**
   * Find the Access Point with the exact provided endpoint reference URL.
   *
   * @param sEndpointReference
   *        The endpoint reference URL to search. May be <code>null</code>.
   * @return <code>null</code> if no such Access Point exists.
   */
  @Nullable
  ISMPAccessPoint findAccessPoint (@Nullable String sEndpointReference);

  /**
   * Get the existing Access Point with the provided endpoint reference URL or create a new one, if
   * no such Access Point exists yet. This is the main entry point for the backends when saving
   * service information.
   * <p>
   * If an Access Point with the provided URL already exists but has a different certificate, the
   * certificate of that Access Point is updated to the provided one. This is intentional: a
   * physical Access Point can only have one certificate, so the change is effective for all
   * endpoints referencing this Access Point.
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
   * Change the certificate of a single Access Point. The change is immediately effective for all
   * endpoints referencing this Access Point.
   *
   * @param sID
   *        The ID of the Access Point to be changed. May be <code>null</code>.
   * @param sNewCertificate
   *        The new certificate to be set. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if something was changed.
   */
  @NonNull
  EChange updateAccessPointCertificate (@Nullable String sID, @Nullable String sNewCertificate);

  /**
   * Change the endpoint reference URL of a single Access Point. The change is immediately effective
   * for all endpoints referencing this Access Point. The caller must ensure that no other Access
   * Point with the new URL exists.
   *
   * @param sID
   *        The ID of the Access Point to be changed. May be <code>null</code>.
   * @param sNewEndpointReference
   *        The new endpoint reference URL to be set. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if something was changed.
   */
  @NonNull
  EChange updateAccessPointEndpointReference (@Nullable String sID, @Nullable String sNewEndpointReference);

  /**
   * Find the IDs of all Access Points that use the provided certificate. The comparison is
   * performed on the normalized form of the certificate, as created by
   * {@link SMPCertificateHelper#getNormalizedCert(String)}.
   *
   * @param sUnifiedCertificate
   *        The normalized certificate to search for. May not be <code>null</code>.
   * @return A non-<code>null</code> mutable set of Access Point IDs.
   */
  @NonNull
  @ReturnsMutableCopy
  default ICommonsSet <String> getAllAccessPointIDsWithCertificate (@NonNull final String sUnifiedCertificate)
  {
    ValueEnforcer.notNull (sUnifiedCertificate, "UnifiedCertificate");

    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    for (final ISMPAccessPoint aAP : getAllAccessPoints ())
      if (aAP.hasCertificate () &&
          sUnifiedCertificate.equals (SMPCertificateHelper.getNormalizedCert (aAP.getCertificate ())))
        ret.add (aAP.getID ());
    return ret;
  }

  /**
   * Bulk-change the certificate of all Access Points that currently use the provided certificate.
   * Because each endpoint only references an Access Point, no endpoint needs to be touched at all -
   * this is what makes a certificate rollover cheap, no matter how many endpoints are affected.
   *
   * @param sUnifiedOldCertificate
   *        The normalized old certificate to search for. May not be <code>null</code>.
   * @param sNewCertificate
   *        The new certificate to be set (stored as-is). May not be <code>null</code>.
   * @return The number of changed Access Points. Always &ge; 0.
   */
  @Nonnegative
  default long updateAllAccessPointCertificates (@NonNull final String sUnifiedOldCertificate,
                                                 @NonNull final String sNewCertificate)
  {
    ValueEnforcer.notNull (sUnifiedOldCertificate, "UnifiedOldCertificate");
    ValueEnforcer.notNull (sNewCertificate, "NewCertificate");

    long nChanged = 0;
    for (final String sID : getAllAccessPointIDsWithCertificate (sUnifiedOldCertificate))
      if (updateAccessPointCertificate (sID, sNewCertificate).isChanged ())
        nChanged++;
    return nChanged;
  }

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
