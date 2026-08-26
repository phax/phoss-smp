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
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.IHCNode;
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
import com.helger.url.ISimpleURL;
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

  private final ISimpleURL m_aBaseURL;
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
   *        The total number of items to be paginated. If the value is &lt; 0 (e.g. because the
   *        backend could not determine it) it is treated as 0.
   */
  public SMPPagination (@NonNull final IWebPageExecutionContext aWPEC, final long nTotalCount)
  {
    ValueEnforcer.notNull (aWPEC, "WPEC");

    m_aBaseURL = aWPEC.getSelfHref ();
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
  private ISimpleURL _getPageURL (@Nonnegative final int nPageIndex, @Nonnegative final int nPageSize)
  {
    return new SimpleURL (m_aBaseURL).add (PARAM_PAGE_INDEX, nPageIndex).add (PARAM_PAGE_SIZE, nPageSize);
  }

  @NonNull
  private HCLI _createItem (@NonNull final HCUL aUL,
                            @NonNull final String sText,
                            @Nullable final ISimpleURL aTargetURL,
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
   * Align the provided DataTables with the server side pagination: the client side page length is
   * set to the server side page size, so that the client side pagination never kicks in, and the
   * client side page length selection is hidden, because the server side one is used instead.
   *
   * @param aDataTables
   *        The DataTables to be modified. May not be <code>null</code>.
   * @return The provided DataTables for chaining. Never <code>null</code>.
   */
  @NonNull
  public DataTables applyTo (@NonNull final DataTables aDataTables)
  {
    ValueEnforcer.notNull (aDataTables, "DataTables");
    return aDataTables.setPageLength (m_nPageSize).setLengthChange (false);
  }

  /**
   * Create the UI of the pagination. If all items fit onto a single page, only the item count is
   * shown.
   *
   * @return The created UI node. Never <code>null</code>.
   */
  @NonNull
  public IHCNode getUI ()
  {
    final HCNodeList ret = new HCNodeList ();
    final int nPageCount = getPageCount ();

    // Textual information on the currently shown items
    {
      final String sInfo;
      if (m_nTotalCount == 0)
        sInfo = "No entry found";
      else
      {
        final long nFirst = getFirstItemIndex () + 1L;
        final long nLast = Math.min (nFirst + m_nPageSize - 1L, m_nTotalCount);
        sInfo = "Showing entries " + nFirst + " to " + nLast + " of " + m_nTotalCount;
      }
      ret.addChild (new HCSpan ().addClass (DefaultCSSClassProvider.create ("me-3")).addChild (sInfo));
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
