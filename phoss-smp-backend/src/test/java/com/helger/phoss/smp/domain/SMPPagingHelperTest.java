/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Comparator;

import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * Test class for class {@link SMPPagingHelper}.
 *
 * @author Philip Helger
 */
public final class SMPPagingHelperTest
{
  private static ICommonsList <String> _list ()
  {
    return new CommonsArrayList <> ("d", "b", "e", "a", "c");
  }

  @Test
  public void testGetPage ()
  {
    final Comparator <String> aComp = Comparator.naturalOrder ();

    // First page
    assertEquals (new CommonsArrayList <> ("a", "b"), SMPPagingHelper.getPage (_list (), aComp, 0, 2));
    // Second page
    assertEquals (new CommonsArrayList <> ("c", "d"), SMPPagingHelper.getPage (_list (), aComp, 2, 2));
    // Last page - less elements than the page size
    assertEquals (new CommonsArrayList <> ("e"), SMPPagingHelper.getPage (_list (), aComp, 4, 2));
    // Start index out of range
    assertEquals (new CommonsArrayList <> (), SMPPagingHelper.getPage (_list (), aComp, 5, 2));
    assertEquals (new CommonsArrayList <> (), SMPPagingHelper.getPage (_list (), aComp, 100, 2));
    // Empty page
    assertEquals (new CommonsArrayList <> (), SMPPagingHelper.getPage (_list (), aComp, 0, 0));
    // Page larger than the list
    assertEquals (new CommonsArrayList <> ("a", "b", "c", "d", "e"),
                  SMPPagingHelper.getPage (_list (), aComp, 0, 100));
    // Empty list
    assertEquals (new CommonsArrayList <> (),
                  SMPPagingHelper.getPage (new CommonsArrayList <String> (), aComp, 0, 10));
  }

  @Test
  public void testInvalidParams ()
  {
    try
    {
      SMPPagingHelper.checkPagingParams (-1, 10);
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }

    try
    {
      SMPPagingHelper.checkPagingParams (0, -1);
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }
}
