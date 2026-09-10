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
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.html.hc.html.forms.HCSelect;
import com.helger.html.jscode.JSAnonymousFunction;
import com.helger.html.jscode.JSAssocArray;
import com.helger.html.jscode.JSExpr;
import com.helger.html.jscode.JSParam;
import com.helger.html.request.IHCRequestField;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.ajax.AjaxExecutorSecureServiceGroupSelect;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.html.select.HCExtSelect;
import com.helger.photon.uictrls.select2.HCSelect2;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nullable;

/**
 * Select box for existing service groups.
 *
 * @author Philip Helger
 */
public class HCServiceGroupSelect extends HCExtSelect implements IHCServiceGroupSelect
{
  @NonNull
  @Nonempty
  public static String getDisplayName (@NonNull final ISMPServiceGroup aServiceGroup)
  {
    final String sOwnerName = SMPCommonUI.getOwnerName (aServiceGroup.getOwnerID ());
    return aServiceGroup.getParticipantIdentifier ().getURIEncoded () + " [" + sOwnerName + "]";
  }

  private static void _iterateMatchingSG (@Nullable final Predicate <? super ISMPServiceGroup> aIncludeFilter,
                                          @NonNull final Consumer <ISMPServiceGroup> aSGConsumer)
  {
    for (final ISMPServiceGroup aServiceGroup : SMPMetaManager.getServiceGroupMgr ()
                                                              .getAllSMPServiceGroups ()
                                                              .getSortedInline (ISMPServiceGroup.comparator ()))
      if (aIncludeFilter == null || aIncludeFilter.test (aServiceGroup))
        aSGConsumer.accept (aServiceGroup);
  }

  private HCServiceGroupSelect (@NonNull final RequestField aRF,
                                @NonNull final Locale aDisplayLocale,
                                @Nullable final Predicate <? super ISMPServiceGroup> aIncludeFilter)
  {
    super (aRF);

    _iterateMatchingSG (aIncludeFilter,
                        aServiceGroup -> addOption (aServiceGroup.getID (), getDisplayName (aServiceGroup)));

    if (!hasSelectedOption ())
      addOptionPleaseSelect (aDisplayLocale);
  }

  public boolean containsAnyServiceGroup ()
  {
    return containsEffectiveOption ();
  }

  private static class AjaxHCSelect2 extends HCSelect2 implements IHCServiceGroupSelect
  {
    public AjaxHCSelect2(@NonNull final IHCRequestField aRF)
    {
      super (aRF);
    }

    public boolean containsAnyServiceGroup ()
    {
      return containsEffectiveOption ();
    }
  }

  private static class ReadOnlyHSelect extends HCSelect implements IHCServiceGroupSelect
  {
    public ReadOnlyHSelect(@NonNull final IHCRequestField aRF)
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
   * {@link AjaxExecutorSecureServiceGroupSelect#PAGE_SIZE} entries per request.
   *
   * @author Philip Helger
   */
  private static final class HCAjaxSelect2 extends HCSelect2 implements IHCServiceGroupSelect
  {
    private final String m_sAjaxURL;
    private final String m_sFilterID;

    HCAjaxSelect2(@NonNull final IHCRequestField aRF, @NonNull final String sAjaxURL, @NonNull final String sFilterID)
    {
      super (aRF);
      m_sAjaxURL = sAjaxURL;
      m_sFilterID = sFilterID;
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
                                            .add (AjaxExecutorSecureServiceGroupSelect.PARAM_FILTER, m_sFilterID));

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
                                                         .add ("cache", true)
                                                         .add ("global", false)
                                                         .add ("data", aDataFunc)
                                                         .add ("processResults", aProcessFunc)
                                                         .add ("error", aErrorFunc))
                                .add ("minimumInputLength", 0);
    }

