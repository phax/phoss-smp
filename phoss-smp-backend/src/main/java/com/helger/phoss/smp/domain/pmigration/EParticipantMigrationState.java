package com.helger.phoss.smp.domain.pmigration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.name.IHasDisplayName;

/**
 * Defines the state of a single SMP participant migration state
 *
 * @author Philip Helger
 * @since 5.4.0
 */
public enum EParticipantMigrationState implements IHasID <String>, IHasDisplayName
{
  /** The migration is in progress */
  IN_PROGRESS ("inprogress", "In progress"),
  /** The migration was cancelled */
  CANCELLED ("cancelled", "Cancelled"),
  /** The migration was successfully performed. */
  MIGRATED ("migrated", "Migrated");

  private final String m_sID;
  private final String m_sDisplayName;

  EParticipantMigrationState (@Nonnull @Nonempty final String sID, @Nonnull @Nonempty final String sDisplayName)
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
   * @return <code>true</code> if this state indicates, that the migration is in
   *         progress.
   */
  public boolean isInProgress ()
  {
    return this == IN_PROGRESS;
  }

  /**
   * @return <code>true</code> if this state prevents a new participant for the
   *         same migration to start.
   */
  public boolean preventsNewMigration ()
  {
    return this == IN_PROGRESS || this == MIGRATED;
  }

  @Nullable
  public static EParticipantMigrationState getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EParticipantMigrationState.class, sID);
  }
}
