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
package com.helger.phoss.smp.dnscheck;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.name.IHasDisplayName;

/**
 * The outcome of the DNS check of a single participant.
 *
 * @author Philip Helger
 * @since 8.6.0
 */
public enum EDNSCheckState implements IHasID <String>, IHasDisplayName
{
  /** The participant resolves to an SMP URI */
  RESOLVED ("resolved", "Resolved"),
  /** The participant has no DNS entry, so it is not registered in the SML */
  NOT_REGISTERED ("notregistered", "Not registered in the SML"),
  /** The DNS name could not even be built, or the lookup failed unexpectedly */
  LOOKUP_FAILED ("lookupfailed", "Lookup failed");

  private final String m_sID;
  private final String m_sDisplayName;

  EDNSCheckState (@NonNull @Nonempty final String sID, @NonNull @Nonempty final String sDisplayName)
  {
    m_sID = sID;
    m_sDisplayName = sDisplayName;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  @Nonempty
  public String getDisplayName ()
  {
    return m_sDisplayName;
  }

  public boolean isResolved ()
  {
    return this == RESOLVED;
  }

  @Nullable
  public static EDNSCheckState getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EDNSCheckState.class, sID);
  }
}
