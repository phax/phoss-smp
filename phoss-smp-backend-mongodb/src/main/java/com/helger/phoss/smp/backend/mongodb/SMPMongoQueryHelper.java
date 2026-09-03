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
package com.helger.phoss.smp.backend.mongodb;

import java.util.regex.Pattern;

import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.IPagingSpec;
import com.helger.phoss.smp.domain.ISMPTableColumn;
import com.helger.phoss.smp.domain.SMPSortColumn;
import com.helger.phoss.smp.domain.SMPTableColumnHelper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * Helper class to turn the {@link ISMPTableColumn}s of a domain object into the MongoDB sort and
 * filter documents.
 *
 * @author Philip Helger
 * @since 8.2.1
 */
@Immutable
public final class SMPMongoQueryHelper
{
  private SMPMongoQueryHelper ()
  {}

  /**
   * Create the MongoDB sort document for the sort fields of the provided paging specification.
   * Unknown or non-sortable field names are ignored, because they are provided by a client. If no
   * sort field remains, the first column is used, so that consecutive page requests return disjunct
   * results.
   *
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param aPagingSpec
   *        The paging specification to be applied. May not be <code>null</code>.
   * @return <code>null</code> if no sort field could be resolved to a MongoDB field.
   */
  @Nullable
  public static Bson createSort (@NonNull final ISMPTableColumn <?> [] aColumns,
                                 @NonNull final IPagingSpec aPagingSpec)
  {
    ValueEnforcer.notNull (aColumns, "Columns");
    ValueEnforcer.notNull (aPagingSpec, "PagingSpec");

    final ICommonsList <Bson> aSorts = new CommonsArrayList <> ();
    for (final SMPSortColumn <?> aSortColumn : SMPTableColumnHelper.getAllSortColumns (_cast (aColumns), aPagingSpec))
    {
      final ICommonsList <String> aFieldNames = aSortColumn.getColumn ().getAllMongoFieldNames ();
      if (aFieldNames == null || aFieldNames.isEmpty ())
        continue;

      aSorts.add (aSortColumn.isAscending () ? Sorts.ascending (aFieldNames) : Sorts.descending (aFieldNames));
    }
    return aSorts.isEmpty () ? null : Sorts.orderBy (aSorts);
  }

  /**
   * Create the MongoDB filter that matches the provided search text against all searchable columns.
   * The search text is quoted, so that it is always matched literally and can never act as a
   * regular expression.
   *
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param sSearchText
   *        The search text as entered by the user. May be <code>null</code> or empty in which case
   *        no filter is created.
   * @return <code>null</code> if no filtering is to take place.
   */
  @Nullable
  public static Bson createSearchFilter (@NonNull final ISMPTableColumn <?> [] aColumns,
                                         @Nullable final String sSearchText)
  {
    ValueEnforcer.notNull (aColumns, "Columns");

    if (StringHelper.isEmpty (sSearchText))
      return null;

    // Quote, so that the search text can never be interpreted as a regular expression
    final Pattern aPattern = Pattern.compile (Pattern.quote (sSearchText), Pattern.CASE_INSENSITIVE);
    final ICommonsList <Bson> aFilters = new CommonsArrayList <> ();
    for (final ISMPTableColumn <?> aColumn : aColumns)
      if (aColumn.isSearchable ())
      {
        final ICommonsList <String> aFieldNames = aColumn.getAllMongoFieldNames ();
        if (aFieldNames != null)
          for (final String sFieldName : aFieldNames)
            aFilters.add (Filters.regex (sFieldName, aPattern));
      }
    return aFilters.isEmpty () ? null : Filters.or (aFilters);
  }

  @SuppressWarnings ("unchecked")
  @NonNull
  private static <T> ISMPTableColumn <T> [] _cast (@NonNull final ISMPTableColumn <?> [] aColumns)
  {
    return (ISMPTableColumn <T> []) aColumns;
  }
}
