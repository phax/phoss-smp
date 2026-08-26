/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.sql;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.phoss.smp.domain.SMPPagingHelper;

/**
 * Helper class to create the database specific SQL clause to limit a query to a single "page" of
 * results. Requires an <code>ORDER BY</code> clause to be present in the query, to ensure a stable
 * order.
 *
 * @author Philip Helger
 * @since 8.2.1
 */
@Immutable
public final class SMPJDBCPagingHelper
{
  private SMPJDBCPagingHelper ()
  {}

  /**
   * Get the database specific SQL clause to only return a single "page" of a query result. The
   * returned clause starts with a blank and must be appended to a query that contains an
   * <code>ORDER BY</code> clause.
   *
   * @param eDBType
   *        The database type to use. May not be <code>null</code>.
   * @param nStartIndex
   *        The 0-based index of the first row to be returned. Must be &ge; 0.
   * @param nMaxCount
   *        The maximum number of rows to be returned. Must be &ge; 0.
   * @return The SQL clause to be appended. Never <code>null</code>.
   */
  @NonNull
  public static String getPagingClause (@NonNull final EDatabaseSystemType eDBType,
                                        @Nonnegative final int nStartIndex,
                                        @Nonnegative final int nMaxCount)
  {
    SMPPagingHelper.checkPagingParams (nStartIndex, nMaxCount);

    // MySQL does not support the SQL standard "OFFSET .. FETCH" syntax
    if (eDBType == EDatabaseSystemType.MYSQL)
      return " LIMIT " + nMaxCount + " OFFSET " + nStartIndex;

    // DB2, Oracle, PostgreSQL and SQL Server all support the SQL standard
    return " OFFSET " + nStartIndex + " ROWS FETCH NEXT " + nMaxCount + " ROWS ONLY";
  }

  /**
   * Get the database specific SQL clause to only return a single "page" of a query result, using
   * the currently configured database type.
   *
   * @param nStartIndex
   *        The 0-based index of the first row to be returned. Must be &ge; 0.
   * @param nMaxCount
   *        The maximum number of rows to be returned. Must be &ge; 0.
   * @return The SQL clause to be appended. Never <code>null</code>.
   */
  @NonNull
  public static String getPagingClause (@Nonnegative final int nStartIndex, @Nonnegative final int nMaxCount)
  {
    return getPagingClause (SMPDataSourceSingleton.getDatabaseType (), nStartIndex, nMaxCount);
  }
}
