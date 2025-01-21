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
package com.helger.phoss.smp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.Since;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.name.IHasDisplayName;
import com.helger.peppol.sml.ESMPAPIType;

/**
 * Defines the type of REST responses to be returned. Either Peppol (using
 * objects in namespace http://busdox.org/serviceMetadata/publishing/1.0/) or
 * BDXR (using objects in namespace
 * http://docs.oasis-open.org/bdxr/ns/SMP/2016/05)
 *
 * @author Philip Helger
 */
public enum ESMPRESTType implements IHasID <String>, IHasDisplayName
{
  PEPPOL ("peppol", "Peppol", ESMPAPIType.PEPPOL),
  OASIS_BDXR_V1 ("bdxr", "OASIS BDXR SMP v1", ESMPAPIType.OASIS_BDXR_V1),
  @Since ("5.7.0")
  OASIS_BDXR_V2("bdxr2", "OASIS BDXR SMP v2", ESMPAPIType.OASIS_BDXR_V2);

  private final String m_sID;
  private final String m_sDisplayName;
  private final ESMPAPIType m_eAPIType;

  ESMPRESTType (@Nonnull @Nonempty final String sID,
                @Nonnull @Nonempty final String sDisplayName,
                @Nonnull final ESMPAPIType eAPIType)
  {
    m_sID = sID;
    m_sDisplayName = sDisplayName;
    m_eAPIType = eAPIType;
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

  @Nonnull
  public ESMPAPIType getAPIType ()
  {
    return m_eAPIType;
  }

  public boolean isPeppol ()
  {
    return this == PEPPOL;
  }

  public boolean isBDXR ()
  {
    return this == OASIS_BDXR_V1 || this == OASIS_BDXR_V2;
  }

  /**
   * @return <code>true</code> if this REST type supports retrieving the
   *         complete service group
   */
  public boolean isCompleteServiceGroupSupported ()
  {
    return this == PEPPOL || this == OASIS_BDXR_V1;
  }

  /**
   * @return <code>true</code> if this REST API enforces the usage of HTTP and
   *         does not allow HTTPS.
   */
  public boolean isHttpConstraint ()
  {
    return this == PEPPOL;
  }

  /**
   * @return <code>true</code> if this REST API enforces the usage of port 80.
   */
  public boolean isPort80Constraint ()
  {
    return this == PEPPOL;
  }

  /**
   * @return <code>true</code> if this REST API enforces the usage of the "/"
   *         path.
   */
  public boolean isPathConstraint ()
  {
    return this == PEPPOL;
  }

  /**
   * @return The query path prefix for the particular REST type. This was
   *         introduced for OASIS BDXR SMP v2 support. Never <code>null</code>
   *         but maybe empty. If a path is returned, it must end with a slash.
   * @since 5.7.0
   */
  @Nonnull
  public String getQueryPathPrefix ()
  {
    return this == OASIS_BDXR_V2 ? "bdxr-smp-2/" : "";
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
