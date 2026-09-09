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
package com.helger.phoss.smp.domain.totp;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * Helper to convert the list of recovery code hashes from and to the single string value, that is
 * used by backends that cannot store lists natively (like SQL).
 *
 * @author Philip Helger
 * @since 8.4.3
 */
@Immutable
public final class SMPUserTotpRecoveryCodeHelper
{
  /** The separator between two recovery code hashes. */
  public static final char SEPARATOR = '\n';

  private SMPUserTotpRecoveryCodeHelper ()
  {}

  /**
   * Convert a list of recovery code hashes to the single string value to be stored.
   *
   * @param aRecoveryCodeHashes
   *        The hashes to be converted. May be <code>null</code>.
   * @return <code>null</code> if the provided list is <code>null</code> or empty.
   */
  @Nullable
  public static String getAsStorageValue (@Nullable final ICommonsList <String> aRecoveryCodeHashes)
  {
    if (aRecoveryCodeHashes == null || aRecoveryCodeHashes.isEmpty ())
      return null;
    return StringImplode.getImploded (SEPARATOR, aRecoveryCodeHashes);
  }

  /**
   * Convert a stored string value back to the list of recovery code hashes.
   *
   * @param sStorageValue
   *        The stored value. May be <code>null</code>.
   * @return Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <String> getAsList (@Nullable final String sStorageValue)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    if (StringHelper.isNotEmpty (sStorageValue))
      StringHelper.explode (SEPARATOR, sStorageValue, x -> {
        final String sTrimmed = x.trim ();
        if (StringHelper.isNotEmpty (sTrimmed))
          ret.add (sTrimmed);
      });
    return ret;
  }
}
