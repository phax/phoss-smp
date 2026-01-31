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
package com.helger.phoss.smp;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.misc.Since;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.name.IHasDisplayName;
import com.helger.peppol.sml.ESMPAPIType;

/**
 * Defines the type of REST responses to be returned. Either Peppol (using objects in namespace
 * http://busdox.org/serviceMetadata/publishing/1.0/) or BDXR (using objects in namespace
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

  ESMPRESTType (@NonNull @Nonempty final String sID,
                @NonNull @Nonempty final String sDisplayName,
                @NonNull final ESMPAPIType eAPIType)
  {
    m_sID = sID;
    m_sDisplayName = sDisplayName;
    m_eAPIType = eAPIType;
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

  @NonNull
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
   * @return <code>true</code> if this REST type supports retrieving the complete service group
   */
  public boolean isCompleteServiceGroupSupported ()
  {
    return this == PEPPOL || this == OASIS_BDXR_V1;
  }

  /**
   * @return <code>true</code> if this REST API allows the usage of HTTP and HTTPS in parallel. This
   *         is only true for {@link #PEPPOL} until 2026-02-01. Afterwards Peppol must also use
   *         HTTPS only.
   */
  public boolean isHttpAlsoAllowed ()
  {
    return this == PEPPOL;
  }

  /**
   * @return The query path prefix for the particular REST type. This was introduced for OASIS BDXR
   *         SMP v2 support. Never <code>null</code> but maybe empty. If a path is returned, it must
   *         end with a slash.
   * @since 5.7.0
   */
  @NonNull
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
