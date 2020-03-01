/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.sql;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.db.jdbc.executor.DBExecutor;

public abstract class AbstractJDBCEnabledManager
{
  private static final DBExecutor s_aDBExec;

  static
  {
    s_aDBExec = new DBExecutor (SMPDataSourceSingleton.getInstance ().getDataSourceProvider ());
    s_aDBExec.setDebugConnections (false);
    s_aDBExec.setDebugTransactions (false);
    s_aDBExec.setDebugSQLStatements (true);
  }

  public AbstractJDBCEnabledManager ()
  {}

  @Nonnull
  protected final DBExecutor executor ()
  {
    return s_aDBExec;
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
}
