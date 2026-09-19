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

import com.helger.annotation.Nonempty;
import com.helger.base.compare.CompareHelper;
import com.helger.base.compare.IComparator;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.id.IHasID;
import com.helger.base.name.IHasName;
import com.helger.base.string.StringHelper;

/**
 * Represents a single named Access Point - meaning the endpoint reference URL and the public
 * certificate of that Access Point.
 * <p>
 * Access Points are an <b>optional</b> feature: an {@link com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint}
 * either contains the endpoint reference URL and the certificate directly (the classic way) or it
 * references an Access Point - but never both. Referencing an Access Point is beneficial if many
 * endpoints share the same physical Access Point, because in that case the - potentially large -
 * data is stored only once and a change of e.g. the certificate is a single write that is
 * immediately effective for all endpoints referencing the Access Point.
 * <p>
 * An Access Point is <b>identified by its unique name</b>. The name is also the identifier that is
 * used to reference an Access Point in the REST API.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public interface ISMPAccessPoint extends IHasID <String>, IHasName
{
  /**
   * @return The unique internal ID of this Access Point. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getID ();

  /**
   * @return The unique name of this Access Point. This is the identifier that is used in the REST
   *         API. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getName ();

  /**
   * @return The address of the Access Point, as a WS-Addressing Endpoint Reference (EPR). This is
   *         just a URL. May be <code>null</code>.
   */
  @Nullable
  String getEndpointReference ();

  /**
   * @return <code>true</code> if this Access Point has an endpoint reference URL, <code>false</code>
   *         otherwise.
   * @see #getEndpointReference()
   */
  default boolean hasEndpointReference ()
  {
    return StringHelper.isNotEmpty (getEndpointReference ());
  }

  /**
   * @return the complete signing certificate of the Access Point, as a PEM base 64 encoded X509 DER
   *         formatted value. May be <code>null</code>.
   */
  @Nullable
  String getCertificate ();

  /**
   * @return <code>true</code> if this Access Point has a certificate, <code>false</code> otherwise.
   * @see #getCertificate()
   */
  default boolean hasCertificate ()
  {
    return StringHelper.isNotEmpty (getCertificate ());
  }

  /**
   * Check if this Access Point has exactly the provided name. Names are compared case insensitive,
   * because the name is the unique business key of an Access Point.
   *
   * @param sName
   *        The name to compare to. May be <code>null</code>.
   * @return <code>true</code> if the name matches.
   */
  default boolean hasSameName (@Nullable final String sName)
  {
    return SMPAccessPointHelper.createNameLookupKey (getName ())
                               .equals (SMPAccessPointHelper.createNameLookupKey (sName));
  }

  /**
   * Check if this Access Point uses exactly the provided endpoint reference URL.
   *
   * @param sEndpointReference
   *        The endpoint reference to compare to. May be <code>null</code>.
   * @return <code>true</code> if the endpoint reference matches.
   */
  default boolean hasSameEndpointReference (@Nullable final String sEndpointReference)
  {
    return EqualsHelper.equals (StringHelper.getNotNull (getEndpointReference (), ""),
                                StringHelper.getNotNull (sEndpointReference, ""));
  }

  /**
   * Check if this Access Point uses exactly the provided certificate.
   *
   * @param sCertificate
   *        The certificate to compare to. May be <code>null</code>.
   * @return <code>true</code> if the certificate matches.
   */
  default boolean hasSameCertificate (@Nullable final String sCertificate)
  {
    return EqualsHelper.equals (getCertificate (), sCertificate);
  }

  @NonNull
  static IComparator <ISMPAccessPoint> comparator ()
  {
    return (aElement1, aElement2) -> CompareHelper.compare (aElement1.getName (), aElement2.getName (), true);
  }
}
