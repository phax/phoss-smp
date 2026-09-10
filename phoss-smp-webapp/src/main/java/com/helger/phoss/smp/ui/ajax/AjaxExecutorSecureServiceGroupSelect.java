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
package com.helger.phoss.smp.ui.ajax;

import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.collection.paging.PagingSpec;
import com.helger.collection.paging.SortField;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupColumn;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.ui.cache.SMPOwnerNameCache;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.core.execcontext.LayoutExecutionContext;

/**
 * Ajax executor that delivers a single "page" of Service Groups for the Service Group select boxes
 * of the secure area. This avoids sending all Service Groups to the browser.<br>
 * The response is in the format expected by "select2":
 *
 * <pre>
 * { "results": [ { "id": "...", "text": "..." } ], "pagination": { "more": false } }
 * </pre>
 *
 * @author Philip Helger
 * @since 8.4.3
 */
public final class AjaxExecutorSecureServiceGroupSelect extends AbstractSMPAjaxExecutor
{
  /** Name of the request parameter containing the search term */
  public static final String PARAM_SEARCH_TERM = "q";
  /** Name of the request parameter containing the 1-based page number */
  public static final String PARAM_PAGE = "page";
  /** Name of the request parameter containing the optional filter ID */
  public static final String PARAM_FILTER = "filter";

  /** Filter ID: no filtering at all */
  public static final String FILTER_NONE = "none";
  /** Filter ID: only Service Groups that have no Business Card yet */
  public static final String FILTER_NO_BUSINESS_CARD = "nobc";

  /** The maximum number of Service Groups delivered per request */
  public static final int PAGE_SIZE = 25;

  public static final String JSON_RESULTS = "results";
  public static final String JSON_ID = "id";
  public static final String JSON_TEXT = "text";
  public static final String JSON_PAGINATION = "pagination";
  public static final String JSON_MORE = "more";

  /**
   * The number of entries read at once from the backend, in case a filter needs to be applied on
   * top of the backend paging.
   */
  private static final int SCAN_CHUNK_SIZE = 500;

  /**
   * Resolve a filter ID to the respective predicate.
   *
   * @param sFilterID
   *        The filter ID to be resolved. May be <code>null</code>.
   * @return <code>null</code> if no filtering should take place.
   */
  @Nullable
  public static Predicate <ISMPServiceGroup> getFilterPredicate (@Nullable final String sFilterID)
  {
    if (FILTER_NO_BUSINESS_CARD.equals (sFilterID))
    {
      final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
      if (aBusinessCardMgr == null)
        return null;

      // Show only Service Groups that don't have a Business Card already
      final ICommonsSet <String> aAllPIDsWithBusinessCards = aBusinessCardMgr.getAllSMPBusinessCardIDs ();
      return x -> !aAllPIDsWithBusinessCards.contains (x.getParticipantIdentifier ().getURIEncoded ());
    }
    return null;
  }

