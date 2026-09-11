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
package com.helger.phoss.smp.backend.xml.mgr;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.numeric.mutable.MutableBoolean;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsSet;
import com.helger.dao.DAOException;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPointHelper;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

/**
 * An XML file based implementation of the {@link ISMPAccessPointManager} interface.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public final class SMPAccessPointManagerXML extends AbstractPhotonMapBasedWALDAO <ISMPAccessPoint, SMPAccessPoint>
                                            implements
                                            ISMPAccessPointManager
{
  /** Lookup key to ID. Only to be accessed inside the RW lock. */
  private final ICommonsMap <String, String> m_aLookupIndex = new CommonsHashMap <> ();

  public SMPAccessPointManagerXML (@NonNull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPAccessPoint.class, sFilename);
  }

  /**
   * Rebuild the lookup index if it is out of sync with the contained items. This is necessary,
   * because items are also added while reading the persisted data, bypassing this manager.
   * <p>
   * Must be called inside the write lock.
   */
  private void _ensureIndexIsValid ()
  {
    if (m_aLookupIndex.size () != size ())
    {
      m_aLookupIndex.clear ();
      internalForEachValue (x -> m_aLookupIndex.put (SMPAccessPointHelper.createLookupKey (x), x.getID ()));
    }
  }

  @Nullable
  public ISMPAccessPoint findAccessPoint (@Nullable final String sEndpointReference)
  {
    final String sLookupKey = SMPAccessPointHelper.createLookupKey (sEndpointReference);
    return m_aRWLock.writeLockedGet ( () -> {
      _ensureIndexIsValid ();
      final String sID = m_aLookupIndex.get (sLookupKey);
      return sID == null ? null : getOfID (sID);
    });
  }

  @NonNull
  public ISMPAccessPoint getOrCreateAccessPoint (@Nullable final String sEndpointReference,
                                                 @Nullable final String sCertificate)
  {
    final String sLookupKey = SMPAccessPointHelper.createLookupKey (sEndpointReference);

    // Try to find an existing one and create it if it is not yet present. This must happen
    // atomically to avoid creating duplicates.
    final MutableBoolean aCertChanged = new MutableBoolean (false);
    final SMPAccessPoint aResolved = m_aRWLock.writeLockedGet ( () -> {
      _ensureIndexIsValid ();

      final String sExistingID = m_aLookupIndex.get (sLookupKey);
      if (sExistingID != null)
      {
        final SMPAccessPoint aExisting = getOfID (sExistingID);
        if (aExisting != null)
        {
          // An Access Point can only have one certificate - the latest one wins
          if (aExisting.setCertificate (sCertificate).isChanged ())
          {
            internalUpdateItem (aExisting);
            aCertChanged.set (true);
          }
          return aExisting;
        }
        // Stale index entry
        m_aLookupIndex.remove (sLookupKey);
      }

      final SMPAccessPoint aNew = SMPAccessPoint.createDetached (sEndpointReference, sCertificate);
      internalCreateItem (aNew);
      m_aLookupIndex.put (sLookupKey, aNew.getID ());
      return aNew;
    });

    if (aCertChanged.booleanValue ())
      AuditHelper.onAuditModifySuccess (SMPAccessPoint.OT, "set-certificate", aResolved.getID ());
    return aResolved;
  }

  @NonNull
  public EChange updateAccessPointCertificate (@Nullable final String sID, @Nullable final String sNewCertificate)
  {
    if (StringHelper.isEmpty (sID))
      return EChange.UNCHANGED;

    final EChange eChange = m_aRWLock.writeLockedGet ( () -> {
      final SMPAccessPoint aAP = getOfID (sID);
      if (aAP == null)
        return EChange.UNCHANGED;
      if (aAP.setCertificate (sNewCertificate).isUnchanged ())
        return EChange.UNCHANGED;
      internalUpdateItem (aAP);
      return EChange.CHANGED;
    });

    if (eChange.isUnchanged ())
      return EChange.UNCHANGED;

    AuditHelper.onAuditModifySuccess (SMPAccessPoint.OT, "set-certificate", sID);
    return EChange.CHANGED;
  }

  @NonNull
  public EChange updateAccessPointEndpointReference (@Nullable final String sID,
                                                     @Nullable final String sNewEndpointReference)
  {
    if (StringHelper.isEmpty (sID))
      return EChange.UNCHANGED;

    final EChange eChange = m_aRWLock.writeLockedGet ( () -> {
      _ensureIndexIsValid ();

      final SMPAccessPoint aAP = getOfID (sID);
      if (aAP == null)
        return EChange.UNCHANGED;

      final String sOldLookupKey = SMPAccessPointHelper.createLookupKey (aAP);
      if (aAP.setEndpointReference (sNewEndpointReference).isUnchanged ())
        return EChange.UNCHANGED;

      m_aLookupIndex.remove (sOldLookupKey);
      m_aLookupIndex.put (SMPAccessPointHelper.createLookupKey (aAP), sID);
      internalUpdateItem (aAP);
      return EChange.CHANGED;
    });

    if (eChange.isUnchanged ())
      return EChange.UNCHANGED;

    AuditHelper.onAuditModifySuccess (SMPAccessPoint.OT, "set-endpoint-reference", sID, sNewEndpointReference);
    return EChange.CHANGED;
  }

  @Nullable
  public ISMPAccessPoint getAccessPointOfID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;
    return getOfID (sID);
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPAccessPoint> getAllAccessPoints ()
  {
    return getAll ();
  }

  @Nonnegative
  public long getAccessPointCount ()
  {
    return size ();
  }

  @NonNull
  public EChange deleteAccessPoint (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return EChange.UNCHANGED;

    final EChange eChange = m_aRWLock.writeLockedGet ( () -> {
      final SMPAccessPoint aDeleted = internalDeleteItem (sID);
      if (aDeleted == null)
        return EChange.UNCHANGED;
      m_aLookupIndex.remove (SMPAccessPointHelper.createLookupKey (aDeleted));
      return EChange.CHANGED;
    });

    if (eChange.isUnchanged ())
    {
      AuditHelper.onAuditDeleteFailure (SMPAccessPoint.OT, sID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (SMPAccessPoint.OT, sID);
    return EChange.CHANGED;
  }

  @Nonnegative
  public long deleteAllUnusedAccessPoints (@NonNull final ICommonsSet <String> aUsedIDs)
  {
    final ICommonsList <String> aUnusedIDs = new CommonsArrayList <> ();
    forEachKey (x -> !aUsedIDs.contains (x), aUnusedIDs::add);

    long nDeleted = 0;
    for (final String sID : aUnusedIDs)
      if (deleteAccessPoint (sID).isChanged ())
        nDeleted++;
    return nDeleted;
  }
}
