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

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.html.jscode.JSAnonymousFunction;
import com.helger.html.jscode.JSAssocArray;
import com.helger.html.jscode.JSParam;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.ajax.AjaxExecutorSecureAccessPointSelect;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uictrls.select2.HCSelect2;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Select for all existing Access Points. The first entry is always the "no Access Point" entry,
 * because referencing an Access Point is an opt-in feature per endpoint.<br>
 * The Access Points are loaded on demand via Ajax in chunks of {@link SMPCommonUI#PAGE_SIZE}
 * elements, so that the select also works with a large number of Access Points.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public class HCSMPAccessPointSelect extends HCSelect2
{
  public static final String VALUE_NONE = "";
  public static final String DISPLAY_NAME_NONE = "- none - (use the data below)";

  @NonNull
  @Nonempty
  public static String getDisplayName (@NonNull final ISMPAccessPoint aAP)
  {
    return StringHelper.isNotEmpty (aAP.getEndpointReference ()) ? aAP.getName () +
                                                                   " (" +
                                                                   aAP.getEndpointReference () +
                                                                   ")" : aAP.getName ();
  }

  private final String m_sAjaxURL;

  public HCSMPAccessPointSelect (@NonNull final RequestField aRF,
                                 @NonNull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    super (aRF);

    m_sAjaxURL = CAjax.FUNCTION_ACCESS_POINT_SELECT.getInvocationURL (aRequestScope).getAsStringWithEncodedParameters ();

    // Only the "none" option and the currently selected Access Point are part of the initial HTML -
    // all other options are loaded on demand via Ajax
    addOption (VALUE_NONE, DISPLAY_NAME_NONE);

    final String sSelectedID = aRF.getRequestValue ();
    if (StringHelper.isNotEmpty (sSelectedID))
    {
      final ISMPAccessPoint aSelected = SMPMetaManager.getAccessPointMgr ().getAccessPointOfID (sSelectedID);
      if (aSelected != null)
        addOption (aSelected.getID (), getDisplayName (aSelected));
    }
  }

  @Override
  protected JSAssocArray getSelect2InvocationOptions ()
  {
    final JSAssocArray ret = super.getSelect2InvocationOptions ();
    final JSAssocArray aOptions = ret != null ? ret : new JSAssocArray ();

    // function (params) { return { q: params.term, page: params.page }; }
    final JSAnonymousFunction aDataFunc = new JSAnonymousFunction ();
    final JSParam aParams = aDataFunc.param ("params");
    aDataFunc.body ()
             ._return (new JSAssocArray ().add (AjaxExecutorSecureAccessPointSelect.PARAM_SEARCH_TEXT,
                                               aParams.ref ("term"))
                                         .add (AjaxExecutorSecureAccessPointSelect.PARAM_PAGE,
                                               aParams.ref ("page")));

    aOptions.add ("ajax",
                 new JSAssocArray ().add ("url", m_sAjaxURL)
                                    .add ("dataType", "json")
                                    .add ("delay", 250)
                                    .add ("cache", false)
                                    .add ("global", false)
                                    .add ("data", aDataFunc));
    return aOptions;
  }
}
