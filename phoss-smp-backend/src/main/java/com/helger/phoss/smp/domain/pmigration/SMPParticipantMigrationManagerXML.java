package com.helger.phoss.smp.domain.pmigration;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.dao.DAOException;
import com.helger.photon.app.dao.AbstractPhotonMapBasedWALDAO;

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
}
