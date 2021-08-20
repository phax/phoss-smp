/**
 * Copyright (C) 2019-2020 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.mgr;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.db.jdbc.executor.DBExecutor;

public abstract class AbstractJDBCEnabledManager
{
  private final Supplier <? extends DBExecutor> m_aDBExecSupplier;

  protected AbstractJDBCEnabledManager (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    m_aDBExecSupplier = aDBExecSupplier;
  }

  @Nonnull
  protected final DBExecutor newExecutor ()
  {
    return m_aDBExecSupplier.get ();
  }

  @Nullable
  public static Time toTime (@Nullable final LocalTime aLT)
  {
    return aLT == null ? null : Time.valueOf (aLT);
  }

  @Nullable
  public static Date toDate (@Nullable final LocalDate aLD)
  {
    return aLD == null ? null : Date.valueOf (aLD);
  }

  @Nullable
  public static Timestamp toTimestamp (@Nullable final OffsetDateTime aLDT)
  {
    return aLDT == null ? null : Timestamp.from (aLDT.toInstant ());
  }
}