    public boolean containsAnyServiceGroup ()
    {
      return AjaxExecutorSecureServiceGroupSelect.containsAnyServiceGroup (AjaxExecutorSecureServiceGroupSelect.getFilterPredicate (m_sFilterID));
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
   * queried from the server in chunks of
   * {@link AjaxExecutorSecureServiceGroupSelect#PAGE_SIZE} entries, based on the text entered by
   * the user.
   *
   * @param aRequestScope
   *        The current request scope, needed to build the Ajax URL. May not be <code>null</code>.
   * @param aRF
   *        The request field to be used. May not be <code>null</code>.
   * @param aDisplayLocale
   *        The display locale to be used. May not be <code>null</code>.
   * @param sFilterID
   *        The ID of the server side filter to be applied. See the <code>FILTER_*</code> constants
   *        of {@link AjaxExecutorSecureServiceGroupSelect}. May not be <code>null</code>.
   * @param bReadOnly
   *        <code>true</code> if the select box should be read-only.
   * @return Never <code>null</code>.
   * @since 8.4.3
   */
  @NonNull
  public static IHCServiceGroupSelect createAjax (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                                  @NonNull final RequestField aRF,
                                                  @NonNull final Locale aDisplayLocale,
                                                  @NonNull final String sFilterID,
                                                  final boolean bReadOnly)
  {
    final ISMPServiceGroup aSelectedServiceGroup = _getSelectedServiceGroup (aRF.getRequestValue ());

    if (bReadOnly)
    {
      // Less HTML code
      // Using a simple read-only edit does not work, because it has no possibility to separate
      // display text and value
      // So we create a simple select with a single entry
      final ReadOnlyHSelect aSelect = new ReadOnlyHSelect(aRF);
      if (aSelectedServiceGroup != null)
        aSelect.addOption (aSelectedServiceGroup.getID (), getDisplayName (aSelectedServiceGroup));
      return aSelect;
    }

    final HCAjaxSelect2 aSelect2 = new HCAjaxSelect2(aRF,
                                                      CAjax.FUNCTION_SERVICE_GROUP_SELECT.getInvocationURI (aRequestScope),
                                                      sFilterID);
    // Only add the currently selected option - all others are loaded on demand
    if (aSelectedServiceGroup != null)
      aSelect2.addOption (aSelectedServiceGroup.getID (), getDisplayName (aSelectedServiceGroup));
    else
      aSelect2.addOptionPleaseSelect (aDisplayLocale);
    return aSelect2;
  }

  @NonNull
  public static IHCServiceGroupSelect create (@NonNull final RequestField aRF,
                                              @NonNull final Locale aDisplayLocale,
                                              @Nullable final Predicate <? super ISMPServiceGroup> aIncludeFilter,
                                              final boolean bReadOnly)
  {
    if (true)
    {
      if (bReadOnly)
      {
        // Less HTML code
        // Using a simple read-only edit does not work, because it has no possibility to separate
        // display text and value
        // So we create a simple select with a single entry
        final IParticipantIdentifier aSelectedPID = SMPMetaManager.getIdentifierFactory ()
                                                                  .parseParticipantIdentifier (aRF.getRequestValue ());
        final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ()
                                                             .getSMPServiceGroupOfID (aSelectedPID);
        final ReadOnlyHSelect aSelect = new ReadOnlyHSelect(aRF);
        if (aServiceGroup != null)
          aSelect.addOption (aServiceGroup.getID (), getDisplayName (aServiceGroup));
        return aSelect;
      }

      final AjaxHCSelect2 aSelect2 = new AjaxHCSelect2(aRF);
      _iterateMatchingSG (aIncludeFilter,
                          aServiceGroup -> aSelect2.addOption (aServiceGroup.getID (), getDisplayName (aServiceGroup)));
      return aSelect2;
    }

    final HCServiceGroupSelect ret = new HCServiceGroupSelect (aRF, aDisplayLocale, aIncludeFilter);
    ret.setReadOnly (bReadOnly);
    return ret;
  }
}
