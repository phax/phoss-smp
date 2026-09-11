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
package com.helger.phoss.smp.mock;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.collection.commons.ICommonsSet;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPointHelper;

/**
 * Simple in-memory implementation of {@link ISMPAccessPointManager} for testing purposes.
 *
 * @author Philip Helger
 */
final class MockSMPAccessPointManager implements ISMPAccessPointManager
{
  private final ICommonsOrderedMap <String, ISMPAccessPoint> m_aMap = new CommonsLinkedHashMap <> ();

  @Nullable
  public ISMPAccessPoint findAccessPoint (@Nullable final String sEndpointReference,
                                          @Nullable final String sCertificate)
  {
    final String sLookupKey = SMPAccessPointHelper.createLookupKey (sEndpointReference, sCertificate);
    synchronized (m_aMap)
    {
      return m_aMap.findFirstValue (x -> SMPAccessPointHelper.createLookupKey (x.getValue ()).equals (sLookupKey));
    }
  }

  @NonNull
  public ISMPAccessPoint getOrCreateAccessPoint (@Nullable final String sEndpointReference,
                                                 @Nullable final String sCertificate)
  {
    synchronized (m_aMap)
    {
      final ISMPAccessPoint aExisting = findAccessPoint (sEndpointReference, sCertificate);
      if (aExisting != null)
        return aExisting;

      final SMPAccessPoint aNew = SMPAccessPoint.createDetached (sEndpointReference, sCertificate);
      m_aMap.put (aNew.getID (), aNew);
      return aNew;
    }
  }

  @Nullable
  public ISMPAccessPoint getAccessPointOfID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;
    synchronized (m_aMap)
    {
      return m_aMap.get (sID);
    }
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPAccessPoint> getAllAccessPoints ()
  {
    synchronized (m_aMap)
    {
      return new CommonsArrayList <> (m_aMap.values ());
    }
  }

  @Nonnegative
  public long getAccessPointCount ()
  {
    synchronized (m_aMap)
    {
      return m_aMap.size ();
    }
  }

  @NonNull
  public EChange deleteAccessPoint (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return EChange.UNCHANGED;
    synchronized (m_aMap)
    {
      return EChange.valueOf (m_aMap.remove (sID) != null);
    }
  }

  @Nonnegative
  public long deleteAllUnusedAccessPoints (@NonNull final ICommonsSet <String> aUsedIDs)
  {
    synchronized (m_aMap)
    {
      final ICommonsList <String> aUnused = new CommonsArrayList <> ();
      for (final String sID : m_aMap.keySet ())
        if (!aUsedIDs.contains (sID))
          aUnused.add (sID);
      for (final String sID : aUnused)
        m_aMap.remove (sID);
      return aUnused.size ();
    }
  }
}
