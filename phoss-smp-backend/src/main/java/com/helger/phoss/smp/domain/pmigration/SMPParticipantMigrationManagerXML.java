/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.pmigration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.ELockType;
import com.helger.annotation.concurrent.IsLocked;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.dao.DAOException;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

/**
 * The XML implementation of {@link ISMPParticipantMigrationManager}
 *
 * @author Philip Helger
 * @since 5.4.0
 */
public class SMPParticipantMigrationManagerXML extends
                                               AbstractPhotonMapBasedWALDAO <ISMPParticipantMigration, SMPParticipantMigration>
                                               implements
                                               ISMPParticipantMigrationManager
{
  public SMPParticipantMigrationManagerXML (@NonNull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPParticipantMigration.class, sFilename);
  }

  @NonNull
  @IsLocked (ELockType.WRITE)
  private ISMPParticipantMigration _createSMPParticipantMigration (@NonNull final SMPParticipantMigration aSMPParticipantMigration)
  {
    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aSMPParticipantMigration);
    });
    AuditHelper.onAuditCreateSuccess (SMPParticipantMigration.OT,
                                      aSMPParticipantMigration.getID (),
                                      aSMPParticipantMigration.getDirection (),
                                      aSMPParticipantMigration.getParticipantIdentifier ().getURIEncoded (),
                                      aSMPParticipantMigration.getInitiationDateTime (),
                                      aSMPParticipantMigration.getMigrationKey ());
    return aSMPParticipantMigration;
  }

  @NonNull
  @IsLocked (ELockType.WRITE)
  private ISMPParticipantMigration _updateSMPParticipantMigration (@NonNull final SMPParticipantMigration aSMPParticipantMigration)
  {
    m_aRWLock.writeLocked ( () -> {
      internalUpdateItem (aSMPParticipantMigration);
    });
    AuditHelper.onAuditModifySuccess (SMPParticipantMigration.OT,
                                      aSMPParticipantMigration.getID (),
                                      aSMPParticipantMigration.getDirection (),
                                      aSMPParticipantMigration.getParticipantIdentifier ().getURIEncoded (),
                                      aSMPParticipantMigration.getInitiationDateTime (),
                                      aSMPParticipantMigration.getMigrationKey ());
    return aSMPParticipantMigration;
  }

  @NonNull
  public ISMPParticipantMigration createOutboundParticipantMigration (@NonNull final IParticipantIdentifier aParticipantID,
                                                                      @NonNull @Nonempty final String sMigrationKey)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final SMPParticipantMigration ret = SMPParticipantMigration.createOutbound (aParticipantID, sMigrationKey);
    _createSMPParticipantMigration (ret);
    return ret;
  }

  @NonNull
  public ISMPParticipantMigration createInboundParticipantMigration (@NonNull final IParticipantIdentifier aParticipantID,
                                                                     @NonNull @Nonempty final String sMigrationKey)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final SMPParticipantMigration ret = SMPParticipantMigration.createInbound (aParticipantID, sMigrationKey);
    _createSMPParticipantMigration (ret);
    return ret;
  }

  @NonNull
  public EChange deleteAllParticipantMigrationsOfParticipant (@NonNull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final ICommonsList <SMPParticipantMigration> aAllMatching = internalGetAll (x -> x.getParticipantIdentifier ()
                                                                                      .hasSameContent (aParticipantID));
    EChange ret = EChange.UNCHANGED;
    for (final SMPParticipantMigration aItem : aAllMatching)
      ret = ret.or (deleteParticipantMigrationOfID (aItem.getID ()));
    return ret;
  }

  @NonNull
  public EChange deleteParticipantMigrationOfID (@Nullable final String sParticipantMigrationID)
  {
    if (StringHelper.isEmpty (sParticipantMigrationID))
      return EChange.UNCHANGED;

    final SMPParticipantMigration aParticipantMigration;
    m_aRWLock.writeLock ().lock ();
    try
    {
      aParticipantMigration = internalDeleteItem (sParticipantMigrationID);
      if (aParticipantMigration == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPParticipantMigration.OT, sParticipantMigrationID, "no-such-id");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (SMPParticipantMigration.OT,
                                      sParticipantMigrationID,
                                      aParticipantMigration.getDirection ().getID (),
                                      aParticipantMigration.getParticipantIdentifier ().getURIEncoded (),
                                      aParticipantMigration.getInitiationDateTime (),
                                      aParticipantMigration.getMigrationKey ());
    return EChange.CHANGED;
  }

  @NonNull
  public EChange setParticipantMigrationState (@Nullable final String sParticipantMigrationID,
                                               @NonNull final EParticipantMigrationState eNewState)
  {
    ValueEnforcer.notNull (eNewState, "NewState");

    final SMPParticipantMigration aPM = getOfID (sParticipantMigrationID);
    if (aPM == null)
    {
      AuditHelper.onAuditModifyFailure (SMPParticipantMigration.OT,
                                        "set-migration-state",
                                        sParticipantMigrationID,
                                        "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aPM.setState (eNewState));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;

      internalUpdateItem (aPM);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (SMPParticipantMigration.OT,
                                      "set-migration-state",
                                      sParticipantMigrationID,
                                      eNewState);
    return EChange.CHANGED;
  }

  @Nullable
  public ISMPParticipantMigration getParticipantMigrationOfID (@Nullable final String sParticipantMigrationID)
  {
    return getOfID (sParticipantMigrationID);
  }

  @Nullable
  public ISMPParticipantMigration getParticipantMigrationOfParticipantID (@NonNull final EParticipantMigrationDirection eDirection,
                                                                          @NonNull final EParticipantMigrationState eState,
                                                                          @Nullable final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (eDirection, "Direction");
    ValueEnforcer.notNull (eState, "State");
    if (aParticipantID == null)
      return null;

    return findFirst (x -> x.getDirection ().equals (eDirection) &&
                           x.getState ().equals (eState) &&
                           x.getParticipantIdentifier ().hasSameContent (aParticipantID));
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPParticipantMigration> getAllOutboundParticipantMigrations (@Nullable final EParticipantMigrationState eState)
  {
    return getAll (x -> x.getDirection ().isOutbound () && x.isMatchingState (eState));
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPParticipantMigration> getAllInboundParticipantMigrations (@Nullable final EParticipantMigrationState eState)
  {
    return getAll (x -> x.getDirection ().isInbound () && x.isMatchingState (eState));
  }

  public boolean containsOutboundMigrationInProgress (@Nullable final IParticipantIdentifier aParticipantID)
  {
    return aParticipantID != null &&
           containsAny (x -> x.getDirection ().isOutbound () &&
                             x.getState ().isInProgress () &&
                             x.getParticipantIdentifier ().hasSameContent (aParticipantID));
  }

  public boolean containsInboundMigration (@Nullable final IParticipantIdentifier aParticipantID)
  {
    return aParticipantID != null &&
           containsAny (x -> x.getDirection ().isInbound () &&
                             x.getState () == EParticipantMigrationState.MIGRATED &&
                             x.getParticipantIdentifier ().hasSameContent (aParticipantID));
  }
}
