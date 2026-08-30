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

import java.util.Comparator;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.compare.ESortOrder;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.IPagingSpec;
import com.helger.collection.paging.PagingHelper;
import com.helger.collection.paging.SortField;

/**
 * Helper class to resolve the {@link SortField}s of an {@link IPagingSpec} and the global search
 * text onto the {@link ISMPTableColumn}s of a domain object. It contains the backend independent
 * parts only - the SQL and the MongoDB specific resolution lives in the respective backend.
 *
 * @author Philip Helger
 * @since 8.2.1
 */
@Immutable
public final class SMPTableColumnHelper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPTableColumnHelper.class);

  private SMPTableColumnHelper ()
  {}

  /**
   * Find the column with the provided ID.
   *
   * @param <DATATYPE>
   *        The domain object type
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param sFieldName
   *        The field name to search. May be <code>null</code>. This value is usually provided by a
   *        client and must therefore be treated as untrusted input.
   * @return <code>null</code> if no such column exists.
   */
  @Nullable
  public static <DATATYPE> ISMPTableColumn <DATATYPE> findColumn (@NonNull final ISMPTableColumn <DATATYPE> [] aColumns,
                                                                  @Nullable final String sFieldName)
  {
    ValueEnforcer.notNull (aColumns, "Columns");

    if (StringHelper.isNotEmpty (sFieldName))
      for (final ISMPTableColumn <DATATYPE> aColumn : aColumns)
        if (aColumn.getID ().equals (sFieldName))
          return aColumn;
    return null;
  }

  /**
   * Get all sortable columns of the provided paging specification, in the order of precedence.
   * Field names that are unknown or that refer to a non-sortable column are ignored, because they
   * are provided by a client.
   *
   * @param <DATATYPE>
   *        The domain object type
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param aPagingSpec
   *        The paging specification to be resolved. May not be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list, with the matching sort order per column.
   */
  @NonNull
  @ReturnsMutableCopy
  public static <DATATYPE> ICommonsList <SMPSortColumn <DATATYPE>> getAllSortColumns (@NonNull final ISMPTableColumn <DATATYPE> [] aColumns,
                                                                                      @NonNull final IPagingSpec aPagingSpec)
  {
    ValueEnforcer.notNull (aColumns, "Columns");
    ValueEnforcer.notNull (aPagingSpec, "PagingSpec");

    final ICommonsList <SMPSortColumn <DATATYPE>> ret = new CommonsArrayList <> ();
    for (final SortField aSortField : aPagingSpec.getAllSortFields ())
    {
      final ISMPTableColumn <DATATYPE> aColumn = findColumn (aColumns, aSortField.getFieldName ());
      if (aColumn == null || !aColumn.isSortable ())
      {
        LOGGER.warn ("Ignoring the unknown or non-sortable sort field '" + aSortField.getFieldName () + "'");
        continue;
      }
      ret.add (new SMPSortColumn <> (aColumn, aSortField.getSortOrder ()));
    }

    if (ret.isEmpty () && aColumns.length > 0)
    {
      // Paging without a deterministic order returns arbitrary rows, so the first column is used
      // as the default order. All backends use this same fallback.
      ret.add (new SMPSortColumn <> (aColumns[0], ESortOrder.ASCENDING));
    }
    return ret;
  }

  /**
   * Create the comparator for the sort fields of the provided paging specification, for the
   * in-memory backends.
   *
   * @param <DATATYPE>
   *        The domain object type
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param aPagingSpec
   *        The paging specification to be resolved. May not be <code>null</code>.
   * @return <code>null</code> if no sort field could be resolved.
   */
  @Nullable
  public static <DATATYPE> Comparator <DATATYPE> getComparator (@NonNull final ISMPTableColumn <DATATYPE> [] aColumns,
                                                                @NonNull final IPagingSpec aPagingSpec)
  {
    Comparator <DATATYPE> ret = null;
    for (final SMPSortColumn <DATATYPE> aSortColumn : getAllSortColumns (aColumns, aPagingSpec))
    {
      final Comparator <DATATYPE> aColumnComparator = aSortColumn.getComparator ();
      ret = ret == null ? aColumnComparator : ret.thenComparing (aColumnComparator);
    }
    return ret;
  }

  /**
   * Create the predicate that matches the provided search text against all searchable columns, for
   * the in-memory backends.
   *
   * @param <DATATYPE>
   *        The domain object type
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param sSearchText
   *        The search text to be applied. May be <code>null</code> or empty.
   * @return <code>null</code> if no filtering is to take place.
   */
  @Nullable
  public static <DATATYPE> Predicate <DATATYPE> getSearchPredicate (@NonNull final ISMPTableColumn <DATATYPE> [] aColumns,
                                                                    @Nullable final String sSearchText)
  {
    ValueEnforcer.notNull (aColumns, "Columns");

    if (StringHelper.isEmpty (sSearchText))
      return null;

    Predicate <DATATYPE> ret = null;
    for (final ISMPTableColumn <DATATYPE> aColumn : aColumns)
      if (aColumn.isSearchable ())
      {
        final Predicate <DATATYPE> aColumnPredicate = x -> aColumn.matchesSearchText (x, sSearchText);
        ret = ret == null ? aColumnPredicate : ret.or (aColumnPredicate);
      }

    if (ret == null)
    {
      // No searchable column at all - don't silently return everything
      LOGGER.warn ("None of the provided columns is searchable, so the search text is ignored");
    }
    return ret;
  }

  /**
   * The in-memory implementation of a paged, sorted and filtered query. This is the reference
   * behaviour all native backend implementations must comply to.
   *
   * @param <DATATYPE>
   *        The domain object type
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param aAll
   *        All available domain objects. May not be <code>null</code>.
   * @param aPagingSpec
   *        The paging specification to be applied. May not be <code>null</code>.
   * @param sSearchText
   *        The global search text to be applied. May be <code>null</code> or empty.
   * @return A non-<code>null</code> but maybe empty list.
   */
  @NonNull
  @ReturnsMutableCopy
  public static <DATATYPE> ICommonsList <DATATYPE> getPage (@NonNull final ISMPTableColumn <DATATYPE> [] aColumns,
                                                            @NonNull final ICommonsList <DATATYPE> aAll,
                                                            @NonNull final IPagingSpec aPagingSpec,
                                                            @Nullable final String sSearchText)
  {
    ValueEnforcer.notNull (aAll, "All");

    final Predicate <DATATYPE> aFilter = getSearchPredicate (aColumns, sSearchText);
    final ICommonsList <DATATYPE> aMatching = aFilter == null ? aAll : aAll.getAll (aFilter);
    // No copy needed - all callers pass a list they own, because the manager methods returning all
    // entities are annotated with @ReturnsMutableCopy
    final boolean bCopyList = false;
    return PagingHelper.getPage (aMatching, bCopyList, aPagingSpec, getComparator (aColumns, aPagingSpec));
  }

  /**
   * The in-memory implementation of counting the domain objects matching a search text.
   *
   * @param <DATATYPE>
   *        The domain object type
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param aAll
   *        All available domain objects. May not be <code>null</code>.
   * @param sSearchText
   *        The global search text to be applied. May be <code>null</code> or empty.
   * @return The number of matching domain objects. Always &ge; 0.
   */
  @Nonnegative
  public static <DATATYPE> long getCount (@NonNull final ISMPTableColumn <DATATYPE> [] aColumns,
                                          @NonNull final ICommonsList <DATATYPE> aAll,
                                          @Nullable final String sSearchText)
  {
    ValueEnforcer.notNull (aAll, "All");

    final Predicate <DATATYPE> aFilter = getSearchPredicate (aColumns, sSearchText);
    return aFilter == null ? aAll.size () : aAll.getCount (aFilter);
  }
}
