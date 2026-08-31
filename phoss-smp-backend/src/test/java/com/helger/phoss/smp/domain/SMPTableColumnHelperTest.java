/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.base.compare.ESortOrder;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.collection.paging.PagingSpec;
import com.helger.collection.paging.SortField;
import com.helger.phoss.smp.domain.businesscard.ESMPBusinessCardColumn;
import com.helger.phoss.smp.domain.pmigration.ESMPParticipantMigrationColumn;
import com.helger.phoss.smp.domain.redirect.ESMPRedirectColumn;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupColumn;
import com.helger.phoss.smp.domain.serviceinfo.ESMPServiceInformationColumn;

/**
 * Test class for class {@link SMPTableColumnHelper}.
 *
 * @author Philip Helger
 */
public final class SMPTableColumnHelperTest
{
  /**
   * A minimal column implementation on plain Strings, so that the helper can be tested without any
   * domain object scaffolding.
   *
   * @author Philip Helger
   */
  private static final class MockColumn implements ISMPTableColumn <String>
  {
    private final String m_sID;
    private final boolean m_bSortable;
    private final boolean m_bSearchable;
    private final ESortOrder m_eDefaultSortOrder;
    private final Function <String, String> m_aValueProvider;

    MockColumn (@NonNull final String sID,
                final boolean bSortable,
                final boolean bSearchable,
                @Nullable final ESortOrder eDefaultSortOrder,
                @NonNull final Function <String, String> aValueProvider)
    {
      m_sID = sID;
      m_bSortable = bSortable;
      m_bSearchable = bSearchable;
      m_eDefaultSortOrder = eDefaultSortOrder;
      m_aValueProvider = aValueProvider;
    }

    public String getID ()
    {
      return m_sID;
    }

    @Nullable
    public ICommonsList <String> getAllSQLColumnNames ()
    {
      return new CommonsArrayList <> (m_sID);
    }

    @Nullable
    public ICommonsList <String> getAllMongoFieldNames ()
    {
      return new CommonsArrayList <> (m_sID);
    }

    public boolean isSortable ()
    {
      return m_bSortable;
    }

    public boolean isSearchable ()
    {
      return m_bSearchable;
    }

    @Nullable
    public ESortOrder getDefaultSortOrder ()
    {
      return m_eDefaultSortOrder;
    }

    @NonNull
    public Function <String, String> getValueProvider ()
    {
      return m_aValueProvider;
    }
  }

  // "first" sorts by the first character, "all" by the whole value
  private static final MockColumn COL_FIRST = new MockColumn ("first", true, true, ESortOrder.ASCENDING, x -> x.substring (0, 1));
  private static final MockColumn COL_ALL = new MockColumn ("all", true, true, null, x -> x);
  private static final MockColumn COL_NOSORT = new MockColumn ("nosort", false, true, null, x -> x);
  private static final MockColumn COL_NOSEARCH = new MockColumn ("nosearch", true, false, null, x -> x);
  @SuppressWarnings ("unchecked")
  private static final ISMPTableColumn <String> [] COLUMNS = new ISMPTableColumn [] { COL_FIRST, COL_ALL, COL_NOSORT,
                                                                                      COL_NOSEARCH };
  private static final ICommonsList <String> VALUES = new CommonsArrayList <> ("bb", "ca", "ab", "aa", "ba");

  @Test
  public void testFindColumn ()
  {
    assertSame (COL_ALL, SMPTableColumnHelper.findColumn (COLUMNS, "all"));
    assertNull (SMPTableColumnHelper.findColumn (COLUMNS, "unknown"));
    assertNull (SMPTableColumnHelper.findColumn (COLUMNS, null));
    assertNull (SMPTableColumnHelper.findColumn (COLUMNS, ""));
  }

  @Test
  public void testSortColumnsIgnoreUnknownAndNonSortable ()
  {
    // Unknown and non-sortable field names must be dropped, because they come from a client
    final ICommonsList <SMPSortColumn <String>> aSortColumns;
    aSortColumns = SMPTableColumnHelper.getAllSortColumns (COLUMNS,
                                                           new PagingSpec (0,
                                                                           10,
                                                                           SortField.ascending ("does-not-exist"),
                                                                           SortField.descending ("nosort"),
                                                                           SortField.descending ("all")));
    assertEquals (1, aSortColumns.size ());
    assertSame (COL_ALL, aSortColumns.getFirstOrNull ().getColumn ());
    assertFalse (aSortColumns.getFirstOrNull ().isAscending ());
  }

  @Test
  public void testSortColumnsFallBackToDefaultOrder ()
  {
    // No sort field at all - the declared default order is used, so that paging is deterministic
    final ICommonsList <SMPSortColumn <String>> aSortColumns;
    aSortColumns = SMPTableColumnHelper.getAllSortColumns (COLUMNS, new PagingSpec (0, 10));
    assertEquals (1, aSortColumns.size ());
    assertSame (COL_FIRST, aSortColumns.getFirstOrNull ().getColumn ());
    assertTrue (aSortColumns.getFirstOrNull ().isAscending ());

    // Same for a specification whose sort fields are all unknown
    assertEquals (aSortColumns.size (),
                  SMPTableColumnHelper.getAllSortColumns (COLUMNS,
                                                          new PagingSpec (0, 10, SortField.ascending ("unknown")))
                                      .size ());
  }

