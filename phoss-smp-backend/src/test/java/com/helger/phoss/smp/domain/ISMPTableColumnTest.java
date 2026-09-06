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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.phoss.smp.domain.businesscard.ESMPBusinessCardColumn;
import com.helger.phoss.smp.domain.pmigration.ESMPParticipantMigrationColumn;
import com.helger.phoss.smp.domain.redirect.ESMPRedirectColumn;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupColumn;
import com.helger.phoss.smp.domain.serviceinfo.ESMPServiceInformationColumn;

/**
 * Test class for the {@link ISMPTableColumn} enums of all domain objects. The behaviour of
 * {@code TableColumnHelper} itself is tested in ph-oton.
 *
 * @author Philip Helger
 */
public final class ISMPTableColumnTest
{
  private static final ISMPTableColumn <?> [] [] ALL_COLUMNS = { ESMPServiceGroupColumn.values (),
                                                                 ESMPServiceInformationColumn.values (),
                                                                 ESMPRedirectColumn.values (),
                                                                 ESMPBusinessCardColumn.values (),
                                                                 ESMPParticipantMigrationColumn.values () };

  private static void _testAllNamesUsable (@Nullable final ICommonsList <String> aNames)
  {
    if (aNames != null)
    {
      // Present but empty is a bug - use null instead
      assertFalse (aNames.isEmpty ());
      for (final String sName : aNames)
        assertTrue (StringHelper.isNotEmpty (sName));
    }
  }

  @Test
  public void testColumnsAreConsistent ()
  {
    for (final ISMPTableColumn <?> [] aColumns : ALL_COLUMNS)
    {
      assertTrue (aColumns.length > 0);

      final ICommonsSet <String> aIDs = new CommonsHashSet <> ();
      boolean bAnyDefaultSortColumn = false;
      for (final ISMPTableColumn <?> aColumn : aColumns)
      {
        // The IDs are the field names a client may sort by, so they must be unique
        assertTrue ("Duplicate column ID " + aColumn.getID (), aIDs.add (aColumn.getID ()));

        // A column that can neither be sorted nor searched by is dead weight
        assertTrue ("The column '" + aColumn.getID () + "' is neither sortable nor searchable",
                    aColumn.isSortable () || aColumn.isSearchable ());
        if (aColumn.isSortable ())
          assertNotNull (aColumn.getComparator ());
        if (aColumn.isSearchable ())
          assertNotNull (aColumn.getSearchValueProvider ());

        if (aColumn.isDefaultSortColumn ())
        {
          // Without a sortable default order the paging returns the rows of a page arbitrarily
          assertTrue ("The column '" + aColumn.getID () + "' is part of the default order but is not sortable",
                      aColumn.isSortable ());
          bAnyDefaultSortColumn = true;
        }

        _testAllNamesUsable (aColumn.getAllSQLColumnNames ());
        _testAllNamesUsable (aColumn.getAllMongoFieldNames ());
      }
      assertTrue ("No column declares a default sort order", bAnyDefaultSortColumn);
    }
  }

  private static <T extends ISMPTableColumn <?>> void _testGetFromID (final T @NonNull [] aColumns,
                                                                      @NonNull final Function <String, T> aResolver)
  {
    for (final T aColumn : aColumns)
      assertSame (aColumn, aResolver.apply (aColumn.getID ()));
    assertNull (aResolver.apply ("does-not-exist"));
    assertNull (aResolver.apply (null));
  }

  @Test
  public void testGetFromIDOrNull ()
  {
    _testGetFromID (ESMPServiceGroupColumn.values (), ESMPServiceGroupColumn::getFromIDOrNull);
    _testGetFromID (ESMPServiceInformationColumn.values (), ESMPServiceInformationColumn::getFromIDOrNull);
    _testGetFromID (ESMPRedirectColumn.values (), ESMPRedirectColumn::getFromIDOrNull);
    _testGetFromID (ESMPBusinessCardColumn.values (), ESMPBusinessCardColumn::getFromIDOrNull);
    _testGetFromID (ESMPParticipantMigrationColumn.values (), ESMPParticipantMigrationColumn::getFromIDOrNull);
  }
}
