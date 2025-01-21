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
package com.helger.phoss.smp.domain.pmigration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Defines the directions for SMP participant migrations.
 *
 * @author Philip Helger
 * @since 5.4.0
 */
public enum EParticipantMigrationDirection implements IHasID <String>
{
  /** Migrate a participant from this SMP to a different SMP. */
  OUTBOUND ("outbound"),
  /** Migrate a participant from another SMP to this SMP. */
  INBOUND ("inbound");

  private final String m_sID;

  EParticipantMigrationDirection (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public boolean isOutbound ()
  {
    return this == OUTBOUND;
  }

  public boolean isInbound ()
  {
    return this == INBOUND;
  }

  @Nullable
  public static EParticipantMigrationDirection getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EParticipantMigrationDirection.class, sID);
  }
}
