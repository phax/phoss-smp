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
package com.helger.phoss.smp.domain.serviceinfo;

import java.util.Comparator;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;

/**
 * Lightweight holder for endpoint usage information, containing the number of endpoints sharing a
 * particular URL or certificate and the set of service group IDs that use it.
 *
 * @author Philip Helger
 * @since 8.0.16
 */
public final class EndpointUsageInfo implements IEndpointUsageInfo
{
  private int m_nEndpointCount;
  private final ICommonsSet <String> m_aServiceGroupIDs = new CommonsHashSet <> ();

  public EndpointUsageInfo ()
  {}

  /**
   * Increment the endpoint usage for the provided service group ID.
   *
   * @param sServiceGroupID
   *        The service group ID to add. May not be <code>null</code>.
   */
  public void incrementForServiceGroupID (@NonNull final String sServiceGroupID)
  {
    m_nEndpointCount++;
    m_aServiceGroupIDs.add (sServiceGroupID);
  }

  /**
   * @return The number of endpoints sharing this URL or certificate. Always &ge; 0.
   */
  @Nonnegative
  public int getEndpointCount ()
  {
    return m_nEndpointCount;
  }

  /**
   * @return The number of distinct service groups using this URL or certificate. Always &ge; 0.
   */
  @Nonnegative
  public int getServiceGroupCount ()
  {
    return m_aServiceGroupIDs.size ();
  }

  /**
   * @return A mutable copy of the set of service group IDs. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllServiceGroupIDs ()
  {
    return m_aServiceGroupIDs.getClone ();
  }

  /**
   * @return A mutable sorted list of service group IDs. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <String> getServiceGroupIDsSorted (@NonNull final Comparator <? super String> aComparator)
  {
    return m_aServiceGroupIDs.getSorted (aComparator);
  }
}
