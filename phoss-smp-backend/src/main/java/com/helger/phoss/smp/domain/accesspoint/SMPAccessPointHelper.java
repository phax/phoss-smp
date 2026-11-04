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

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

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
  /** Maximum length of the Access Point name as stored in the DB backends */
  public static final int NAME_MAX_LENGTH = 64;

  /**
   * The prefix that is used in the REST API to reference an Access Point by name instead of
   * providing the endpoint reference URL and the certificate directly.
   */
  public static final String REST_ACCESS_POINT_PREFIX = "accesspoint:";

  private static final Pattern VALID_NAME = Pattern.compile ("[a-zA-Z0-9][a-zA-Z0-9._\\-]*");

  private SMPAccessPointHelper ()
  {}

  /**
   * @return A new unique ID for an Access Point. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public static String createUniqueAccessPointID ()
  {
    return UUID.randomUUID ().toString ();
  }

  /**
   * Create the key that is used to check the uniqueness of Access Point names. Names are treated
   * case insensitive, so that Access Points cannot be confused with each other.
   *
   * @param sName
   *        The Access Point name. May be <code>null</code>.
   * @return The non-<code>null</code> lookup key.
   */
  @NonNull
  public static String createNameLookupKey (@Nullable final String sName)
  {
    return StringHelper.getNotNull (sName, "").trim ().toLowerCase (Locale.ROOT);
  }

  /**
   * Create the name lookup key of an existing Access Point.
   *
   * @param aAccessPoint
   *        The Access Point to create the key for. May not be <code>null</code>.
   * @return The non-<code>null</code> lookup key.
   */
  @NonNull
  public static String createNameLookupKey (@NonNull final ISMPAccessPoint aAccessPoint)
  {
    return createNameLookupKey (aAccessPoint.getName ());
  }

  /**
   * Check if the provided Access Point name is syntactically valid. A valid name starts with a
   * letter or a digit and may additionally contain dots, underscores and hyphens. The length is
   * limited to {@link #NAME_MAX_LENGTH} characters, because the name is stored in an indexed
   * database column.
   *
   * @param sName
   *        The name to check. May be <code>null</code>.
   * @return <code>true</code> if the name is valid.
   */
  public static boolean isValidName (@Nullable final String sName)
  {
    if (StringHelper.isEmpty (sName) || sName.length () > NAME_MAX_LENGTH)
      return false;
    return VALID_NAME.matcher (sName).matches ();
  }

  /**
   * Create the value that is used in the REST API to reference the provided Access Point by name.
   *
   * @param sName
   *        The Access Point name. May neither be <code>null</code> nor empty.
   * @return The reference value. Neither <code>null</code> nor empty.
   * @see #getAccessPointNameFromRESTReference(String)
   */
  @NonNull
  @Nonempty
  public static String createRESTReference (@NonNull @Nonempty final String sName)
  {
    return REST_ACCESS_POINT_PREFIX + sName;
  }

  /**
   * Extract the Access Point name from a REST API endpoint reference value.
   *
   * @param sEndpointReference
   *        The endpoint reference value as provided via the REST API. May be <code>null</code>.
   * @return <code>null</code> if the provided value does not reference an Access Point.
   * @see #createRESTReference(String)
   */
  @Nullable
  public static String getAccessPointNameFromRESTReference (@Nullable final String sEndpointReference)
  {
    if (sEndpointReference == null)
      return null;

    final String sTrimmed = sEndpointReference.trim ();
    if (!StringHelper.startsWithIgnoreCase (sTrimmed, REST_ACCESS_POINT_PREFIX))
      return null;

    final String sName = sTrimmed.substring (REST_ACCESS_POINT_PREFIX.length ()).trim ();
    return StringHelper.isEmpty (sName) ? null : sName;
  }
}
