/*
 * Copyright (C) 2019-2021 Philip Helger and contributors
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.datetime.XMLOffsetDateTime;
import com.helger.db.jdbc.executor.DBExecutor;

/**
 * Base class for all JDBC enabled managers.
 *
 * @author Philip Helger
 */
public abstract class AbstractJDBCEnabledManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractJDBCEnabledManager.class);
  private final Supplier <? extends DBExecutor> m_aDBExecSupplier;

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be
   *        <code>null</code>.
   */
  protected AbstractJDBCEnabledManager (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    ValueEnforcer.notNull (aDBExecSupplier, "DBExecSupplier");
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
  public static Timestamp toTimestamp (@Nullable final LocalDateTime aLDT)
  {
    return aLDT == null ? null : Timestamp.valueOf (aLDT);
  }

  @Nullable
  public static Timestamp toTimestamp (@Nullable final XMLOffsetDateTime aODT)
  {
    return aODT == null ? null : Timestamp.from (aODT.toInstant ());
  }

  @Nullable
  public static String getTrimmedToLength (@Nullable final String s, final int nMaxLengthIncl)
  {
    ValueEnforcer.isGT0 (nMaxLengthIncl, "MaxLengthIncl");
    if (s == null)
      return null;

    final int nLength = s.length ();
    if (nLength <= nMaxLengthIncl)
      return s;

    LOGGER.warn ("Cutting value with length " + nLength + " to " + nMaxLengthIncl + " for DB");
    return s.substring (0, nMaxLengthIncl);
  }
}
