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
package com.helger.phoss.smp.domain.sgprops;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

public enum ESGCustomPropertyType implements IHasID <String>
{
  PRIVATE ("priv"),
  PUBLIC ("pub");

  // for DB storage
  public static final int ID_MAX_LEN = 6;

  @NonNull
  @Nonempty
  private final String m_sID;

  private ESGCustomPropertyType (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public boolean isPrivate ()
  {
    return this == PRIVATE;
  }

  public boolean isPublic ()
  {
    return this == PUBLIC;
  }

  @Nullable
  public static ESGCustomPropertyType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESGCustomPropertyType.class, sID);
  }
}
