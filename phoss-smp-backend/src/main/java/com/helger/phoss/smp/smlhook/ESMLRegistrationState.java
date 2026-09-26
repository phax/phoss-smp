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
package com.helger.phoss.smp.smlhook;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.name.IHasDisplayName;

import org.jspecify.annotations.Nullable;

/**
 * Defines the outcome of reading this SMPs own registration from the SML, as defined in chapter
 * 3.1.3.2 of the SML specification.
 *
 * @author Philip Helger
 * @since 8.5.1
 */
public enum ESMLRegistrationState implements IHasID <String>, IHasDisplayName
{
  /** The SML holds a record for this SMP */
  REGISTERED ("registered", "Registered"),
  /** The SML has no record for this SMP */
  NOT_REGISTERED ("notregistered", "Not registered"),
  /** The SML could not be queried at all */
  CHECK_FAILED ("checkfailed", "Check failed");

  private final String m_sID;
  private final String m_sDisplayName;

  ESMLRegistrationState (@NonNull @Nonempty final String sID, @NonNull @Nonempty final String sDisplayName)
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

  /**
   * @return <code>true</code> if the SML holds a record for this SMP.
   */
  public boolean isRegistered ()
  {
    return this == REGISTERED;
  }

  /**
   * @return <code>true</code> if the SML could not be queried, so that neither
   *         {@link #REGISTERED} nor {@link #NOT_REGISTERED} could be determined.
   */
  public boolean isCheckFailed ()
  {
    return this == CHECK_FAILED;
  }

  @Nullable
  public static ESMLRegistrationState getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMLRegistrationState.class, sID);
  }
}
