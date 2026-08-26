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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.helger.db.api.EDatabaseSystemType;

/**
 * Test class for class {@link SMPJDBCPagingHelper}.
 *
 * @author Philip Helger
 */
public final class SMPJDBCPagingHelperTest
{
  @Test
  public void testGetPagingClause ()
  {
    // MySQL uses a proprietary syntax
    assertEquals (" LIMIT 25 OFFSET 50", SMPJDBCPagingHelper.getPagingClause (EDatabaseSystemType.MYSQL, 50, 25));

    // All other supported DBs use the SQL standard syntax
    for (final EDatabaseSystemType eDBType : new EDatabaseSystemType [] { EDatabaseSystemType.DB2,
                                                                          EDatabaseSystemType.ORACLE,
                                                                          EDatabaseSystemType.POSTGRESQL,
                                                                          EDatabaseSystemType.SQLSERVER })
      assertEquals (" OFFSET 50 ROWS FETCH NEXT 25 ROWS ONLY",
                    SMPJDBCPagingHelper.getPagingClause (eDBType, 50, 25));
  }

  @Test
  public void testInvalidParams ()
  {
    try
    {
      SMPJDBCPagingHelper.getPagingClause (EDatabaseSystemType.MYSQL, -1, 25);
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }

    try
    {
      SMPJDBCPagingHelper.getPagingClause (EDatabaseSystemType.MYSQL, 0, -1);
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }
}
