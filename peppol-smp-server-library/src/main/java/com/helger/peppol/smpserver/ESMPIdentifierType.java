/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Defines the identifier types to be used - simple (allows all), PEPPOL
 * (special schemes) or BDXR (different implementation type).
 *
 * @author Philip Helger
 */
public enum ESMPIdentifierType implements IHasID <String>
{
  SIMPLE ("simple"),
  PEPPOL ("peppol"),
  BDXR ("bdxr");

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
    return EnumHelper.getFromIDOrNull (ESMPIdentifierType.class, sID);
  }

  @Nullable
  public static ESMPIdentifierType getFromIDOrDefault (@Nullable final String sID,
                                                       @Nullable final ESMPIdentifierType eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ESMPIdentifierType.class, sID, eDefault);
  }
}
