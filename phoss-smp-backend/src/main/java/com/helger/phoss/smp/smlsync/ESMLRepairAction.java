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
                    "Register missing",
                    "Register the missing participants at the SML",
                    CSMLSync.ENTRY_MISSING_IN_SML,
                    false),
  /** Remove the participants the SML holds for this SMP that do not exist locally */
  REMOVE_ORPHANS ("removeorphans",
                  "Remove orphans from SML",
                  "Remove the orphaned participants from the SML",
                  CSMLSync.ENTRY_ORPHANS_IN_SML,
                  false),
  /**
   * Create the participants the SML holds for this SMP that do not exist locally as local Service
   * Groups. This is the constructive resolution of the same difference that
   * {@link #REMOVE_ORPHANS} resolves destructively - after a restore that lost local data, adopting
   * the participants is what is wanted, not deleting them from the network.
   */
  CREATE_LOCALLY ("createlocally",
                  "Create orphans locally",
                  "Create the orphaned participants as local Service Groups",
                  CSMLSync.ENTRY_ORPHANS_IN_SML,
                  true);

  private final String m_sID;
  private final String m_sShortName;
  private final String m_sDisplayName;
  private final String m_sReportEntry;
  private final boolean m_bLocalOperation;

  ESMLRepairAction (@NonNull @Nonempty final String sID,
                    @NonNull @Nonempty final String sShortName,
                    @NonNull @Nonempty final String sDisplayName,
                    @NonNull @Nonempty final String sReportEntry,
                    final boolean bLocalOperation)
  {
    m_sID = sID;
    m_sShortName = sShortName;
    m_sDisplayName = sDisplayName;
    m_sReportEntry = sReportEntry;
    m_bLocalOperation = bLocalOperation;
  }

  /**
   * @return A short name, suitable for a table column header and a button. Neither
   *         <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getShortName ()
  {
    return m_sShortName;
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

  /**
   * @return <code>true</code> if this action only changes the local Service Groups and performs no
   *         SML call at all. The participants of such an action are by definition already
   *         registered at the SML, so the Service Groups must be created without informing the
   *         SML - otherwise the SML rejects them as already in use.
   */
  public boolean isLocalOperation ()
  {
    return m_bLocalOperation;
  }

  @Nullable
  public static ESMLRepairAction getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMLRepairAction.class, sID);
  }
}
