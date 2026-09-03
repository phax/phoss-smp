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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.compare.ESortOrder;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;

/**
 * A single resolved sort instruction: the column to sort by and the sort order to be applied to it.
 *
 * @author Philip Helger
 * @param <DATATYPE>
 *        The domain object type this column belongs to
 * @since 8.2.1
 */
@Immutable
public class SMPSortColumn <DATATYPE>
{
  private final ISMPTableColumn <DATATYPE> m_aColumn;
  private final ESortOrder m_eSortOrder;

  public SMPSortColumn (@NonNull final ISMPTableColumn <DATATYPE> aColumn, @NonNull final ESortOrder eSortOrder)
  {
    m_aColumn = ValueEnforcer.notNull (aColumn, "Column");
    m_eSortOrder = ValueEnforcer.notNull (eSortOrder, "SortOrder");
  }

  @NonNull
  public ISMPTableColumn <DATATYPE> getColumn ()
  {
    return m_aColumn;
  }

  @NonNull
  public ESortOrder getSortOrder ()
  {
    return m_eSortOrder;
  }

  public boolean isAscending ()
  {
    return m_eSortOrder.isAscending ();
  }

  /**
   * @return The comparator of the contained column, reversed if the sort order is descending. Never
   *         <code>null</code>.
   */
  @NonNull
  public Comparator <DATATYPE> getComparator ()
  {
    final Comparator <DATATYPE> ret = m_aColumn.getComparator ();
    return isAscending () ? ret : ret.reversed ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Column", m_aColumn).append ("SortOrder", m_eSortOrder).getToString ();
  }
}
