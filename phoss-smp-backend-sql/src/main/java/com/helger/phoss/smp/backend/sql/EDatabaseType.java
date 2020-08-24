package com.helger.phoss.smp.backend.sql;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.name.IHasDisplayName;

/**
 * Database types - the next enum
 *
 * @author Philip Helger
 */
public enum EDatabaseType implements IHasID <String>, IHasDisplayName
{
  MYSQL ("mysql", "MySQL"),
  POSTGRESQL ("postgresql", "PostgreSQL"),
  GENERIC ("generic", "Generic SQL");

  private final String m_sID;
  private final String m_sDisplayName;

  EDatabaseType (@Nonnull @Nonempty final String sID, @Nonnull @Nonempty final String sDisplayName)
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

  @Nullable
  public static EDatabaseType getFromIDOrDefault (@Nullable final String sID)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrDefault (EDatabaseType.class, sID, EDatabaseType.GENERIC);
  }
}
