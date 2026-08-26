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
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * Helper class for the server side pagination of manager queries. It contains the fallback
 * implementation that is used, if a backend does not support native paging.
 *
 * @author Philip Helger
 * @since 8.2.1
 */
@Immutable
public final class SMPPagingHelper
{
  private SMPPagingHelper ()
  {}

  /**
   * Extract a single page out of the provided list. The list is sorted with the provided comparator
   * first, to ensure a stable order across single page requests.
   *
   * @param <T>
   *        The list element type
   * @param aList
   *        The list to take the page from. May not be <code>null</code>. The list is sorted in
   *        place.
   * @param aComparator
   *        The comparator to be used for sorting. May not be <code>null</code>.
   * @param nStartIndex
   *        The 0-based index of the first element to be returned. Must be &ge; 0.
   * @param nMaxCount
   *        The maximum number of elements to be returned. Must be &ge; 0.
   * @return A non-<code>null</code> but maybe empty list.
   */
  @NonNull
  @ReturnsMutableCopy
  public static <T> ICommonsList <T> getPage (@NonNull final ICommonsList <T> aList,
                                              @NonNull final Comparator <? super T> aComparator,
                                              @Nonnegative final int nStartIndex,
                                              @Nonnegative final int nMaxCount)
  {
    checkPagingParams (nStartIndex, nMaxCount);

    if (nMaxCount == 0 || nStartIndex >= aList.size ())
      return new CommonsArrayList <> ();

    aList.sort (aComparator);

    final int nEndIndex = (int) Math.min ((long) nStartIndex + nMaxCount, aList.size ());
    final List <T> aSubList = aList.subList (nStartIndex, nEndIndex);
    return new CommonsArrayList <> (aSubList);
  }

  /**
   * Check if the provided value contains the provided search text, ignoring case.
   *
   * @param sValue
   *        The value to be checked. May be <code>null</code>.
   * @param sSearchText
   *        The search text to be searched. May be <code>null</code> in which case
   *        <code>true</code> is returned.
   * @return <code>true</code> if the search text is empty or if the value contains the search text.
   */
  public static boolean matchesSearchText (@Nullable final String sValue, @Nullable final String sSearchText)
  {
    if (StringHelper.isEmpty (sSearchText))
      return true;
    if (StringHelper.isEmpty (sValue))
      return false;
    return sValue.toLowerCase (Locale.ROOT).contains (sSearchText.toLowerCase (Locale.ROOT));
  }

  /**
   * Check if the provided paging parameters are valid.
   *
   * @param nStartIndex
   *        The 0-based index of the first element to be returned. Must be &ge; 0.
   * @param nMaxCount
   *        The maximum number of elements to be returned. Must be &ge; 0.
   * @throws IllegalArgumentException
   *         If one of the parameters is &lt; 0.
   */
  public static void checkPagingParams (@Nonnegative final int nStartIndex, @Nonnegative final int nMaxCount)
  {
    if (nStartIndex < 0)
      throw new IllegalArgumentException ("The start index must be >= 0 but is " + nStartIndex);
    if (nMaxCount < 0)
      throw new IllegalArgumentException ("The maximum count must be >= 0 but is " + nMaxCount);
  }
}
