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
package com.helger.phoss.smp.ui;

import java.text.NumberFormat;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.IPagingSpec;
import com.helger.collection.paging.PagingSpec;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.EHCFormMethod;
import com.helger.html.hc.html.forms.HCButton_Submit;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCForm;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.forms.HCSelect;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCLI;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.sections.HCNav;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.photon.bootstrap5.CBootstrapCSS;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.page.IWebPageExecutionContext;
import com.helger.url.param.URLParameter;
import com.helger.url.SimpleURL;

/**
 * Represents the state of the server side pagination of a single page. The current page index, the
 * current page size and the search text are taken from the request parameters, so that every page
 * can be bookmarked. Use this for the pages that are not driven by a DataTables, because DataTables
 * brings its own pagination UI.
 *
 * @author Philip Helger
 * @since 8.3.1
 */
public class SMPPagination
{
  /** Name of the request parameter with the 0-based index of the current page */
  public static final String PARAM_PAGE_INDEX = "pageindex";
  /** Name of the request parameter with the number of items per page */
  public static final String PARAM_PAGE_SIZE = "pagesize";
  /** Name of the request parameter with the search text to filter the items */
  public static final String PARAM_SEARCH_TEXT = "searchtext";

  /** The minimum allowed page size */
  public static final int MIN_PAGE_SIZE = 1;
  /** The maximum allowed page size */
  public static final int MAX_PAGE_SIZE = 10_000;
  /** The number of entries shown per page, if the request contains no explicit page size */
  public static final int DEFAULT_PAGE_SIZE = 20;

  /** The page sizes offered by the page size selector */
  private static final int [] PAGE_SIZES = { 20, 50, 100, 250 };

  private final Locale m_aDisplayLocale;
  private final SimpleURL m_aBaseURL;
  private final String m_sSearchText;
  private final long m_nFilteredCount;
  private final long m_nTotalCount;
  private final int m_nPageSize;
  private final int m_nPageIndex;

  /**
   * Constructor.
   *
   * @param aWPEC
   *        The web page execution context to retrieve the request parameters from. May not be
   *        <code>null</code>.
   * @param nFilteredCount
   *        The number of items to be paginated, taking the current search text into account. If the
   *        value is &lt; 0 (e.g. because the backend could not determine it) it is treated as 0.
   * @param nTotalCount
   *        The total number of items, ignoring the current search text. It is only used for the
   *        textual information on the shown items. If the value is &lt; 0 it is treated as 0.
   * @see #getSearchText(IWebPageExecutionContext)
   */
  public SMPPagination (@NonNull final IWebPageExecutionContext aWPEC,
                        final long nFilteredCount,
                        final long nTotalCount)
  {
    ValueEnforcer.notNull (aWPEC, "WPEC");

    m_aDisplayLocale = aWPEC.getDisplayLocale ();

    // Remove the pagination parameters from the base URL, because they are
    // added explicitly to each created link
    m_aBaseURL = new SimpleURL (aWPEC.getSelfHref ()).withParams (x -> x.removeIf (p -> p.hasName (PARAM_PAGE_INDEX) ||
                                                                                        p.hasName (PARAM_PAGE_SIZE) ||
                                                                                        p.hasName (PARAM_SEARCH_TEXT)));
    m_sSearchText = getSearchText (aWPEC);
    m_nFilteredCount = Math.max (nFilteredCount, 0);
    m_nTotalCount = Math.max (nTotalCount, 0);

    // Determine the page size
    int nPageSize = aWPEC.params ().getAsInt (PARAM_PAGE_SIZE, DEFAULT_PAGE_SIZE);
    if (nPageSize < MIN_PAGE_SIZE || nPageSize > MAX_PAGE_SIZE)
      nPageSize = DEFAULT_PAGE_SIZE;
    m_nPageSize = nPageSize;

    // Determine the page index - avoid an empty page in case the underlying
    // data changed in the meantime
    final int nPageCount = getPageCount ();
    int nPageIndex = aWPEC.params ().getAsInt (PARAM_PAGE_INDEX, 0);
    if (nPageIndex < 0)
      nPageIndex = 0;
    else
      if (nPageIndex >= nPageCount)
        nPageIndex = nPageCount - 1;
    m_nPageIndex = nPageIndex;
  }

