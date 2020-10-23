package com.helger.phoss.smp.domain.pmigration;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsLocked;
import com.helger.commons.annotation.Nonempty;
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
  public ISMPParticipantMigration createOutboundMigration (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final SMPParticipantMigration ret = SMPParticipantMigration.createOutbound (aParticipantID);
    _createSMPParticipantMigration (ret);
    return ret;
  }
}