  @Test
  public void testDefaultSortColumns ()
  {
    // Only the columns that declare a default sort order are part of it
    final ICommonsList <SMPSortColumn <String>> aDefault = SMPTableColumnHelper.getAllDefaultSortColumns (COLUMNS);
    assertEquals (1, aDefault.size ());
    assertSame (COL_FIRST, aDefault.getFirstOrNull ().getColumn ());

    assertEquals (new CommonsArrayList <> (SortField.ascending ("first")),
                  SMPTableColumnHelper.getAllDefaultSortFields (COLUMNS));
  }

  @Test
  public void testGetPageSorting ()
  {
    // Sorted by the whole value, ascending
    assertEquals (new CommonsArrayList <> ("aa", "ab"),
                  SMPTableColumnHelper.getPage (COLUMNS,
                                                VALUES,
                                                new PagingSpec (0, 2, SortField.ascending ("all")),
                                                null));
    assertEquals (new CommonsArrayList <> ("ba", "bb"),
                  SMPTableColumnHelper.getPage (COLUMNS,
                                                VALUES,
                                                new PagingSpec (2, 2, SortField.ascending ("all")),
                                                null));

    // Descending
    assertEquals (new CommonsArrayList <> ("ca", "bb"),
                  SMPTableColumnHelper.getPage (COLUMNS,
                                                VALUES,
                                                new PagingSpec (0, 2, SortField.descending ("all")),
                                                null));

    // Multiple sort fields: by first character descending, then by the whole value ascending
    assertEquals (new CommonsArrayList <> ("ca", "ba", "bb", "aa", "ab"),
                  SMPTableColumnHelper.getPage (COLUMNS,
                                                VALUES,
                                                new PagingSpec (0,
                                                                10,
                                                                SortField.descending ("first"),
                                                                SortField.ascending ("all")),
                                                null));
  }

  @Test
  public void testGetPageSearching ()
  {
    // Search is applied before the paging
    assertEquals (new CommonsArrayList <> ("aa", "ab", "ba", "ca"),
                  SMPTableColumnHelper.getPage (COLUMNS,
                                                VALUES,
                                                new PagingSpec (0, 10, SortField.ascending ("all")),
                                                "a"));
    assertEquals (4, SMPTableColumnHelper.getCount (COLUMNS, VALUES, "a"));

    // Case insensitive
    assertEquals (4, SMPTableColumnHelper.getCount (COLUMNS, VALUES, "A"));

    // No search text means everything
    assertEquals (VALUES.size (), SMPTableColumnHelper.getCount (COLUMNS, VALUES, null));
    assertEquals (VALUES.size (), SMPTableColumnHelper.getCount (COLUMNS, VALUES, ""));

    // Nothing matches
    assertEquals (0, SMPTableColumnHelper.getCount (COLUMNS, VALUES, "zzz"));
    assertTrue (SMPTableColumnHelper.getPage (COLUMNS, VALUES, new PagingSpec (0, 10), "zzz").isEmpty ());
  }

  @Test
  public void testSearchPredicate ()
  {
    assertNull (SMPTableColumnHelper.getSearchPredicate (COLUMNS, null));
    assertNull (SMPTableColumnHelper.getSearchPredicate (COLUMNS, ""));
    assertNotNull (SMPTableColumnHelper.getSearchPredicate (COLUMNS, "a"));

    // Only searchable columns are considered - if none is searchable, no filtering takes place and
    // a warning is logged
    @SuppressWarnings ("unchecked")
    final ISMPTableColumn <String> [] aOnlyNonSearchable = new ISMPTableColumn [] { COL_NOSEARCH };
    assertNull (SMPTableColumnHelper.getSearchPredicate (aOnlyNonSearchable, "a"));
    assertEquals (VALUES.size (), SMPTableColumnHelper.getCount (aOnlyNonSearchable, VALUES, "a"));
  }

  @Test
  public void testAllColumnEnums ()
  {
    // All column IDs of one domain object must be unique, and the first column must be sortable,
    // because it is the fallback order for the paging
    for (final ISMPTableColumn <?> [] aColumns : new ISMPTableColumn <?> [] [] { ESMPServiceGroupColumn.values (),
                                                                                 ESMPServiceInformationColumn.values (),
                                                                                 ESMPRedirectColumn.values (),
                                                                                 ESMPBusinessCardColumn.values (),
                                                                                 ESMPParticipantMigrationColumn.values () })
    {
      assertTrue (aColumns.length > 0);
      assertTrue ("The first column must be sortable", aColumns[0].isSortable ());

      final ICommonsSet <String> aIDs = new CommonsHashSet <> ();
      for (final ISMPTableColumn <?> aColumn : aColumns)
      {
        assertTrue ("Duplicate column ID " + aColumn.getID (), aIDs.add (aColumn.getID ()));
        assertNotNull (aColumn.getValueProvider ());

        // A sortable column must be resolvable in at least one backend
        if (aColumn.isSortable ())
          assertNotNull (aColumn.getComparator ());
      }
    }
  }
}
