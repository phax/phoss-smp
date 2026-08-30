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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.db.api.paging.IDBColumnNameResolver;
import com.helger.phoss.smp.backend.sql.SMPJDBCQueryHelper.SearchCondition;
import com.helger.phoss.smp.domain.redirect.ESMPRedirectColumn;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupColumn;

/**
 * Test class for class {@link SMPJDBCQueryHelper}.
 *
 * @author Philip Helger
 */
public final class SMPJDBCQueryHelperTest
{
  @Test
  public void testColumnNameResolver ()
  {
    final IDBColumnNameResolver aResolver = SMPJDBCQueryHelper.createColumnNameResolver (ESMPServiceGroupColumn.values ());

    // A logical column that consists of two SQL columns
    assertEquals (new CommonsArrayList <> ("sg.businessIdentifierScheme", "sg.businessIdentifier"),
                  aResolver.getAllSQLColumnNames ("participantid"));
    assertEquals (new CommonsArrayList <> ("so.username"), aResolver.getAllSQLColumnNames ("owner"));

    // A forged field name must never resolve
    assertNull (aResolver.getAllSQLColumnNames ("sg.businessIdentifier"));
    assertNull (aResolver.getAllSQLColumnNames ("1; DROP TABLE smp_service_group"));
    assertNull (aResolver.getAllSQLColumnNames ("unknown"));
  }

  @Test
  public void testColumnNameResolverSkipsNonSortable ()
  {
    // The Business Card name is searchable but not sortable, so it may not appear in ORDER BY
    final IDBColumnNameResolver aResolver;
    aResolver = SMPJDBCQueryHelper.createColumnNameResolver (com.helger.phoss.smp.domain.businesscard.ESMPBusinessCardColumn.values ());
    assertEquals (new CommonsArrayList <> ("pid"), aResolver.getAllSQLColumnNames ("servicegroup"));
    assertNull (aResolver.getAllSQLColumnNames ("name"));
  }

  @Test
  public void testSearchConditionEmpty ()
  {
    assertTrue (SMPJDBCQueryHelper.createSearchCondition (ESMPServiceGroupColumn.values (), null).isEmpty ());
    assertTrue (SMPJDBCQueryHelper.createSearchCondition (ESMPServiceGroupColumn.values (), "").isEmpty ());
  }

  @Test
  public void testSearchConditionServiceGroup ()
  {
    // Only the participant ID is searchable, and it consists of two columns
    final SearchCondition aSC = SMPJDBCQueryHelper.createSearchCondition (ESMPServiceGroupColumn.values (), "0088");
    assertFalse (aSC.isEmpty ());
    assertEquals ("(LOWER(sg.businessIdentifierScheme) LIKE ? ESCAPE '!'" +
                  " OR LOWER(sg.businessIdentifier) LIKE ? ESCAPE '!')",
                  aSC.getSQL ());
    assertEquals (new CommonsArrayList <Object> ("%0088%", "%0088%"), aSC.getAllParams ());
  }

  @Test
  public void testSearchConditionRedirect ()
  {
    final SearchCondition aSC = SMPJDBCQueryHelper.createSearchCondition (ESMPRedirectColumn.values (), "x");
    // 2 + 2 + 1 columns
    assertEquals (5, aSC.getAllParams ().size ());
    assertTrue (aSC.getSQL ().contains ("LOWER(redirectionUrl) LIKE ?"));
  }

  @Test
  public void testLikePatternEscaping ()
  {
    // The search text is lower cased and enclosed in wildcards
    assertEquals ("%abc%", SMPJDBCQueryHelper.getLikePattern ("AbC"));

    // LIKE wildcards in the search text must be matched literally
    assertEquals ("%100!% of a!_b%", SMPJDBCQueryHelper.getLikePattern ("100% of a_b"));

    // The escape character itself must be escaped
    assertEquals ("%a!!b%", SMPJDBCQueryHelper.getLikePattern ("a!b"));

    // Empty search text
    assertEquals ("%%", SMPJDBCQueryHelper.getLikePattern (""));
  }
}
