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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.EHCFormMethod;
import com.helger.html.hc.html.forms.HCButton_Submit;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCForm;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.grouping.HCLI;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.sections.HCNav;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.photon.uicore.page.IWebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.url.param.URLParameter;
import com.helger.url.SimpleURL;

/**
 * Represents the state of the server side pagination of a single list page. The current page index
 * and the current page size are taken from the request parameters, so that every page can be
 * bookmarked.
 *
 * @author Philip Helger
 * @since 8.2.1
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

  private static final int [] PAGE_SIZES = { 25, 50, 100, 250, 1_000 };
  private static final ICSSClassProvider CSS_CLASS_PAGINATION = DefaultCSSClassProvider.create ("pagination");
  private static final ICSSClassProvider CSS_CLASS_PAGE_ITEM = DefaultCSSClassProvider.create ("page-item");
  private static final ICSSClassProvider CSS_CLASS_PAGE_LINK = DefaultCSSClassProvider.create ("page-link");
  private static final ICSSClassProvider CSS_CLASS_ACTIVE = DefaultCSSClassProvider.create ("active");
  private static final ICSSClassProvider CSS_CLASS_DISABLED = DefaultCSSClassProvider.create ("disabled");
  private static final ICSSClassProvider CSS_CLASS_D_FLEX = DefaultCSSClassProvider.create ("d-flex");
  private static final ICSSClassProvider CSS_CLASS_ALIGN_ITEMS_CENTER = DefaultCSSClassProvider.create ("align-items-center");
  private static final ICSSClassProvider CSS_CLASS_GAP_2 = DefaultCSSClassProvider.create ("gap-2");
  private static final ICSSClassProvider CSS_CLASS_MB_2 = DefaultCSSClassProvider.create ("mb-2");
  private static final ICSSClassProvider CSS_CLASS_FORM_CONTROL = DefaultCSSClassProvider.create ("form-control");
  private static final ICSSClassProvider CSS_CLASS_W_AUTO = DefaultCSSClassProvider.create ("w-auto");
  private static final ICSSClassProvider CSS_CLASS_BTN = DefaultCSSClassProvider.create ("btn");
  private static final ICSSClassProvider CSS_CLASS_BTN_SECONDARY = DefaultCSSClassProvider.create ("btn-secondary");
  private static final ICSSClassProvider CSS_CLASS_ME_3 = DefaultCSSClassProvider.create ("me-3");

  private final SimpleURL m_aBaseURL;
  private final String m_sSearchText;
  private final long m_nTotalCount;
  private final int m_nPageSize;
  private final int m_nPageIndex;

  /**
   * Constructor.
   *
   * @param aWPEC
   *        The web page execution context to retrieve the request parameters from. May not be
   *        <code>null</code>.
   * @param nTotalCount
   *        The total number of items to be paginated, taking the current search text into account.
   *        If the value is &lt; 0 (e.g. because the backend could not determine it) it is treated
   *        as 0.
   * @see #getSearchText(IWebPageExecutionContext)
   */
  public SMPPagination (@NonNull final IWebPageExecutionContext aWPEC, final long nTotalCount)
  {
    ValueEnforcer.notNull (aWPEC, "WPEC");

    // Remove the pagination parameters from the base URL, because they are
    // added explicitly to each created link
    m_aBaseURL = new SimpleURL (aWPEC.getSelfHref ()).withParams (x -> x.removeIf (p -> p.hasName (PARAM_PAGE_INDEX) ||
                                                                                        p.hasName (PARAM_PAGE_SIZE) ||
                                                                                        p.hasName (PARAM_SEARCH_TEXT)));
    m_sSearchText = getSearchText (aWPEC);
    m_nTotalCount = Math.max (nTotalCount, 0);

    // Determine the page size
    final int nDefaultPageSize = SMPWebAppConfiguration.getPaginationPageSize ();
    int nPageSize = aWPEC.params ().getAsInt (PARAM_PAGE_SIZE, nDefaultPageSize);
    if (nPageSize < MIN_PAGE_SIZE || nPageSize > MAX_PAGE_SIZE)
      nPageSize = nDefaultPageSize;
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
   * @return The total number of items. Always &ge; 0.
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
    if (m_nTotalCount <= 0)
      return 1;
    return (int) Math.min ((m_nTotalCount + m_nPageSize - 1) / m_nPageSize, Integer.MAX_VALUE);
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
    final HCLI aLI = aUL.addAndReturnItem ((IHCNode) null).addClass (CSS_CLASS_PAGE_ITEM);
    if (bActive)
      aLI.addClass (CSS_CLASS_ACTIVE);
    if (aTargetURL == null)
    {
      aLI.addClass (CSS_CLASS_DISABLED);
      aLI.addChild (new HCSpan ().addClass (CSS_CLASS_PAGE_LINK).addChild (sText));
    }
    else
      aLI.addChild (new HCA (aTargetURL).addClass (CSS_CLASS_PAGE_LINK).addChild (sText));
    return aLI;
  }

  /**
   * Align the provided DataTables with the server side pagination: all client side controls that
   * are provided by the server side pagination as well (paging, page length selection, the
   * information on the shown entries and the search field) are disabled, so that they are not shown
   * twice and so that they don't only work on the entries of the current page.
   *
   * @param aDataTables
   *        The DataTables to be modified. May not be <code>null</code>.
   * @return The provided DataTables for chaining. Never <code>null</code>.
   */
  @NonNull
  public DataTables applyTo (@NonNull final DataTables aDataTables)
  {
    ValueEnforcer.notNull (aDataTables, "DataTables");
    return aDataTables.setPageLength (m_nPageSize)
                      .setPaging (false)
                      .setLengthChange (false)
                      .setInfo (false)
                      .setSearching (false);
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

  /**
   * Create the search field. Because the filtering happens on the server side, a simple GET form is
   * used, that resets the page index, because the number of matching entries changes.
   *
   * @return The created UI node. Never <code>null</code>.
   */
  @NonNull
  private IHCNode _createSearchUI ()
  {
    final HCForm aForm = new HCForm (m_aBaseURL).setMethod (EHCFormMethod.GET)
                                                .addClasses (CSS_CLASS_D_FLEX,
                                                             CSS_CLASS_ALIGN_ITEMS_CENTER,
                                                             CSS_CLASS_GAP_2,
                                                             CSS_CLASS_MB_2);

    // A GET form drops the query parameters of the action URL, so all existing
    // parameters must be added as hidden fields
    for (final URLParameter aParam : m_aBaseURL.params ())
      aForm.addChild (new HCHiddenField (aParam.getName (), aParam.getValue ()));

    // Always start with the first page again, because the number of matching
    // entries changes
    aForm.addChild (new HCHiddenField (PARAM_PAGE_INDEX, 0));
    aForm.addChild (new HCHiddenField (PARAM_PAGE_SIZE, m_nPageSize));
    aForm.addChild (new HCEdit (PARAM_SEARCH_TEXT).setValue (m_sSearchText)
                                                  .setPlaceholder ("Search")
                                                  .addClasses (CSS_CLASS_FORM_CONTROL, CSS_CLASS_W_AUTO));
    aForm.addChild (new HCButton_Submit ("Search").addClasses (CSS_CLASS_BTN, CSS_CLASS_BTN_SECONDARY));
    if (StringHelper.isNotEmpty (m_sSearchText))
      aForm.addChild (new HCA (_getPageURLNoSearch (0, m_nPageSize)).addChild ("Clear search"));
    return aForm;
  }

  /**
   * Create the UI of the pagination. If all items fit onto a single page, only the item count and
   * the search field are shown.
   *
   * @return The created UI node. Never <code>null</code>.
   */
  @NonNull
  public IHCNode getUI ()
  {
    final HCNodeList ret = new HCNodeList ();
    final int nPageCount = getPageCount ();

    ret.addChild (_createSearchUI ());

    // Textual information on the currently shown items
    {
      final String sInfo;
      if (m_nTotalCount == 0)
        sInfo = StringHelper.isNotEmpty (m_sSearchText) ? "No matching entry found" : "No entry found";
      else
      {
        final long nFirst = getFirstItemIndex () + 1L;
        final long nLast = Math.min (nFirst + m_nPageSize - 1L, m_nTotalCount);
        sInfo = "Showing entries " + nFirst + " to " + nLast + " of " + m_nTotalCount;
      }
      ret.addChild (new HCSpan ().addClass (CSS_CLASS_ME_3).addChild (sInfo));
    }

    // The page size selector
    {
      final HCNodeList aPageSizes = new HCNodeList ();
      aPageSizes.addChild (new HCTextNode ("Entries per page: "));
      boolean bFirst = true;
      for (final int nPageSize : PAGE_SIZES)
      {
        if (bFirst)
          bFirst = false;
        else
          aPageSizes.addChild (new HCTextNode (" | "));

        final String sText = Integer.toString (nPageSize);
        if (nPageSize == m_nPageSize)
          aPageSizes.addChild (new HCSpan ().addChild (sText));
        else
        {
          // Start with the first page again, because the offsets change
          aPageSizes.addChild (new HCA (_getPageURL (0, nPageSize)).addChild (sText));
        }
      }
      ret.addChild (aPageSizes);
    }

    if (nPageCount > 1)
    {
      final HCUL aUL = new HCUL ().addClass (CSS_CLASS_PAGINATION);

      // Previous page
      _createItem (aUL, "«", m_nPageIndex > 0 ? _getPageURL (m_nPageIndex - 1, m_nPageSize) : null, false);

      // Show at most 9 page links around the current page
      final int nMaxLinks = 9;
      int nFirstPage = Math.max (m_nPageIndex - nMaxLinks / 2, 0);
      final int nLastPage = Math.min (nFirstPage + nMaxLinks - 1, nPageCount - 1);
      nFirstPage = Math.max (nLastPage - nMaxLinks + 1, 0);

      if (nFirstPage > 0)
        _createItem (aUL, "1", _getPageURL (0, m_nPageSize), false);
      if (nFirstPage > 1)
        _createItem (aUL, "…", null, false);

      for (int i = nFirstPage; i <= nLastPage; ++i)
        _createItem (aUL, Integer.toString (i + 1), _getPageURL (i, m_nPageSize), i == m_nPageIndex);

      if (nLastPage < nPageCount - 2)
        _createItem (aUL, "…", null, false);
      if (nLastPage < nPageCount - 1)
        _createItem (aUL, Integer.toString (nPageCount), _getPageURL (nPageCount - 1, m_nPageSize), false);

      // Next page
      _createItem (aUL,
                   "»",
                   m_nPageIndex < nPageCount - 1 ? _getPageURL (m_nPageIndex + 1, m_nPageSize) : null,
                   false);

      ret.addChild (new HCNav ().addChild (aUL));
    }
    return ret;
  }
}
