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

import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.paging.IPagingSpec;
import com.helger.dao.DAOException;
import com.helger.phoss.smp.domain.accesspoint.ESMPAccessPointColumn;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPointHelper;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.core.paging.TableColumnHelper;
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
  private static final ESMPAccessPointColumn [] COLUMNS = ESMPAccessPointColumn.values ();

  /** Name lookup key to ID. Only to be accessed inside the RW lock. */
  private final ICommonsMap <String, String> m_aNameIndex = new CommonsHashMap <> ();

  public SMPAccessPointManagerXML (@NonNull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPAccessPoint.class, sFilename);
  }

  /**
   * Rebuild the name index if it is out of sync with the contained items. This is necessary,
   * because items are also added while reading the persisted data, bypassing this manager.
   * <p>
   * Must be called inside the write lock.
   */
  private void _ensureIndexIsValid ()
  {
    if (m_aNameIndex.size () != size ())
    {
      m_aNameIndex.clear ();
      internalForEachValue (x -> m_aNameIndex.put (SMPAccessPointHelper.createNameLookupKey (x), x.getID ()));
    }
  }

  @Nullable
  private SMPAccessPoint _getOfName (@Nullable final String sName)
  {
    final String sLookupKey = SMPAccessPointHelper.createNameLookupKey (sName);
    if (sLookupKey.isEmpty ())
      return null;

    return m_aRWLock.writeLockedGet ( () -> {
      _ensureIndexIsValid ();
      final String sID = m_aNameIndex.get (sLookupKey);
      return sID == null ? null : getOfID (sID);
    });
  }

  @Nullable
  public ISMPAccessPoint createAccessPoint (@NonNull @Nonempty final String sName,
                                            @Nullable final String sEndpointReference,
                                            @Nullable final String sCertificate)
  {
    ValueEnforcer.notEmpty (sName, "Name");

    final String sLookupKey = SMPAccessPointHelper.createNameLookupKey (sName);
    final SMPAccessPoint aCreated = m_aRWLock.writeLockedGet ( () -> {
      _ensureIndexIsValid ();
      if (m_aNameIndex.containsKey (sLookupKey))
        return null;

      final SMPAccessPoint aNew = SMPAccessPoint.createWithNewID (sName, sEndpointReference, sCertificate);
      internalCreateItem (aNew);
      m_aNameIndex.put (sLookupKey, aNew.getID ());
      return aNew;
    });

    if (aCreated == null)
    {
      AuditHelper.onAuditCreateFailure (SMPAccessPoint.OT, "name-already-in-use", sName);
      return null;
    }

    AuditHelper.onAuditCreateSuccess (SMPAccessPoint.OT, aCreated.getID (), sName, sEndpointReference);
    return aCreated;
  }

  @NonNull
  public EChange updateAccessPoint (@Nullable final String sID,
                                    @NonNull @Nonempty final String sName,
                                    @Nullable final String sEndpointReference,
                                    @Nullable final String sCertificate)
  {
    ValueEnforcer.notEmpty (sName, "Name");

    if (StringHelper.isEmpty (sID))
      return EChange.UNCHANGED;

    final String sNewLookupKey = SMPAccessPointHelper.createNameLookupKey (sName);
    final EChange eChange = m_aRWLock.writeLockedGet ( () -> {
      _ensureIndexIsValid ();

      final SMPAccessPoint aAP = getOfID (sID);
      if (aAP == null)
        return EChange.UNCHANGED;

      // The name must stay unique
      final String sExistingID = m_aNameIndex.get (sNewLookupKey);
      if (sExistingID != null && !sExistingID.equals (sID))
        return EChange.UNCHANGED;

      final String sOldLookupKey = SMPAccessPointHelper.createNameLookupKey (aAP);
      EChange eRealChange = EChange.UNCHANGED;
      eRealChange = eRealChange.or (aAP.setName (sName));
      eRealChange = eRealChange.or (aAP.setEndpointReference (sEndpointReference));
      eRealChange = eRealChange.or (aAP.setCertificate (sCertificate));
      if (eRealChange.isUnchanged ())
        return EChange.UNCHANGED;

      m_aNameIndex.remove (sOldLookupKey);
      m_aNameIndex.put (sNewLookupKey, sID);
      internalUpdateItem (aAP);
      return EChange.CHANGED;
    });

    if (eChange.isUnchanged ())
      return EChange.UNCHANGED;

    AuditHelper.onAuditModifySuccess (SMPAccessPoint.OT, "set-all", sID, sName, sEndpointReference);
    return EChange.CHANGED;
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

  @Nullable
  public ISMPAccessPoint getAccessPointOfID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;
    return getOfID (sID);
  }

  @Nullable
  public ISMPAccessPoint getAccessPointOfName (@Nullable final String sName)
  {
    return _getOfName (sName);
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPAccessPoint> getAllAccessPoints ()
  {
    return getAll ();
  }

  @Override
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPAccessPoint> getAllAccessPoints (@NonNull final IPagingSpec aPagingSpec,
                                                            @Nullable final String sSearchText)
  {
    return getAllPaged (TableColumnHelper.getSearchPredicate (COLUMNS, sSearchText),
                        aPagingSpec,
                        TableColumnHelper.getComparator (COLUMNS, aPagingSpec));
  }

  @Nonnegative
  public long getAccessPointCount ()
  {
    return size ();
  }

  @Override
  public long getAccessPointCount (@Nullable final String sSearchText)
  {
    final Predicate <ISMPAccessPoint> aFilter = TableColumnHelper.getSearchPredicate (COLUMNS, sSearchText);
    return aFilter == null ? getAccessPointCount () : getCount (aFilter);
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
      m_aNameIndex.remove (SMPAccessPointHelper.createNameLookupKey (aDeleted));
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
}
