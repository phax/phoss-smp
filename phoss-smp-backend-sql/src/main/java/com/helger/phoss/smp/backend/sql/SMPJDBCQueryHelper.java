/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.backend.sql;

import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.IPagingSpec;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.api.paging.DBPagingHelper;
import com.helger.db.api.paging.IDBColumnNameResolver;
import com.helger.phoss.smp.domain.ISMPTableColumn;
import com.helger.photon.core.paging.TableColumnHelper;

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

  /**
   * The number of leading characters of a LOB column that are used for sorting on databases that
   * cannot sort by a LOB column. Oracle limits a <code>VARCHAR2</code> in SQL to 4000 bytes, so this
   * fits even with 4 bytes per character.
   *
   * @since 8.8.0
   */
  public static final int LOB_SORT_LENGTH = 1000;

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
   * Create the database specific SQL expression to sort by a LOB column. Oracle and DB2 cannot use a
   * LOB column in an <code>ORDER BY</code> clause, so only the first {@link #LOB_SORT_LENGTH}
   * characters are used there. All other databases sort by the column itself.
   *
   * @param eDBType
   *        The database system to create the expression for. May not be <code>null</code>.
   * @param sColumnName
   *        The name of the LOB column. May neither be <code>null</code> nor empty.
   * @return The SQL expression. Never <code>null</code>.
   * @since 8.8.0
   */
  @NonNull
  public static String getLobSortExpression (@NonNull final EDatabaseSystemType eDBType,
                                             @NonNull @Nonempty final String sColumnName)
  {
    ValueEnforcer.notNull (eDBType, "DBType");
    ValueEnforcer.notEmpty (sColumnName, "ColumnName");

    return switch (eDBType)
    {
      case ORACLE -> "DBMS_LOB.SUBSTR(" + sColumnName + ", " + LOB_SORT_LENGTH + ", 1)";
      case DB2 -> "CAST(" + sColumnName + " AS VARCHAR(" + LOB_SORT_LENGTH + "))";
      default -> sColumnName;
    };
  }

  /**
   * Create the resolver from the logical field name of a sort field to the SQL column expressions.
   * Only sortable columns are resolved, everything else is rejected, so that a forged field name can
   * never reach the SQL statement. The provided LOB columns are resolved to a database specific
   * expression that can be sorted by.
   *
   * @param eDBType
   *        The database system to create the resolver for. May not be <code>null</code>.
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param aLobColumnNames
   *        The SQL names of all columns that are LOB columns in at least one database system. May
   *        be <code>null</code>.
   * @return Never <code>null</code>.
   * @see #getLobSortExpression(EDatabaseSystemType, String)
   * @since 8.8.0
   */
  @NonNull
  public static IDBColumnNameResolver createColumnNameResolver (@NonNull final EDatabaseSystemType eDBType,
                                                                @NonNull final ISMPTableColumn <?> [] aColumns,
                                                                @Nullable final Set <String> aLobColumnNames)
  {
    ValueEnforcer.notNull (eDBType, "DBType");

    final IDBColumnNameResolver aResolver = createColumnNameResolver (aColumns);
    if (aLobColumnNames == null || aLobColumnNames.isEmpty ())
      return aResolver;

    return sFieldName -> {
      final ICommonsList <String> ret = aResolver.getAllSQLColumnNames (sFieldName);
      if (ret != null)
        ret.replaceAll (x -> aLobColumnNames.contains (x) ? getLobSortExpression (eDBType, x) : x);
      return ret;
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
    return getOrderByAndPagingClause (SMPDataSourceSingleton.getDatabaseType (), aColumns, aPagingSpec);
  }

  /**
   * Create the combined <code>ORDER BY</code> and paging clause for the provided paging
   * specification. If the specification contains no usable sort field - because none was requested
   * or because none could be resolved - the default order declared by the provided columns is used,
   * so that the created SQL always contains an <code>ORDER BY</code> clause. Without it a query
   * would return the rows of a page in an undefined order, and consecutive pages could overlap or
   * lose rows.
   *
   * @param eDBType
   *        The database system to create the clause for. May not be <code>null</code>.
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param aPagingSpec
   *        The paging specification to be applied. May not be <code>null</code>.
   * @return The SQL clause, starting with a blank. Never <code>null</code> but maybe empty.
   * @see ISMPTableColumn#getDefaultSortOrder()
   */
  @NonNull
  public static String getOrderByAndPagingClause (@NonNull final EDatabaseSystemType eDBType,
                                                  @NonNull final ISMPTableColumn <?> [] aColumns,
                                                  @NonNull final IPagingSpec aPagingSpec)
  {
    return getOrderByAndPagingClause (eDBType, aColumns, aPagingSpec, null);
  }

  /**
   * Create the combined <code>ORDER BY</code> and paging clause for the provided paging
   * specification, for a table that contains LOB columns.
   *
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param aPagingSpec
   *        The paging specification to be applied. May not be <code>null</code>.
   * @param aLobColumnNames
   *        The SQL names of all columns that are LOB columns in at least one database system. May
   *        be <code>null</code>.
   * @return The SQL clause, starting with a blank. Never <code>null</code> but maybe empty.
   * @since 8.8.0
   */
  @NonNull
  public static String getOrderByAndPagingClause (@NonNull final ISMPTableColumn <?> [] aColumns,
                                                  @NonNull final IPagingSpec aPagingSpec,
                                                  @Nullable final Set <String> aLobColumnNames)
  {
    return getOrderByAndPagingClause (SMPDataSourceSingleton.getDatabaseType (),
                                      aColumns,
                                      aPagingSpec,
                                      aLobColumnNames);
  }

  /**
   * Create the combined <code>ORDER BY</code> and paging clause for the provided paging
   * specification, for a table that contains LOB columns. See
   * {@link #getOrderByAndPagingClause(EDatabaseSystemType, ISMPTableColumn[], IPagingSpec)} for
   * the details.
   *
   * @param eDBType
   *        The database system to create the clause for. May not be <code>null</code>.
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @param aPagingSpec
   *        The paging specification to be applied. May not be <code>null</code>.
   * @param aLobColumnNames
   *        The SQL names of all columns that are LOB columns in at least one database system. May
   *        be <code>null</code>.
   * @return The SQL clause, starting with a blank. Never <code>null</code> but maybe empty.
   * @since 8.8.0
   */
  @NonNull
  public static String getOrderByAndPagingClause (@NonNull final EDatabaseSystemType eDBType,
                                                  @NonNull final ISMPTableColumn <?> [] aColumns,
                                                  @NonNull final IPagingSpec aPagingSpec,
                                                  @Nullable final Set <String> aLobColumnNames)
  {
    ValueEnforcer.notNull (aColumns, "Columns");

    return DBPagingHelper.getOrderByAndPagingClause (eDBType,
                                                     aPagingSpec,
                                                     createColumnNameResolver (eDBType, aColumns, aLobColumnNames),
                                                     TableColumnHelper.getAllDefaultSortFields (aColumns));
  }

  /**
   * Create the database specific SQL statement that returns one row if the provided table contains
   * at least one row, and no row otherwise.
   *
   * @param sTableName
   *        The name of the table to be checked. May neither be <code>null</code> nor empty.
   * @return The SQL statement. Never <code>null</code>.
   * @since 8.6.0
   */
  @NonNull
  public static String getExistsAnyRow (@NonNull @Nonempty final String sTableName)
  {
    return getExistsAnyRow (SMPDataSourceSingleton.getDatabaseType (), sTableName);
  }

  /**
   * Create the database specific SQL statement that returns one row if the provided table contains
   * at least one row, and no row otherwise. A plain <code>COUNT(*)</code> would be portable but
   * scans the whole table, which is wasteful if all that is asked is whether the table is empty.
   * <br>
   * The SQL standard <code>FETCH FIRST n ROWS ONLY</code> cannot be used for all systems: MySQL
   * does not support that syntax at all and needs <code>LIMIT</code>, and SQL Server rejects
   * <code>OFFSET</code>/<code>FETCH</code> unless the query also has an <code>ORDER BY</code>, so
   * it needs <code>TOP</code>.
   *
   * @param eDBType
   *        The database system to create the statement for. May not be <code>null</code>.
   * @param sTableName
   *        The name of the table to be checked. May neither be <code>null</code> nor empty.
   * @return The SQL statement. Never <code>null</code>.
   * @since 8.6.0
   */
  @NonNull
  public static String getExistsAnyRow (@NonNull final EDatabaseSystemType eDBType,
                                        @NonNull @Nonempty final String sTableName)
  {
    ValueEnforcer.notNull (eDBType, "DBType");
    ValueEnforcer.notEmpty (sTableName, "TableName");

    return switch (eDBType)
    {
      case MYSQL -> "SELECT 1 FROM " + sTableName + " LIMIT 1";
      case SQLSERVER -> "SELECT TOP 1 1 FROM " + sTableName;
      // DB2, Oracle and PostgreSQL support the SQL standard
      default -> "SELECT 1 FROM " + sTableName + " FETCH FIRST 1 ROW ONLY";
    };
  }

  /**
   * Create the database specific SQL expression that concatenates the provided SQL expressions to a
   * single string.
   *
   * @param aExpressions
   *        The SQL expressions to be concatenated. May neither be <code>null</code> nor empty.
   * @return The SQL expression. Never <code>null</code>.
   * @since 8.5.0
   */
  @NonNull
  public static String getStringConcat (@NonNull @Nonempty final String... aExpressions)
  {
    return getStringConcat (SMPDataSourceSingleton.getDatabaseType (), aExpressions);
  }

  /**
   * Create the database specific SQL expression that concatenates the provided SQL expressions to a
   * single string. MySQL cannot use the <code>||</code> operator, because it is the logical OR by
   * default, and SQL Server does not support it at all.
   *
   * @param eDBType
   *        The database system to create the expression for. May not be <code>null</code>.
   * @param aExpressions
   *        The SQL expressions to be concatenated. May neither be <code>null</code> nor empty.
   * @return The SQL expression. Never <code>null</code>.
   * @since 8.5.0
   */
  @NonNull
  public static String getStringConcat (@NonNull final EDatabaseSystemType eDBType,
                                        @NonNull @Nonempty final String... aExpressions)
  {
    ValueEnforcer.notNull (eDBType, "DBType");
    ValueEnforcer.notEmptyNoNullValue (aExpressions, "Expressions");

    if (aExpressions.length == 1)
      return aExpressions[0];

    switch (eDBType)
    {
      case MYSQL:
      case SQLSERVER:
        return "CONCAT(" + String.join (", ", aExpressions) + ")";
      default:
        return "(" + String.join (" || ", aExpressions) + ")";
    }
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
            aSB.append ("LOWER(")
               .append (sColumnName)
               .append (") LIKE ? ESCAPE '")
               .append (LIKE_ESCAPE_CHAR)
               .append ('\'');
            aParams.add (sLikePattern);
          }
      }

    if (aSB.length () == 0)
      return new SearchCondition ("", new CommonsArrayList <> ());

    return new SearchCondition ("(" + aSB + ")", aParams);
  }
}
