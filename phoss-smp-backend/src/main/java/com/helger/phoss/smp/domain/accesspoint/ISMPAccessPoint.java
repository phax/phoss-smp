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
import com.helger.base.string.StringHelper;

/**
 * Represents a single physical Access Point - meaning the endpoint reference URL and the public
 * certificate of that Access Point.
 * <p>
 * Because multiple SMP endpoints (of different participants, document types and processes)
 * regularly point to the very same physical Access Point, this data is stored only once and is
 * referenced from all the {@link com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint} objects using
 * it.
 * <p>
 * An Access Point is <b>identified by its endpoint reference URL</b>. A physical Access Point can
 * technically only have one single public certificate, so the certificate is a mutable attribute of
 * the Access Point and not part of its identity. As a consequence, changing the certificate of an
 * Access Point is a single write that is immediately effective for all endpoints referencing it.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public interface ISMPAccessPoint extends IHasID <String>
{
  /**
   * @return The unique ID of this Access Point. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getID ();

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
   * @return <code>true</code> if neither an endpoint reference nor a certificate is present.
   */
  default boolean hasNoContent ()
  {
    return !hasEndpointReference () && !hasCertificate ();
  }

  /**
   * Check if this Access Point uses exactly the provided endpoint reference URL. This is the
   * identity check of an Access Point.
   *
   * @param sEndpointReference
   *        The endpoint reference to compare to. May be <code>null</code>.
   * @return <code>true</code> if the endpoint reference matches.
   */
  default boolean hasSameEndpointReference (@Nullable final String sEndpointReference)
  {
    return SMPAccessPointHelper.createLookupKey (getEndpointReference ())
                               .equals (SMPAccessPointHelper.createLookupKey (sEndpointReference));
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
    return (aElement1, aElement2) -> CompareHelper.compare (aElement1.getEndpointReference (),
                                                            aElement2.getEndpointReference (),
                                                            true);
  }
}
