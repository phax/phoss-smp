/*
 * Copyright (C) 2015-2023 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.restapi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.ToStringGenerator;
import com.helger.http.basicauth.BasicAuthClientCredentials;

/**
 * This class contains the different representations of the SMP API credentials.
 * That may either be a pair of username and password or a User Token.
 *
 * @author Philip Helger
 * @since 6.1.0
 */
@Immutable
public class SMPAPICredentials
{
  private final BasicAuthClientCredentials m_aBasicAuth;
  private final String m_sBearerToken;

  protected SMPAPICredentials (@Nullable final BasicAuthClientCredentials aBasicAuth,
                               @Nullable final String sBearerToken)
  {
    ValueEnforcer.isFalse ( () -> aBasicAuth == null && sBearerToken == null,
                            "One of the credentials must be provided");
    ValueEnforcer.isFalse ( () -> aBasicAuth != null && sBearerToken != null,
                            "Not more then one credential must be provided");
    m_aBasicAuth = aBasicAuth;
    m_sBearerToken = sBearerToken;
  }

  /**
   * @return <code>true</code> if this instance uses BasicAuth credentials,
   *         <code>false</code> if not.
   * @see #hasBearerToken()
   */
  public final boolean hasBasicAuth ()
  {
    return m_aBasicAuth != null;
  }

  /**
   * @return The BasicAuth credentials, if any. May be <code>null</code>.
   * @see #hasBasicAuth()
   */
  @Nullable
  public final BasicAuthClientCredentials getBasicAuth ()
  {
    return m_aBasicAuth;
  }

  /**
   * @return <code>true</code> if this instance uses a Bearer token,
   *         <code>false</code> if not.
   * @see #hasBasicAuth()
   */
  public final boolean hasBearerToken ()
  {
    return m_sBearerToken != null;
  }

  /**
   * @return The Bearer token, if any. May be <code>null</code>.
   * @see #hasBearerToken()
   */
  @Nullable
  public final String getBearerToken ()
  {
    return m_sBearerToken;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("BasicAuth", m_aBasicAuth)
                                       .append ("BearerToken", m_sBearerToken)
                                       .getToString ();
  }

  @Nonnull
  public static SMPAPICredentials createForBasicAuth (@Nonnull final BasicAuthClientCredentials aBasicAuth)
  {
    ValueEnforcer.notNull (aBasicAuth, "BasicAuth");
    return new SMPAPICredentials (aBasicAuth, null);
  }

  @Nonnull
  public static SMPAPICredentials createForBearerToken (@Nonnull @Nonempty final String sBearerToken)
  {
    ValueEnforcer.notEmpty (sBearerToken, "BearerToken");
    return new SMPAPICredentials (null, sBearerToken);
  }
}
