/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Defines the identifier types to be used - simple (allows all), Peppol
 * (special schemes) or BDXR (different implementation type).
 *
 * @author Philip Helger
 */
public enum ESMPIdentifierType implements IHasID <String>
{
  SIMPLE ("simple"),
  PEPPOL ("peppol"),
  BDXR1 ("bdxr1"),
  BDXR2 ("bdxr2");

  private final String m_sID;

  private ESMPIdentifierType (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static ESMPIdentifierType getFromIDOrNull (@Nullable final String sID)
  {
    return getFromIDOrDefault (sID, null);
  }

  @Nullable
  public static ESMPIdentifierType getFromIDOrDefault (@Nullable final String sID,
                                                       @Nullable final ESMPIdentifierType eDefault)
  {
    // Legacy ID
    if ("bdxr".equals (sID))
      return BDXR1;

    return EnumHelper.getFromIDOrDefault (ESMPIdentifierType.class, sID, eDefault);
  }
}
