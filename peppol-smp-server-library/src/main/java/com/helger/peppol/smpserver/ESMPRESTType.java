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
import com.helger.commons.name.IHasDisplayName;

/**
 * Defines the type of REST responses to be returned. Either PEPPOL (using
 * objects in namespace http://busdox.org/serviceMetadata/publishing/1.0/) or
 * BDXR (using objects in namespace
 * http://docs.oasis-open.org/bdxr/ns/SMP/2016/05)
 *
 * @author Philip Helger
 */
public enum ESMPRESTType implements IHasID <String>, IHasDisplayName
{
  PEPPOL ("peppol", "PEPPOL"),
  BDXR ("bdxr", "OASIS BDXR");

  private final String m_sID;
  private final String m_sDisplayName;

  private ESMPRESTType (@Nonnull @Nonempty final String sID, @Nonnull @Nonempty final String sDisplayName)
  {
    m_sID = sID;
    m_sDisplayName = sDisplayName;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getDisplayName ()
  {
    return m_sDisplayName;
  }

  /**
   * @return <code>true</code> if this REST type supports retrieving the
   *         complete service group
   */
  public boolean isCompleteServiceGroupSupported ()
  {
    return this == PEPPOL;
  }

  public boolean isBDXR ()
  {
    return this == BDXR;
  }

  public boolean isPEPPOL ()
  {
    return this == PEPPOL;
  }

  @Nullable
  public static ESMPRESTType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMPRESTType.class, sID);
  }

  @Nullable
  public static ESMPRESTType getFromIDOrDefault (@Nullable final String sID, @Nullable final ESMPRESTType eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ESMPRESTType.class, sID, eDefault);
  }
}
