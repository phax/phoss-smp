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

import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.ICommonsList;
import com.helger.photon.core.paging.ITableColumn;

/**
 * Adds the mapping onto the underlying data stores to an {@link ITableColumn}. It is the single
 * source of truth that ties the column shown in the UI to the SQL column, to the MongoDB field and
 * to an in-memory {@link Comparator}, so that these four cannot drift apart. It is implemented as an
 * enum per domain object.
 *
 * @author Philip Helger
 * @param <DATATYPE>
 *        The domain object type this column belongs to
 * @since 8.2.1
 */
public interface ISMPTableColumn <DATATYPE> extends ITableColumn <DATATYPE>
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
}
