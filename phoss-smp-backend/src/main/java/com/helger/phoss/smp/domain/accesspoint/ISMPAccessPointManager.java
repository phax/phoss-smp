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

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.collection.paging.IPagingSpec;
import com.helger.phoss.smp.security.SMPCertificateHelper;
import com.helger.photon.core.paging.TableColumnHelper;

/**
 * Manager for {@link ISMPAccessPoint} objects. Access Points are an optional feature: SMP endpoints
 * may reference an Access Point instead of containing the endpoint reference URL and the
 * certificate directly. That way this - potentially large - data is stored only once for all
 * endpoints sharing the same physical Access Point.
 * <p>
 * Access Points are created and deleted explicitly by the user only. They are identified by their
 * unique name, which is also the identifier used in the REST API.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public interface ISMPAccessPointManager
{
  /** Maximum length of the endpoint reference URL as stored in the DB backends */
  int ENDPOINT_REFERENCE_MAX_LENGTH = 256;

  /**
   * Create a new Access Point. The name must be unique - if an Access Point with the provided name
   * already exists, no new Access Point is created.
   *
   * @param sName
   *        The unique name of the Access Point. May neither be <code>null</code> nor empty.
   * @param sEndpointReference
   *        The endpoint reference URL. May be <code>null</code>.
   * @param sCertificate
   *        The certificate. May be <code>null</code>.
   * @return <code>null</code> if an Access Point with the provided name already exists.
   */
  @Nullable
  ISMPAccessPoint createAccessPoint (@NonNull @Nonempty String sName,
                                     @Nullable String sEndpointReference,
                                     @Nullable String sCertificate);

  /**
   * Update an existing Access Point. All changes are immediately effective for all endpoints
   * referencing this Access Point.
   *
   * @param sID
   *        The ID of the Access Point to be changed. May be <code>null</code>.
   * @param sName
   *        The new name. Must be unique. May neither be <code>null</code> nor empty.
   * @param sEndpointReference
   *        The new endpoint reference URL. May be <code>null</code>.
   * @param sCertificate
   *        The new certificate. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if something was changed.
   */
  @NonNull
  EChange updateAccessPoint (@Nullable String sID,
                             @NonNull @Nonempty String sName,
                             @Nullable String sEndpointReference,
                             @Nullable String sCertificate);

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
   * Get the Access Point with the provided ID.
   *
   * @param sID
   *        The ID to search. May be <code>null</code>.
   * @return <code>null</code> if no such Access Point exists.
   */
  @Nullable
  ISMPAccessPoint getAccessPointOfID (@Nullable String sID);

  /**
   * Get the Access Point with the provided name. The name is compared case insensitive.
   *
   * @param sName
   *        The name to search. May be <code>null</code>.
   * @return <code>null</code> if no such Access Point exists.
   */
  @Nullable
  ISMPAccessPoint getAccessPointOfName (@Nullable String sName);

  /**
   * Check if an Access Point with the provided name exists.
   *
   * @param sName
   *        The name to search. May be <code>null</code>.
   * @return <code>true</code> if such an Access Point exists.
   */
  default boolean containsAccessPointWithName (@Nullable final String sName)
  {
    return getAccessPointOfName (sName) != null;
  }

  /**
   * @return All contained Access Points. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISMPAccessPoint> getAllAccessPoints ();

  /**
   * Get a single "page" of all Access Points matching the provided search text. This method is
   * meant to be used for server side pagination in combination with
   * {@link #getAccessPointCount(String)}.<br>
   * The sort fields of the paging specification are resolved via {@link ESMPAccessPointColumn} -
   * unknown or non-sortable field names are ignored, because they are provided by a client. If no
   * sort field remains, the first column of {@link ESMPAccessPointColumn} is used, so that
   * consecutive page requests return disjunct results.
   *
   * @param aPagingSpec
   *        The paging specification to be applied. May not be <code>null</code>.
   * @param sSearchText
   *        The global search text to filter by. May be <code>null</code> or empty in which case no
   *        filtering takes place. It is matched against all searchable columns of
   *        {@link ESMPAccessPointColumn}, ignoring case.
   * @return A non-<code>null</code> but maybe empty list.
   */
  @NonNull
  @ReturnsMutableCopy
  default ICommonsList <ISMPAccessPoint> getAllAccessPoints (@NonNull final IPagingSpec aPagingSpec,
                                                             @Nullable final String sSearchText)
  {
    return TableColumnHelper.getPage (ESMPAccessPointColumn.values (), getAllAccessPoints (), aPagingSpec, sSearchText);
  }

  /**
   * @return The total number of contained Access Points. Always &ge; 0.
   */
  @Nonnegative
  long getAccessPointCount ();

  /**
   * Get the number of Access Points matching the provided search text.
   *
   * @param sSearchText
   *        The global search text to filter by. May be <code>null</code> or empty in which case all
   *        Access Points are counted.
   * @return The number of matching Access Points. May be &lt; 0 in case there was an error querying
   *         (e.g. because of a missing SQL backend).
   */
  @CheckForSigned
  default long getAccessPointCount (@Nullable final String sSearchText)
  {
    if (StringHelper.isEmpty (sSearchText))
      return getAccessPointCount ();

    return TableColumnHelper.getCount (ESMPAccessPointColumn.values (), getAllAccessPoints (), sSearchText);
  }

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
   * Delete the Access Point with the provided ID. The caller must ensure that the Access Point is
   * no longer referenced by any endpoint.
   *
   * @param sID
   *        The ID to be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if something was deleted.
   */
  @NonNull
  EChange deleteAccessPoint (@Nullable String sID);
}
