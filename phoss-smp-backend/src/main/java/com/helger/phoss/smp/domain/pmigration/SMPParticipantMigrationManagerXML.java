package com.helger.phoss.smp.domain.pmigration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsLocked;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.dao.DAOException;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.audit.AuditHelper;

/**
 * The XML implementation of {@link ISMPParticipantMigrationManager}
 *
 * @author Philip Helger
 * @since 5.4.0
 */
public class SMPParticipantMigrationManagerXML extends AbstractPhotonMapBasedWALDAO <ISMPParticipantMigration, SMPParticipantMigration>
                                               implements
                                               ISMPParticipantMigrationManager
{
  public SMPParticipantMigrationManagerXML (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPParticipantMigration.class, sFilename);
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPParticipantMigration _createSMPParticipantMigration (@Nonnull final SMPParticipantMigration aSMPParticipantMigration)
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

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPParticipantMigration _updateSMPParticipantMigration (@Nonnull final SMPParticipantMigration aSMPParticipantMigration)
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

  @Nonnull
  public ISMPParticipantMigration createOutboundParticipantMigration (@Nonnull final IParticipantIdentifier aParticipantID,
                                                                      @Nonnull @Nonempty final String sMigrationKey)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final SMPParticipantMigration ret = SMPParticipantMigration.createOutbound (aParticipantID, sMigrationKey);
    _createSMPParticipantMigration (ret);
    return ret;
  }

  @Nonnull
  public EChange setParticipantMigrationState (@Nullable final String sParticipantMigrationID,
                                               @Nonnull final EParticipantMigrationState eNewState)
  {
    ValueEnforcer.notNull (eNewState, "NewState");

    final SMPParticipantMigration aPM = getOfID (sParticipantMigrationID);
    if (aPM == null)
    {
      AuditHelper.onAuditModifyFailure (SMPParticipantMigration.OT, sParticipantMigrationID, "no-such-id");
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
    AuditHelper.onAuditModifySuccess (SMPParticipantMigration.OT, "migration-state", sParticipantMigrationID, eNewState);
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteParticipantMigration (@Nullable final String sParticipantMigrationID)
  {
    if (StringHelper.hasNoText (sParticipantMigrationID))
      return EChange.UNCHANGED;

    final SMPParticipantMigration aParticipantMigration;
    m_aRWLock.writeLock ().lock ();
    try
    {
      aParticipantMigration = internalDeleteItem (sParticipantMigrationID);
      if (aParticipantMigration == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPParticipantMigration.OT, "no-such-id", sParticipantMigrationID);
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

  @Nullable
  public ISMPParticipantMigration getParticipantMigrationOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPParticipantMigration> getAllOutboundParticipantMigrations (@Nullable final EParticipantMigrationState eState)
  {
    return getAll (x -> x.getDirection ().isOutbound () && x.isMatchingState (eState));
  }

  @Nonnull
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

  public boolean containsInboundMigrationInProgress (@Nullable final IParticipantIdentifier aParticipantID)
  {
    return aParticipantID != null &&
           containsAny (x -> x.getDirection ().isInbound () &&
                             x.getState ().isInProgress () &&
                             x.getParticipantIdentifier ().hasSameContent (aParticipantID));
  }
}
