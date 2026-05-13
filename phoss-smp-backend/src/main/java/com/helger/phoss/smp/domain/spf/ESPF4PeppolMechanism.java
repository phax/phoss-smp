/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.spf;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * SPF4Peppol mechanism enum defining how to identify an Access Point.
 *
 * @author Steven Noels
 */
public enum ESPF4PeppolMechanism implements IHasID <String>
{
  /** Match by Access Point Seat ID */
  SEATID ("seatid"),
  /** Match by certificate fingerprint (SHA-256) */
  CERTFP ("certfp"),
  /** Match if AP is listed in sender's SMP ServiceMetadata */
  SMP ("smp"),
  /** Reference another participant's policy (delegation) */
  REFERENCE ("reference"),
  /** Match all - typically used as default behavior */
  ALL ("all");

  private final String m_sID;

  ESPF4PeppolMechanism (@NonNull final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return <code>true</code> if this mechanism requires a value (seat ID, fingerprint, or
   *         reference), <code>false</code> for smp and all.
   */
  public boolean requiresValue ()
  {
    return this == SEATID || this == CERTFP || this == REFERENCE;
  }

  @Nullable
  public static ESPF4PeppolMechanism getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESPF4PeppolMechanism.class, sID);
  }

  @Nullable
  public static ESPF4PeppolMechanism getFromIDOrDefault (@Nullable final String sID,
                                                         @Nullable final ESPF4PeppolMechanism eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ESPF4PeppolMechanism.class, sID, eDefault);
  }
}