  /**
   * @return The number of items matching the current search text. Always &ge; 0.
   */
  @Nonnegative
  public final long getFilteredCount ()
  {
    return m_nFilteredCount;
  }

  /**
   * @return The total number of items, ignoring the current search text. Always &ge; 0.
   */
  @Nonnegative
  public final long getTotalCount ()
  {
    return m_nTotalCount;
  }

  /**
   * @return The number of items per page. Always &gt; 0.
   */
  @Nonnegative
  public final int getPageSize ()
  {
    return m_nPageSize;
  }

  /**
   * @return The 0-based index of the current page. Always &ge; 0.
   */
  @Nonnegative
  public final int getPageIndex ()
  {
    return m_nPageIndex;
  }

  /**
   * @return The search text to filter the shown items. May be <code>null</code> or empty, if no
   *         filtering should take place.
   */
  @Nullable
  public final String getSearchText ()
  {
    return m_sSearchText;
  }

  /**
   * @return The total number of pages. Always &ge; 1.
   */
  @Nonnegative
  public final int getPageCount ()
  {
    if (m_nFilteredCount <= 0)
      return 1;
    return (int) Math.min ((m_nFilteredCount + m_nPageSize - 1) / m_nPageSize, Integer.MAX_VALUE);
  }

  /**
   * @return The paging specification matching the current page index and page size, without any
   *         sort field. Never <code>null</code>.
   */
  @NonNull
  public final IPagingSpec getPagingSpec ()
  {
    return PagingSpec.createForPage (m_nPageIndex, m_nPageSize);
  }

  /**
   * @return The 0-based index of the first item to be shown on the current page. Always &ge; 0.
   */
  @Nonnegative
  public final int getFirstItemIndex ()
  {
    return m_nPageIndex * m_nPageSize;
  }

  @NonNull
  private SimpleURL _getPageURL (@Nonnegative final int nPageIndex, @Nonnegative final int nPageSize)
  {
    final SimpleURL ret = _getPageURLNoSearch (nPageIndex, nPageSize);
    if (StringHelper.isNotEmpty (m_sSearchText))
      ret.add (PARAM_SEARCH_TEXT, m_sSearchText);
    return ret;
  }

  @NonNull
  private SimpleURL _getPageURLNoSearch (@Nonnegative final int nPageIndex, @Nonnegative final int nPageSize)
  {
    return new SimpleURL (m_aBaseURL).add (PARAM_PAGE_INDEX, nPageIndex)
                                     .add (PARAM_PAGE_SIZE, nPageSize);
  }

  @NonNull
  private HCLI _createItem (@NonNull final HCUL aUL,
                            @NonNull final String sText,
                            @Nullable final SimpleURL aTargetURL,
                            final boolean bActive)
  {
    final HCLI aLI = aUL.addAndReturnItem ((IHCNode) null).addClass (CBootstrapCSS.PAGE_ITEM);
    if (bActive)
      aLI.addClass (CBootstrapCSS.ACTIVE);
    if (aTargetURL == null)
    {
      aLI.addClass (CBootstrapCSS.DISABLED);
      aLI.addChild (new HCSpan ().addClass (CBootstrapCSS.PAGE_LINK).addChild (sText));
    }
    else
      aLI.addChild (new HCA (aTargetURL).addClass (CBootstrapCSS.PAGE_LINK).addChild (sText));
    return aLI;
  }

  /**
   * Get the search text of the current request. This is needed to determine the total number of
   * matching items, before this object can be created.
   *
   * @param aWPEC
   *        The web page execution context to retrieve the request parameter from. May not be
   *        <code>null</code>.
   * @return The search text to be used for filtering. May be <code>null</code> or empty.
   */
  @Nullable
  public static String getSearchText (@NonNull final IWebPageExecutionContext aWPEC)
  {
    ValueEnforcer.notNull (aWPEC, "WPEC");
    return StringHelper.trim (aWPEC.params ().getAsString (PARAM_SEARCH_TEXT));
  }