  /**
   * Get a single page of Service Groups, sorted by participant identifier ascending. To be able to
   * determine whether more entries are available, this method returns at most
   * <code>{@link #PAGE_SIZE} + 1</code> entries.
   *
   * @param sSearchText
   *        The search text to filter by. May be <code>null</code> or empty, in which case no
   *        filtering by text takes place.
   * @param nPage
   *        The 1-based number of the page to be returned. Must be &ge; 1.
   * @param aFilter
   *        An optional additional filter to be applied. May be <code>null</code>.
   * @return A list with at most <code>{@link #PAGE_SIZE} + 1</code> entries. Never
   *         <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <ISMPServiceGroup> getPagePlusOne (@Nullable final String sSearchText,
                                                                @Nonnegative final int nPage,
                                                                @Nullable final Predicate <ISMPServiceGroup> aFilter)
  {
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final SortField aSortField = SortField.ascending (ESMPServiceGroupColumn.PARTICIPANT_ID.getID ());
    final long nSkip = (long) (nPage - 1) * PAGE_SIZE;

    if (aFilter == null)
    {
      // The backend can do all the work
      return aServiceGroupMgr.getAllSMPServiceGroups (new PagingSpec (nSkip, PAGE_SIZE + 1L, aSortField), sSearchText);
    }

    // A filter is present - it can only be applied after the backend query, so all entries up to
    // the end of the requested page need to be scanned
    final long nNeeded = nSkip + PAGE_SIZE + 1L;
    final ICommonsList <ISMPServiceGroup> aMatching = new CommonsArrayList <> ();
    long nStartIndex = 0;
    while (aMatching.size () < nNeeded)
    {
      final ICommonsList <ISMPServiceGroup> aChunk = aServiceGroupMgr.getAllSMPServiceGroups (new PagingSpec (nStartIndex,
                                                                                                              SCAN_CHUNK_SIZE,
                                                                                                              aSortField),
                                                                                              sSearchText);
      for (final ISMPServiceGroup aServiceGroup : aChunk)
        if (aFilter.test (aServiceGroup))
          aMatching.add (aServiceGroup);

      if (aChunk.size () < SCAN_CHUNK_SIZE)
      {
        // Last chunk reached
        break;
      }
      nStartIndex += aChunk.size ();
    }

    final ICommonsList <ISMPServiceGroup> ret = new CommonsArrayList <> ();
    for (int i = (int) Math.min (nSkip, aMatching.size ()); i < aMatching.size () && ret.size () <= PAGE_SIZE; ++i)
      ret.add (aMatching.get (i));
    return ret;
  }

  /**
   * Check if at least one Service Group matching the provided filter is present.
   *
   * @param aFilter
   *        An optional filter to be applied. May be <code>null</code>.
   * @return <code>true</code> if at least one matching Service Group is available.
   */
  public static boolean containsAnyServiceGroup (@Nullable final Predicate <ISMPServiceGroup> aFilter)
  {
    return getPagePlusOne (null, 1, aFilter).isNotEmpty ();
  }

  @Override
  protected void mainHandleRequest (@NonNull final LayoutExecutionContext aLEC,
                                    @NonNull final PhotonUnifiedResponse aAjaxResponse) throws Exception
  {
    final String sSearchText = aLEC.params ().getAsStringTrimmed (PARAM_SEARCH_TERM);
    int nPage = aLEC.params ().getAsInt (PARAM_PAGE, 1);
    if (nPage < 1)
      nPage = 1;
    final String sFilterID = aLEC.params ().getAsStringTrimmed (PARAM_FILTER);

    final ICommonsList <ISMPServiceGroup> aList = getPagePlusOne (StringHelper.isEmpty (sSearchText) ? null
                                                                                                     : sSearchText,
                                                                  nPage,
                                                                  getFilterPredicate (sFilterID));
    final boolean bHasMore = aList.size () > PAGE_SIZE;

    // Use an owner name cache to avoid too many DB queries
    final SMPOwnerNameCache aOwnerNameCache = new SMPOwnerNameCache ();
    final JsonArray aResults = new JsonArray ();
    final int nMax = Math.min (aList.size (), PAGE_SIZE);
    for (int i = 0; i < nMax; ++i)
    {
      final ISMPServiceGroup aServiceGroup = aList.get (i);
      aResults.add (new JsonObject ().add (JSON_ID, aServiceGroup.getID ())
                                     .add (JSON_TEXT,
                                           aServiceGroup.getParticipantIdentifier ().getURIEncoded () +
                                                        " [" +
                                                        aOwnerNameCache.getFromCache (aServiceGroup.getOwnerID ()) +
                                                        "]"));
    }

    aAjaxResponse.json (new JsonObject ().add (JSON_RESULTS, aResults)
                                         .add (JSON_PAGINATION, new JsonObject ().add (JSON_MORE, bHasMore)));
  }
}
