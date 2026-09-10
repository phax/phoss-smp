/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.ui.secure.hc;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.PagingSpec;
import com.helger.collection.paging.SortField;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupColumn;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupFilter;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;

/**
 * Helper class that queries the Service Groups for the Service Group select boxes of the secure
 * area. All queries are executed by the backend, so that never more Service Groups than needed are
 * read.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
@Immutable
public final class SMPServiceGroupSelectHelper
{
  /** The maximum number of Service Groups delivered per request */
  public static final int PAGE_SIZE = 25;

  private SMPServiceGroupSelectHelper ()
  {}

  /**
   * Get a single page of Service Groups, sorted by participant identifier ascending. To be able to
   * determine whether more entries are available, this method returns at most
   * <code>{@link #PAGE_SIZE} + 1</code> entries.
   *
   * @param eFilter
   *        The server side filter to be applied. May not be <code>null</code>.
   * @param sSearchText
   *        The search text to filter by. May be <code>null</code> or empty, in which case no
   *        filtering by text takes place.
   * @param nPage
   *        The 1-based number of the page to be returned. Must be &ge; 1.
   * @return A list with at most <code>{@link #PAGE_SIZE} + 1</code> entries. Never
   *         <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <ISMPServiceGroup> getPagePlusOne (@NonNull final ESMPServiceGroupFilter eFilter,
                                                                @Nullable final String sSearchText,
                                                                @Nonnegative final int nPage)
  {
    final SortField aSortField = SortField.ascending (ESMPServiceGroupColumn.PARTICIPANT_ID.getID ());
    final long nSkip = (long) (nPage - 1) * PAGE_SIZE;
    return SMPMetaManager.getServiceGroupMgr ()
                         .getAllSMPServiceGroups (eFilter,
                                                  new PagingSpec (nSkip, PAGE_SIZE + 1L, aSortField),
                                                  sSearchText);
  }

  /**
   * Check if at least one Service Group matching the provided filter is present.
   *
   * @param eFilter
   *        The server side filter to be applied. May not be <code>null</code>.
   * @return <code>true</code> if at least one matching Service Group is available.
   */
  public static boolean containsAnyServiceGroup (@NonNull final ESMPServiceGroupFilter eFilter)
  {
    return SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupCount (eFilter, null) > 0;
  }
}