  @NonNull
  private String _getFormatted (final long nValue)
  {
    return NumberFormat.getIntegerInstance (m_aDisplayLocale).format (nValue);
  }

  /**
   * Create the UI to be placed <b>above</b> the paged content: the page size selector on the left
   * and the search field on the right, as DataTables does it. Because the paging and the filtering
   * happen on the server side, both controls live in a single GET form that resets the page index,
   * so that no JavaScript is needed - which is relevant, because the Content Security Policy of
   * this application does not allow inline event handlers.
   *
   * @return The created UI node. Never <code>null</code>.
   * @see #getFooterUI()
   */
  @NonNull
  public IHCNode getHeaderUI ()
  {
    final HCForm aForm = new HCForm (m_aBaseURL).setMethod (EHCFormMethod.GET)
                                                .addClasses (CBootstrapCSS.D_FLEX,
                                                             CBootstrapCSS.FLEX_WRAP,
                                                             CBootstrapCSS.JUSTIFY_CONTENT_BETWEEN,
                                                             CBootstrapCSS.ALIGN_ITEMS_CENTER,
                                                             CBootstrapCSS.GAP_2,
                                                             CBootstrapCSS.MB_2);

    // A GET form drops the query parameters of the action URL, so all existing parameters must be
    // added as hidden fields
    for (final URLParameter aParam : m_aBaseURL.params ())
      aForm.addChild (new HCHiddenField (aParam.getName (), aParam.getValue ()));

    // Always start with the first page again, because both the page size and the search text
    // change the offsets respectively the number of matching entries
    aForm.addChild (new HCHiddenField (PARAM_PAGE_INDEX, 0));

    // Left hand side: the number of entries per page
    {
      // The current page size may come from a bookmarked URL and not be one of the offered ones -
      // in that case it is added, so that the selection always shows the effective page size
      final ICommonsList <Integer> aPageSizes = new CommonsArrayList <> ();
      for (final int nPageSize : PAGE_SIZES)
        aPageSizes.add (Integer.valueOf (nPageSize));
      if (!aPageSizes.contains (Integer.valueOf (m_nPageSize)))
      {
        aPageSizes.add (Integer.valueOf (m_nPageSize));
        aPageSizes.sort (null);
      }

      final HCSelect aPageSizeSelect = new HCSelect (new RequestField (PARAM_PAGE_SIZE, m_nPageSize));
      aPageSizeSelect.addClasses (CBootstrapCSS.FORM_SELECT, CBootstrapCSS.FORM_SELECT_SM, CBootstrapCSS.W_AUTO);
      for (final Integer aPageSize : aPageSizes)
      {
        final String sPageSize = aPageSize.toString ();
        aPageSizeSelect.addOption (sPageSize, sPageSize);
      }
      aForm.addChild (new HCDiv ().addClasses (CBootstrapCSS.D_FLEX,
                                               CBootstrapCSS.ALIGN_ITEMS_CENTER,
                                               CBootstrapCSS.GAP_2)
                                  .addChild (aPageSizeSelect)
                                  .addChild (new HCSpan ().addChild ("entries per page")));
    }

    // Right hand side: the search
    {
      final HCDiv aSearch = new HCDiv ().addClasses (CBootstrapCSS.D_FLEX,
                                                     CBootstrapCSS.ALIGN_ITEMS_CENTER,
                                                     CBootstrapCSS.GAP_2);
      aSearch.addChild (new HCSpan ().addChild ("Search:"));
      aSearch.addChild (new HCEdit (PARAM_SEARCH_TEXT).setValue (m_sSearchText)
                                                      .setTitle ("Enter a search text and press Enter or use the button")
                                                      .addClasses (CBootstrapCSS.FORM_CONTROL,
                                                                   CBootstrapCSS.FORM_CONTROL_SM,
                                                                   CBootstrapCSS.W_AUTO));
      aSearch.addChild (new HCButton_Submit ("Apply").addClasses (CBootstrapCSS.BTN,
                                                                  CBootstrapCSS.BTN_OUTLINE_SECONDARY,
                                                                  CBootstrapCSS.BTN_SM));
      if (StringHelper.isNotEmpty (m_sSearchText))
        aSearch.addChild (new HCA (_getPageURLNoSearch (0, m_nPageSize)).addClasses (CBootstrapCSS.BTN,
                                                                                     CBootstrapCSS.BTN_OUTLINE_SECONDARY,
                                                                                     CBootstrapCSS.BTN_SM)
                                                                        .addChild ("Clear"));
      aForm.addChild (aSearch);
    }
    return aForm;
  }

