/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.backend.sql;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.name.IHasDisplayName;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Database types - the next enum
 *
 * @author Philip Helger
 */
public enum EDatabaseType implements IHasID <String>, IHasDisplayName
{
  // IDs must be lowercase because they are used in directory names
  MYSQL ("mysql", "MySQL"),
  POSTGRESQL ("postgresql", "PostgreSQL"),
  /** Added in v5.6.0 */
  ORACLE ("oracle", "Oracle"),
  /** Added in v5.7.0 */
  DB2 ("db2", "IBM DB2");

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
  public static EDatabaseType getFromCaseIDInsensitiveOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrNull (EDatabaseType.class, sID);
  }
}
