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
import com.helger.collection.paging.PagingSpec;
import com.helger.json.IJsonArray;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.secure.hc.HCSMPAccessPointSelect;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Ajax executor that provides a single chunk of Access Points for the Access Point select in the
 * endpoint UI. Never more than {@link SMPCommonUI#PAGE_SIZE} Access Points are returned per
 * invocation, so that the select works with an arbitrary number of Access Points.<br>
 * The response uses the JSON format expected by Select2:
 * <code>{ "results": [ { "id": ..., "text": ... } ], "pagination": { "more": true|false } }</code>
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public final class AjaxExecutorSecureAccessPointSelect extends AbstractSMPAjaxExecutor
{
  /** The name of the request parameter containing the search text */
  public static final String PARAM_SEARCH_TEXT = "q";
  /** The name of the request parameter containing the 1-based page number */
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
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
    final String sSearchText = StringHelper.trim (aRequestScope.params ().getAsString (PARAM_SEARCH_TEXT));

    // Select2 uses 1-based page numbers
    int nPage = aRequestScope.params ().getAsInt (PARAM_PAGE, 1);
    if (nPage < 1)
      nPage = 1;

    final int nPageSize = SMPCommonUI.PAGE_SIZE;
    final ISMPAccessPointManager aAccessPointMgr = SMPMetaManager.getAccessPointMgr ();

    final IJsonArray aResults = new JsonArray ();
    if (nPage == 1)
    {
      // The "no Access Point" entry is always present
      aResults.add (new JsonObject ().add (JSON_ID, HCSMPAccessPointSelect.VALUE_NONE)
                                     .add (JSON_TEXT, HCSMPAccessPointSelect.DISPLAY_NAME_NONE));
    }

    // Only load the requested chunk from the backend
    for (final ISMPAccessPoint aAccessPoint : aAccessPointMgr.getAllAccessPoints (new PagingSpec ((nPage - 1) *
                                                                                                  (long) nPageSize,
                                                                                                  nPageSize),
                                                                                  sSearchText))
    {
      aResults.add (new JsonObject ().add (JSON_ID, aAccessPoint.getID ())
                                     .add (JSON_TEXT, HCSMPAccessPointSelect.getDisplayName (aAccessPoint)));
    }

    final long nMatchingCount = aAccessPointMgr.getAccessPointCount (sSearchText);
    final boolean bMore = nMatchingCount > (long) nPage * nPageSize;

    aAjaxResponse.json (new JsonObject ().add (JSON_RESULTS, aResults)
                                         .add (JSON_PAGINATION, new JsonObject ().add (JSON_MORE, bMore)))
                 .disableCaching ();
  }
}
