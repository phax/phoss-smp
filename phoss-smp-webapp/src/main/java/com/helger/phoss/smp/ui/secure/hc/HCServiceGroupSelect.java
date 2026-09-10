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

import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.html.hc.html.forms.HCSelect;
import com.helger.html.jscode.JSAnonymousFunction;
import com.helger.html.jscode.JSAssocArray;
import com.helger.html.jscode.JSExpr;
import com.helger.html.jscode.JSParam;
import com.helger.html.request.IHCRequestField;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupFilter;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.ajax.AjaxExecutorSecureServiceGroupSelect;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uictrls.select2.HCSelect2;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nullable;

/**
 * Factory for the select boxes of existing Service Groups.
 *
 * @author Philip Helger
 */
@Immutable
public final class HCServiceGroupSelect
{
  private HCServiceGroupSelect ()
  {}

  @NonNull
  @Nonempty
  public static String getDisplayName (@NonNull final ISMPServiceGroup aServiceGroup)
  {
    return getDisplayName (aServiceGroup, SMPCommonUI.getOwnerName (aServiceGroup.getOwnerID ()));
  }

  /**
   * Get the display name of the provided Service Group, based on an already resolved owner name.
   *
   * @param aServiceGroup
   *        The Service Group to be displayed. May not be <code>null</code>.
   * @param sOwnerName
   *        The name of the owner of the Service Group. May not be <code>null</code>.
   * @return Neither <code>null</code> nor empty.
   * @since 8.4.3
   */
  @NonNull
  @Nonempty
  public static String getDisplayName (@NonNull final ISMPServiceGroup aServiceGroup, @NonNull final String sOwnerName)
  {
    return aServiceGroup.getParticipantIdentifier ().getURIEncoded () + " [" + sOwnerName + "]";
  }

  /**
   * A read-only select box, containing the currently selected Service Group only.
   *
   * @author Philip Helger
   */
  private static final class HCReadOnlyServiceGroupSelect extends HCSelect implements IHCServiceGroupSelect
  {
    HCReadOnlyServiceGroupSelect (@NonNull final IHCRequestField aRF)
    {
      super (aRF);
      setReadOnly (true);
    }

    public boolean containsAnyServiceGroup ()
    {
      return true;
    }
  }

  /**
   * A select box that loads the Service Groups on demand via Ajax, delivering at most
   * {@link SMPServiceGroupSelectHelper#PAGE_SIZE} entries per request.
   *
   * @author Philip Helger
   */
  private static final class HCAjaxServiceGroupSelect2 extends HCSelect2 implements IHCServiceGroupSelect
  {
    private final String m_sAjaxURL;
    private final ESMPServiceGroupFilter m_eFilter;

    HCAjaxServiceGroupSelect2 (@NonNull final IHCRequestField aRF,
                               @NonNull final String sAjaxURL,
                               @NonNull final ESMPServiceGroupFilter eFilter)
    {
      super (aRF);
      m_sAjaxURL = sAjaxURL;
      m_eFilter = eFilter;
    }

