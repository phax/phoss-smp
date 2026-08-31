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
import java.util.Locale;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.compare.CompareHelper;
import com.helger.base.compare.ESortOrder;
import com.helger.base.id.IHasID;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;

/**
 * Describes a single logical column of a domain object that can be sorted and/or searched by. It is
 * the single source of truth that ties the column shown in the UI to the SQL column, to the MongoDB
 * field and to an in-memory {@link Comparator}, so that these four cannot drift apart. It is
 * implemented as an enum per domain object.<br>
 * The IDs of these columns are the field names used in a
 * {@link com.helger.collection.paging.SortField}, and they are the only field names a client may
 * refer to - see {@link SMPTableColumnHelper}.
 *
 * @author Philip Helger
 * @param <DATATYPE>
 *        The domain object type this column belongs to
 * @since 8.2.1
 */
public interface ISMPTableColumn <DATATYPE> extends IHasID <String>
{
  /**
   * @return The SQL column names this column maps to, in the order of precedence. May be
   *         <code>null</code> or empty if this column has no representation in the SQL backend. A
   *         logical column may map to more than one SQL column, because e.g. a participant
   *         identifier is stored as a scheme and a value column.
   */
  @Nullable
  @ReturnsMutableCopy
  ICommonsList <String> getAllSQLColumnNames ();

  /**
   * @return The MongoDB field names this column maps to, in the order of precedence. May be
   *         <code>null</code> or empty if this column has no representation in the MongoDB backend.
   */
  @Nullable
  @ReturnsMutableCopy
  ICommonsList <String> getAllMongoFieldNames ();

  /**
   * @return <code>true</code> if the data may be sorted by this column, <code>false</code>
   *         otherwise. A column that is not sortable is silently ignored if a client asks to sort
   *         by it.
   */
  boolean isSortable ();

  /**
   * @return <code>true</code> if the global search covers this column, <code>false</code>
   *         otherwise.
   */
  boolean isSearchable ();

  /**
   * The sort order this column contributes to the <b>default order</b> - the order that is used if
   * a client requests no order at all, or only unknown respectively non-sortable ones. All columns
   * with a non-<code>null</code> value form the default order together, in their declaration
   * order, which is how a composite key like "service group, then document type" is expressed.<br>
   * A default order is mandatory for paging: without a deterministic order a query returns the rows
   * of a page arbitrarily, so that consecutive pages may overlap or lose rows. Therefore at least
   * one column of a domain object must declare one, and every such column must be sortable.
   *
   * @return The sort order to be used in the default order, or <code>null</code> if this column is
   *         not part of it.
   */
  @Nullable
  ESortOrder getDefaultSortOrder ();

  /**
   * @return <code>true</code> if this column is part of the default order.
   * @see #getDefaultSortOrder()
   */
  default boolean isDefaultSortColumn ()
  {
    return getDefaultSortOrder () != null;
  }

  /**
   * @return The provider of the value of this column for a single domain object. It is used for the
   *         in-memory sorting and searching, and it must therefore return the same value the SQL
   *         column respectively the MongoDB field contains. Never <code>null</code>.
   */
  @NonNull
  Function <DATATYPE, String> getValueProvider ();

  /**
   * Get the value of this column for the provided domain object.
   *
   * @param aObj
   *        The domain object to get the value of. May not be <code>null</code>.
   * @return The value. May be <code>null</code>.
   */
  @Nullable
  default String getValue (@NonNull final DATATYPE aObj)
  {
    return getValueProvider ().apply (aObj);
  }

  /**
   * @return A comparator that sorts by the value of this column, ascending, <code>null</code>
   *         values first. Never <code>null</code>.
   */
  @NonNull
  default Comparator <DATATYPE> getComparator ()
  {
    return (x, y) -> CompareHelper.compare (getValue (x), getValue (y), true);
  }

  /**
   * Check if the value of this column contains the provided search text, ignoring case.
   *
   * @param aObj
   *        The domain object to check. May not be <code>null</code>.
   * @param sSearchText
   *        The search text to be searched. May neither be <code>null</code> nor empty.
   * @return <code>true</code> if the value contains the search text.
   */
  default boolean matchesSearchText (@NonNull final DATATYPE aObj, @NonNull @Nonempty final String sSearchText)
  {
    final String sValue = getValue (aObj);
    if (StringHelper.isEmpty (sValue))
      return false;
    return sValue.toLowerCase (Locale.ROOT).contains (sSearchText.toLowerCase (Locale.ROOT));
  }
}
