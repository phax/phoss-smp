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
 * @since 5.3.1
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
  public ISMPParticipantMigration createOutboundParticipantMigration (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final SMPParticipantMigration ret = SMPParticipantMigration.createOutbound (aParticipantID);
    _createSMPParticipantMigration (ret);
    return ret;
  }

  @Nonnull
  public EChange deleteParticipantMigration (@Nullable final String sParticipantMigrationID)
  {
    if (StringHelper.hasNoText (sParticipantMigrationID))
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPParticipantMigration aParticipantMigration = internalDeleteItem (sParticipantMigrationID);
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
    AuditHelper.onAuditDeleteSuccess (SMPParticipantMigration.OT, sParticipantMigrationID);
    return EChange.CHANGED;
  }

  @Nullable
  public ISMPParticipantMigration getParticipantMigrationOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPParticipantMigration> getAllOutboundParticipantMigrations ()
  {
    return getAll (x -> x.getDirection ().isOutbound ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPParticipantMigration> getAllInboundParticipantMigrations ()
  {
    return getAll (x -> x.getDirection ().isInbound ());
  }

  public boolean containsOutboundMigration (@Nullable final IParticipantIdentifier aParticipantID)
  {
    return aParticipantID != null &&
           containsAny (x -> x.getDirection ().isOutbound () && x.getParticipantIdentifier ().hasSameContent (aParticipantID));
  }

  public boolean containsInboundMigration (@Nullable final IParticipantIdentifier aParticipantID)
  {
    return aParticipantID != null &&
           containsAny (x -> x.getDirection ().isInbound () && x.getParticipantIdentifier ().hasSameContent (aParticipantID));
  }
}
