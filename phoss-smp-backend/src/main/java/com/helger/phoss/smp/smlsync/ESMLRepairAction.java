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
package com.helger.phoss.smp.smlsync;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.name.IHasDisplayName;

/**
 * The two directions in which the difference between this SMP and the SML can be repaired, based on
 * a reconciliation report of {@link SMLSyncJob}.
 *
 * @author Philip Helger
 * @since 8.5.1
 */
public enum ESMLRepairAction implements IHasID <String>, IHasDisplayName
{
  /** Register the participants that exist locally but are missing at the SML */
  REGISTER_MISSING ("registermissing",
                    "Register the missing participants at the SML",
                    CSMLSync.ENTRY_MISSING_IN_SML),
  /** Remove the participants the SML holds for this SMP that do not exist locally */
  REMOVE_ORPHANS ("removeorphans",
                  "Remove the orphaned participants from the SML",
                  CSMLSync.ENTRY_ORPHANS_IN_SML);

  private final String m_sID;
  private final String m_sDisplayName;
  private final String m_sReportEntry;

  ESMLRepairAction (@NonNull @Nonempty final String sID,
                    @NonNull @Nonempty final String sDisplayName,
                    @NonNull @Nonempty final String sReportEntry)
  {
    m_sID = sID;
    m_sDisplayName = sDisplayName;
    m_sReportEntry = sReportEntry;
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
   * @return The name of the ZIP entry of a reconciliation report that holds the participants this
   *         action operates on. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getReportEntry ()
  {
    return m_sReportEntry;
  }

  /**
   * @return <code>true</code> if this action removes participants from the SML, which makes them
   *         unreachable and allows another SMP to claim them.
   */
  public boolean isDestructive ()
  {
    return this == REMOVE_ORPHANS;
  }

  @Nullable
  public static ESMLRepairAction getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMLRepairAction.class, sID);
  }
}
