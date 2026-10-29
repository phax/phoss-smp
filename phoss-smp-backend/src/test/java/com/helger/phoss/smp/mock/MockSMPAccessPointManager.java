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

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPointHelper;

/**
 * Simple in-memory implementation of {@link ISMPAccessPointManager} for testing purposes.
 *
 * @author Philip Helger
 */
public final class MockSMPAccessPointManager implements ISMPAccessPointManager
{
  private final ICommonsOrderedMap <String, ISMPAccessPoint> m_aMap = new CommonsLinkedHashMap <> ();

  @Nullable
  private ISMPAccessPoint _getOfName (@Nullable final String sName)
  {
    if (StringHelper.isEmpty (sName))
      return null;
    final String sLookupKey = SMPAccessPointHelper.createNameLookupKey (sName);
    return m_aMap.findFirstValue (x -> SMPAccessPointHelper.createNameLookupKey (x.getValue ()).equals (sLookupKey));
  }

  @Nullable
  public ISMPAccessPoint createAccessPoint (@NonNull @Nonempty final String sName,
                                            @Nullable final String sEndpointReference,
                                            @Nullable final String sCertificate)
  {
    ValueEnforcer.notEmpty (sName, "Name");
    synchronized (m_aMap)
    {
      if (_getOfName (sName) != null)
        return null;

      final SMPAccessPoint aNew = SMPAccessPoint.createWithNewID (sName, sEndpointReference, sCertificate);
      m_aMap.put (aNew.getID (), aNew);
      return aNew;
    }
  }

  @NonNull
  public EChange updateAccessPoint (@Nullable final String sID,
                                    @NonNull @Nonempty final String sName,
                                    @Nullable final String sEndpointReference,
                                    @Nullable final String sCertificate)
  {
    ValueEnforcer.notEmpty (sName, "Name");
    synchronized (m_aMap)
    {
      final ISMPAccessPoint aAP = getAccessPointOfID (sID);
      if (aAP == null)
        return EChange.UNCHANGED;

      final ISMPAccessPoint aOther = _getOfName (sName);
      if (aOther != null && !aOther.getID ().equals (aAP.getID ()))
        return EChange.UNCHANGED;

      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (((SMPAccessPoint) aAP).setName (sName));
      eChange = eChange.or (((SMPAccessPoint) aAP).setEndpointReference (sEndpointReference));
      eChange = eChange.or (((SMPAccessPoint) aAP).setCertificate (sCertificate));
      return eChange;
    }
  }

  @NonNull
  public EChange updateAccessPointCertificate (@Nullable final String sID, @Nullable final String sNewCertificate)
  {
    synchronized (m_aMap)
    {
      final ISMPAccessPoint aAP = getAccessPointOfID (sID);
      if (aAP == null)
        return EChange.UNCHANGED;
      return ((SMPAccessPoint) aAP).setCertificate (sNewCertificate);
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

  @Nullable
  public ISMPAccessPoint getAccessPointOfName (@Nullable final String sName)
  {
    synchronized (m_aMap)
    {
      return _getOfName (sName);
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
}