  /**
   * Create the UI to be placed <b>below</b> the paged content: the information on the shown items
   * on the left and the page links on the right, as DataTables does it.
   *
   * @return The created UI node. Never <code>null</code>.
   * @see #getHeaderUI()
   */
  @NonNull
  public IHCNode getFooterUI ()
  {
    final HCDiv ret = new HCDiv ().addClasses (CBootstrapCSS.D_FLEX,
                                               CBootstrapCSS.FLEX_WRAP,
                                               CBootstrapCSS.JUSTIFY_CONTENT_BETWEEN,
                                               CBootstrapCSS.ALIGN_ITEMS_CENTER,
                                               CBootstrapCSS.GAP_2,
                                               CBootstrapCSS.MT_2);

    // Left hand side: the textual information on the currently shown items
    {
      final StringBuilder aInfo = new StringBuilder ();
      if (m_nFilteredCount == 0)
        aInfo.append ("Showing 0 to 0 of 0 entries");
      else
      {
        final long nFirst = getFirstItemIndex () + 1L;
        final long nLast = Math.min (nFirst + m_nPageSize - 1L, m_nFilteredCount);
        aInfo.append ("Showing ")
             .append (_getFormatted (nFirst))
             .append (" to ")
             .append (_getFormatted (nLast))
             .append (" of ")
             .append (_getFormatted (m_nFilteredCount))
             .append (" entries");
      }
      if (StringHelper.isNotEmpty (m_sSearchText))
        aInfo.append (" (filtered from ").append (_getFormatted (m_nTotalCount)).append (" total entries)");
      ret.addChild (new HCSpan ().addChild (aInfo.toString ()));
    }

    // Right hand side: the page links
    {
      final int nPageCount = getPageCount ();
      final HCUL aUL = new HCUL ().addClasses (CBootstrapCSS.PAGINATION, CBootstrapCSS.MB_0);

      // Previous page
      _createItem (aUL, "\u2039", m_nPageIndex > 0 ? _getPageURL (m_nPageIndex - 1, m_nPageSize) : null, false);

      // Show at most 9 page links around the current page
      final int nMaxLinks = 9;
      int nFirstPage = Math.max (m_nPageIndex - nMaxLinks / 2, 0);
      final int nLastPage = Math.min (nFirstPage + nMaxLinks - 1, nPageCount - 1);
      nFirstPage = Math.max (nLastPage - nMaxLinks + 1, 0);

      if (nFirstPage > 0)
        _createItem (aUL, "1", _getPageURL (0, m_nPageSize), false);
      if (nFirstPage > 1)
        _createItem (aUL, "\u2026", null, false);

      for (int i = nFirstPage; i <= nLastPage; ++i)
        _createItem (aUL, Integer.toString (i + 1), _getPageURL (i, m_nPageSize), i == m_nPageIndex);

      if (nLastPage < nPageCount - 2)
        _createItem (aUL, "\u2026", null, false);
      if (nLastPage < nPageCount - 1)
        _createItem (aUL, Integer.toString (nPageCount), _getPageURL (nPageCount - 1, m_nPageSize), false);

      // Next page
      _createItem (aUL,
                   "\u203a",
                   m_nPageIndex < nPageCount - 1 ? _getPageURL (m_nPageIndex + 1, m_nPageSize) : null,
                   false);

      ret.addChild (new HCNav ().addChild (aUL));
    }
    return ret;
  }
}
