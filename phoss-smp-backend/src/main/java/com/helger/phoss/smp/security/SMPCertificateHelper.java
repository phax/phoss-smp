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
package com.helger.phoss.smp.security;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.security.certificate.CertificateHelper;

/**
 * SMP specific X.509 certificate helper class.
 * 
 * @author Philip Helger
 * @since 8.0.16
 */
@Immutable
public final class SMPCertificateHelper
{
  private SMPCertificateHelper ()
  {}

  /**
   * Normalize the provided certificate string.
   * 
   * @param s
   *        The source certificate.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static String getNormalizedCert (@Nullable final String s)
  {
    if (StringHelper.isEmpty (s))
      return "";

    // Trims, removes PEM header, removes spaces
    return CertificateHelper.getWithoutPEMHeader (s);
  }
}
