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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupFilter;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.ui.cache.SMPOwnerNameCache;
import com.helger.phoss.smp.ui.secure.hc.HCServiceGroupSelect;
import com.helger.phoss.smp.ui.secure.hc.SMPServiceGroupSelectHelper;
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
 * @since 8.5.0
 */
public final class AjaxExecutorSecureServiceGroupSelect extends AbstractSMPAjaxExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AjaxExecutorSecureServiceGroupSelect.class);

  /** Name of the request parameter containing the search term */
  public static final String PARAM_SEARCH_TERM = "q";
  /** Name of the request parameter containing the 1-based page number */
  public static final String PARAM_PAGE = "page";
  /** Name of the request parameter containing the optional filter ID */
  public static final String PARAM_FILTER = "filter";

  public static final String JSON_RESULTS = "results";
  public static final String JSON_ID = "id";
  public static final String JSON_TEXT = "text";
  public static final String JSON_PAGINATION = "pagination";
  public static final String JSON_MORE = "more";

  @Override
  protected void mainHandleRequest (@NonNull final LayoutExecutionContext aLEC,
                                    @NonNull final PhotonUnifiedResponse aAjaxResponse) throws Exception
  {
    final String sSearchText = aLEC.params ().getAsStringTrimmed (PARAM_SEARCH_TERM);
    // select2 uses 1-based page numbers
    final long nPage = Math.max (aLEC.params ().getAsLong (PARAM_PAGE, 1), 1);

    // The filter is provided by a client, so an unknown one must not silently widen the result set
    final String sFilterID = aLEC.params ().getAsStringTrimmed (PARAM_FILTER);
    final ESMPServiceGroupFilter eFilter = ESMPServiceGroupFilter.getFromIDOrNull (sFilterID);
    if (eFilter == null)
    {
      LOGGER.warn ("The provided Service Group filter '" + sFilterID + "' is unknown");
      aAjaxResponse.createBadRequest ();
      return;
    }

    final ICommonsList <ISMPServiceGroup> aList = SMPServiceGroupSelectHelper.getPagePlusOne (eFilter,
                                                                                              StringHelper.isEmpty (sSearchText) ? null
                                                                                                                                 : sSearchText,
                                                                                              nPage);
    final boolean bHasMore = aList.size () > SMPServiceGroupSelectHelper.PAGE_SIZE;

    // Use an owner name cache, so that each owner is only resolved once
    final SMPOwnerNameCache aOwnerNameCache = new SMPOwnerNameCache ();
    final JsonArray aResults = new JsonArray ();
    final int nMax = Math.min (aList.size (), SMPServiceGroupSelectHelper.PAGE_SIZE);
    for (int i = 0; i < nMax; ++i)
    {
      final ISMPServiceGroup aServiceGroup = aList.get (i);
      aResults.add (new JsonObject ().add (JSON_ID, aServiceGroup.getID ())
                                     .add (JSON_TEXT,
                                           HCServiceGroupSelect.getDisplayName (aServiceGroup,
                                                                                aOwnerNameCache.getFromCache (aServiceGroup.getOwnerID ()))));
    }

    aAjaxResponse.json (new JsonObject ().add (JSON_RESULTS, aResults)
                                         .add (JSON_PAGINATION, new JsonObject ().add (JSON_MORE, bHasMore)));
  }
}
