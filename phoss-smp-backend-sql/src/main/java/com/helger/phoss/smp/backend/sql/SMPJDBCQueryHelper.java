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

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.IPagingSpec;
import com.helger.db.api.paging.DBPagingHelper;
import com.helger.db.api.paging.IDBColumnNameResolver;
import com.helger.phoss.smp.domain.ISMPTableColumn;

/**
 * Helper class to turn the {@link ISMPTableColumn}s of a domain object into the SQL clauses for
 * sorting, paging and searching.
 *
 * @author Philip Helger
 * @since 8.2.1
 */
@Immutable
public final class SMPJDBCQueryHelper
{
  /**
   * The escape character used in the <code>LIKE</code> clauses. A backslash cannot be used, because
   * MySQL treats it as an escape character inside string literals as well.
   */
  public static final char LIKE_ESCAPE_CHAR = '!';

  private SMPJDBCQueryHelper ()
  {}

  /**
   * Create the resolver from the logical field name of a sort field to the SQL column names. Only
   * sortable columns are resolved, everything else is rejected, so that a forged field name can
   * never reach the SQL statement.
   *
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static IDBColumnNameResolver createColumnNameResolver (@NonNull final ISMPTableColumn <?> [] aColumns)
  {
    ValueEnforcer.notNull (aColumns, "Columns");

    return sFieldName -> {
      for (final ISMPTableColumn <?> aColumn : aColumns)
        if (aColumn.isSortable () && aColumn.getID ().equals (sFieldName))
          return aColumn.getAllSQLColumnNames ();
      return null;
    };
  }

  /**
   * Create the combined <code>ORDER BY</code> and paging clause for the provided paging
   * specification.
   *
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param aPagingSpec
   *        The paging specification to be applied. May not be <code>null</code>.
   * @return The SQL clause, starting with a blank. Never <code>null</code> but maybe empty.
   */
  @NonNull
  public static String getOrderByAndPagingClause (@NonNull final ISMPTableColumn <?> [] aColumns,
                                                  @NonNull final IPagingSpec aPagingSpec)
  {
    return DBPagingHelper.getOrderByAndPagingClause (SMPDataSourceSingleton.getDatabaseType (),
                                                     aPagingSpec,
                                                     createColumnNameResolver (aColumns));
  }

  /**
   * Escape the wildcard characters of a <code>LIKE</code> pattern, so that a search text containing
   * <code>%</code> or <code>_</code> is matched literally.
   *
   * @param sSearchText
   *        The search text as entered by the user. May not be <code>null</code>.
   * @return The escaped and lower cased search text, enclosed in <code>%</code>. Never
   *         <code>null</code>.
   */
  @NonNull
  public static String getLikePattern (@NonNull final String sSearchText)
  {
    final StringBuilder aSB = new StringBuilder (sSearchText.length () + 8).append ('%');
    for (final char c : sSearchText.toLowerCase (Locale.ROOT).toCharArray ())
    {
      if (c == LIKE_ESCAPE_CHAR || c == '%' || c == '_')
        aSB.append (LIKE_ESCAPE_CHAR);
      aSB.append (c);
    }
    return aSB.append ('%').toString ();
  }

  /**
   * A single SQL search condition, consisting of the SQL fragment and the matching prepared
   * statement parameters.
   *
   * @author Philip Helger
   */
  @Immutable
  public static final class SearchCondition
  {
    private final String m_sSQL;
    private final ICommonsList <Object> m_aParams;

    SearchCondition (@NonNull final String sSQL, @NonNull final ICommonsList <Object> aParams)
    {
      m_sSQL = sSQL;
      m_aParams = aParams;
    }

    /**
     * @return The SQL fragment. Never <code>null</code> but empty if no searchable column exists.
     *         It is a self contained boolean expression enclosed in parentheses, so it can be
     *         combined with <code>AND</code> or used as a <code>WHERE</code> on its own.
     */
    @NonNull
    public String getSQL ()
    {
      return m_sSQL;
    }

    /**
     * @return The prepared statement parameters, in the order of the placeholders of the SQL
     *         fragment. Never <code>null</code> but maybe empty.
     */
    @NonNull
    @ReturnsMutableCopy
    public ICommonsList <Object> getAllParams ()
    {
      return m_aParams.getClone ();
    }

    public boolean isEmpty ()
    {
      return m_sSQL.isEmpty ();
    }

    public boolean isNotEmpty ()
    {
      return !m_sSQL.isEmpty ();
    }
  }

  /**
   * Create the SQL condition that matches the provided search text against all searchable columns.
   * The search text is always passed as a prepared statement parameter - only the column names come
   * from the provided columns.
   *
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param sSearchText
   *        The search text as entered by the user. May be <code>null</code> or empty in which case
   *        an empty condition is returned.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static SearchCondition createSearchCondition (@NonNull final ISMPTableColumn <?> [] aColumns,
                                                       @Nullable final String sSearchText)
  {
    ValueEnforcer.notNull (aColumns, "Columns");

    if (StringHelper.isEmpty (sSearchText))
      return new SearchCondition ("", new CommonsArrayList <> ());

    final String sLikePattern = getLikePattern (sSearchText);
    final ICommonsList <Object> aParams = new CommonsArrayList <> ();
    final StringBuilder aSB = new StringBuilder ();
    for (final ISMPTableColumn <?> aColumn : aColumns)
      if (aColumn.isSearchable ())
      {
        final ICommonsList <String> aColumnNames = aColumn.getAllSQLColumnNames ();
        if (aColumnNames != null)
          for (final String sColumnName : aColumnNames)
          {
            if (aSB.length () > 0)
              aSB.append (" OR ");
            aSB.append ("LOWER(").append (sColumnName).append (") LIKE ? ESCAPE '").append (LIKE_ESCAPE_CHAR)
               .append ('\'');
            aParams.add (sLikePattern);
          }
      }

    if (aSB.length () == 0)
      return new SearchCondition ("", new CommonsArrayList <> ());

    return new SearchCondition ("(" + aSB + ")", aParams);
  }
}
