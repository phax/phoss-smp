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

import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;

/**
 * Helper class for dealing with {@link ISMPAccessPoint} objects.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
@Immutable
public final class SMPAccessPointHelper
{
  /** Separator that cannot occur in a URL nor in a PEM encoded certificate */
  private static final char LOOKUP_KEY_SEPARATOR = '\u0000';

  private SMPAccessPointHelper ()
  {}

  /**
   * @return A new unique ID for an Access Point. Neither <code>null</code> nor empty.
   *         <p>
   *         Note: a random UUID is used instead of the {@code GlobalIDFactory}, because Access
   *         Points are implicitly created whenever an {@code SMPEndpoint} is instantiated - and
   *         that may happen outside of an initialized scope.
   */
  @NonNull
  @Nonempty
  public static String createUniqueAccessPointID ()
  {
    return UUID.randomUUID ().toString ();
  }

  /**
   * Create the key that is used to identify identical Access Points. Two Access Points are
   * considered identical if they have the same endpoint reference URL and the same certificate.
   * <code>null</code> and empty values are treated identically, because that is how the different
   * backends store "no value".
   *
   * @param sEndpointReference
   *        The endpoint reference URL. May be <code>null</code>.
   * @param sCertificate
   *        The certificate. May be <code>null</code>.
   * @return The non-<code>null</code> lookup key.
   */
  @NonNull
  public static String createLookupKey (@Nullable final String sEndpointReference, @Nullable final String sCertificate)
  {
    return StringHelper.getNotNull (sEndpointReference, "") +
           LOOKUP_KEY_SEPARATOR +
           StringHelper.getNotNull (sCertificate, "");
  }

  /**
   * Create the lookup key of an existing Access Point.
   *
   * @param aAccessPoint
   *        The Access Point to create the key for. May not be <code>null</code>.
   * @return The non-<code>null</code> lookup key.
   */
  @NonNull
  public static String createLookupKey (@NonNull final ISMPAccessPoint aAccessPoint)
  {
    return createLookupKey (aAccessPoint.getEndpointReference (), aAccessPoint.getCertificate ());
  }
}