    @Override
    @NonNull
    protected JSAssocArray getSelect2InvocationOptions ()
    {
      // function (params) { return { q: params.term, page: params.page || 1, filter: '...' }; }
      final JSAnonymousFunction aDataFunc = new JSAnonymousFunction ();
      final JSParam aDataParams = aDataFunc.param ("params");
      aDataFunc.body ()
               ._return (new JSAssocArray ().add (AjaxExecutorSecureServiceGroupSelect.PARAM_SEARCH_TERM,
                                                  aDataParams.ref ("term"))
                                            .add (AjaxExecutorSecureServiceGroupSelect.PARAM_PAGE,
                                                  aDataParams.ref ("page").cor (JSExpr.lit (1)))
                                            .add (AjaxExecutorSecureServiceGroupSelect.PARAM_FILTER,
                                                  m_eFilter.getID ()));

      // function (data, params) { params.page = params.page || 1; return data; }
      final JSAnonymousFunction aProcessFunc = new JSAnonymousFunction ();
      final JSParam aProcessData = aProcessFunc.param ("data");
      final JSParam aProcessParams = aProcessFunc.param ("params");
      aProcessFunc.body ().assign (aProcessParams.ref ("page"), aProcessParams.ref ("page").cor (JSExpr.lit (1)));
      aProcessFunc.body ()._return (aProcessData);

      // Select2 aborts pending requests whenever the user continues typing. Such an abort would
      // trigger the global jQuery "ajaxError" handler of ph-oton, that shows a JS alert. Therefore
      // the global handlers are disabled for this request, and errors are logged to the console
      // instead.
      // function (jqXHR, textStatus) { if (textStatus !== 'abort' && window.console) console.error
      // ('...' + textStatus); }
      final JSAnonymousFunction aErrorFunc = new JSAnonymousFunction ();
      final JSParam aErrorXHR = aErrorFunc.param ("jqXHR");
      final JSParam aErrorTextStatus = aErrorFunc.param ("textStatus");
      aErrorFunc.body ()
                ._if (aErrorTextStatus.ene ("abort").cand (JSExpr.ref ("window").ref ("console")),
                      JSExpr.ref ("console")
                            .invoke ("error")
                            .arg (JSExpr.lit ("Failed to load the Service Groups: ")
                                        .plus (aErrorXHR.ref ("status"))
                                        .plus (JSExpr.lit (" "))
                                        .plus (aErrorTextStatus)));

      return new JSAssocArray ().add ("ajax",
                                      new JSAssocArray ().add ("url", m_sAjaxURL)
                                                         .add ("dataType", "json")
                                                         .add ("delay", 250)
                                                         .add ("global", false)
                                                         .add ("data", aDataFunc)
                                                         .add ("processResults", aProcessFunc)
                                                         .add ("error", aErrorFunc))
                                .add ("minimumInputLength", 0);
    }

    public boolean containsAnyServiceGroup ()
    {
      return SMPServiceGroupSelectHelper.containsAnyServiceGroup (m_eFilter);
    }
  }

  @Nullable
  private static ISMPServiceGroup _getSelectedServiceGroup (@Nullable final String sSelectedID)
  {
    if (StringHelper.isEmpty (sSelectedID))
      return null;

    final IParticipantIdentifier aSelectedPID = SMPMetaManager.getIdentifierFactory ()
                                                              .parseParticipantIdentifier (sSelectedID);
    if (aSelectedPID == null)
      return null;

    return SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aSelectedPID);
  }

  /**
   * Create a Service Group select box that loads its entries on demand via Ajax. Only the currently
   * selected Service Group (if any) is contained in the created HTML - all other entries are
   * queried from the server in chunks of {@link SMPServiceGroupSelectHelper#PAGE_SIZE} entries,
   * based on the text entered by the user.
   *
   * @param aRequestScope
   *        The current request scope, needed to build the Ajax URL. May not be <code>null</code>.
   * @param aRF
   *        The request field to be used. May not be <code>null</code>.
   * @param aDisplayLocale
   *        The display locale to be used. May not be <code>null</code>.
   * @param eFilter
   *        The server side filter to be applied. May not be <code>null</code>.
   * @param bReadOnly
   *        <code>true</code> if the select box should be read-only.
   * @return Never <code>null</code>.
   * @since 8.4.3
   */
  @NonNull
  public static IHCServiceGroupSelect createAjax (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                                  @NonNull final RequestField aRF,
                                                  @NonNull final Locale aDisplayLocale,
                                                  @NonNull final ESMPServiceGroupFilter eFilter,
                                                  final boolean bReadOnly)
  {
    final ISMPServiceGroup aSelectedServiceGroup = _getSelectedServiceGroup (aRF.getRequestValue ());

    if (bReadOnly)
    {
      // Less HTML code
      // Using a simple read-only edit does not work, because it has no possibility to separate
      // display text and value
      // So we create a simple select with a single entry
      final HCReadOnlyServiceGroupSelect aSelect = new HCReadOnlyServiceGroupSelect (aRF);
      if (aSelectedServiceGroup != null)
        aSelect.addOption (aSelectedServiceGroup.getID (), getDisplayName (aSelectedServiceGroup));
      return aSelect;
    }

    final HCAjaxServiceGroupSelect2 aSelect2 = new HCAjaxServiceGroupSelect2 (aRF,
                                                                             CAjax.FUNCTION_SERVICE_GROUP_SELECT.getInvocationURI (aRequestScope),
                                                                             eFilter);
    // Only add the currently selected option - all others are loaded on demand
    if (aSelectedServiceGroup != null)
      aSelect2.addOption (aSelectedServiceGroup.getID (), getDisplayName (aSelectedServiceGroup));
    else
      aSelect2.addOptionPleaseSelect (aDisplayLocale);
    return aSelect2;
  }
}
