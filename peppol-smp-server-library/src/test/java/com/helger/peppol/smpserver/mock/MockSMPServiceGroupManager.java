package com.helger.peppol.smpserver.mock;

import com.helger.commons.collection.ext.ICommonsCollection;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;

/**
 * Mock implementation of {@link ISMPServiceGroupManager}.
 * 
 * @author Philip Helger
 */
final class MockSMPServiceGroupManager implements ISMPServiceGroupManager
{
  public EChange updateSMPServiceGroup (final String sSMPServiceGroupID, final String sOwnerID, final String sExtension)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPServiceGroup getSMPServiceGroupOfID (final IParticipantIdentifier aParticipantIdentifier)
  {
    throw new UnsupportedOperationException ();
  }

  public int getSMPServiceGroupCountOfOwner (final String sOwnerID)
  {
    return 0;
  }

  public int getSMPServiceGroupCount ()
  {
    return 0;
  }

  public ICommonsCollection <? extends ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (final String sOwnerID)
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsCollection <? extends ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteSMPServiceGroup (final IParticipantIdentifier aParticipantIdentifier)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPServiceGroup createSMPServiceGroup (final String sOwnerID,
                                                 final IParticipantIdentifier aParticipantIdentifier,
                                                 final String sExtension)
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsSMPServiceGroupWithID (final IParticipantIdentifier aParticipantIdentifier)
  {
    return false;
  }
}
