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

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.ui.secure.hc.HCSMPAccessPointSelect;
import com.helger.phoss.smp.ui.secure.hc.SMPAccessPointSelectHelper;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.core.execcontext.LayoutExecutionContext;

/**
 * Ajax executor that delivers a single "page" of Access Points for the Access Point select boxes of
 * the secure area. This avoids sending all Access Points to the browser.<br>
 * The response is in the format expected by "select2":
 *
 * <pre>
 * { "results": [ { "id": "...", "text": "..." } ], "pagination": { "more": false } }
 * </pre>
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public final class AjaxExecutorSecureAccessPointSelect extends AbstractSMPAjaxExecutor
{
  /** Name of the request parameter containing the search term */
  public static final String PARAM_SEARCH_TERM = "q";
  /** Name of the request parameter containing the 1-based page number */
  public static final String PARAM_PAGE = "page";

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
    int nPage = aLEC.params ().getAsInt (PARAM_PAGE, 1);
    if (nPage < 1)
      nPage = 1;

    final ICommonsList <ISMPAccessPoint> aList = SMPAccessPointSelectHelper.getPagePlusOne (StringHelper.isEmpty (sSearchText) ? null
                                                                                                                              : sSearchText,
                                                                                           nPage);
    final boolean bHasMore = aList.size () > SMPAccessPointSelectHelper.PAGE_SIZE;

    final JsonArray aResults = new JsonArray ();
    if (nPage == 1)
    {
      // Referencing an Access Point is opt-in per endpoint, so the "no Access Point" entry is
      // always present
      aResults.add (new JsonObject ().add (JSON_ID, HCSMPAccessPointSelect.VALUE_NONE)
                                     .add (JSON_TEXT, HCSMPAccessPointSelect.DISPLAY_NAME_NONE));
    }

    final int nMax = Math.min (aList.size (), SMPAccessPointSelectHelper.PAGE_SIZE);
    for (int i = 0; i < nMax; ++i)
    {
      final ISMPAccessPoint aAccessPoint = aList.get (i);
      aResults.add (new JsonObject ().add (JSON_ID, aAccessPoint.getID ())
                                     .add (JSON_TEXT, HCSMPAccessPointSelect.getDisplayName (aAccessPoint)));
    }

    aAjaxResponse.json (new JsonObject ().add (JSON_RESULTS, aResults)
                                         .add (JSON_PAGINATION, new JsonObject ().add (JSON_MORE, bHasMore)))
                 .disableCaching ();
  }
}
