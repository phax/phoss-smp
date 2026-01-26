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
 * SPF4Peppol qualifier enum representing the result when a mechanism matches.
 *
 * @author Steven Noels
 */
public enum ESPF4PeppolQualifier implements IHasID <String>
{
  /** Sender is authorized */
  PASS ("pass"),
  /** Sender is NOT authorized */
  FAIL ("fail"),
  /** Sender is probably not authorized (transitional/monitoring mode) */
  SOFTFAIL ("softfail"),
  /** Policy makes no assertion about authorization */
  NEUTRAL ("neutral");

  private final String m_sID;

  ESPF4PeppolQualifier (@NonNull final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static ESPF4PeppolQualifier getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESPF4PeppolQualifier.class, sID);
  }

  @Nullable
  public static ESPF4PeppolQualifier getFromIDOrDefault (@Nullable final String sID,
                                                         @Nullable final ESPF4PeppolQualifier eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ESPF4PeppolQualifier.class, sID, eDefault);
  }
}
