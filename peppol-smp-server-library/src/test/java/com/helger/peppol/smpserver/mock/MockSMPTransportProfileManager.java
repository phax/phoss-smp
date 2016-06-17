package com.helger.peppol.smpserver.mock;

import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.transportprofile.ISMPTransportProfileManager;

/**
 * Mock implementation of {@link ISMPTransportProfileManager}.
 *
 * @author Philip Helger
 */
final class MockSMPTransportProfileManager implements ISMPTransportProfileManager
{
  public EChange updateSMPTransportProfile (final String sSMPTransportProfileID, final String sName)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange removeSMPTransportProfile (final String sSMPTransportProfileID)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPTransportProfile getSMPTransportProfileOfID (final String sID)
  {
    return null;
  }

  public ICommonsList <? extends ISMPTransportProfile> getAllSMPTransportProfiles ()
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPTransportProfile createSMPTransportProfile (final String sID, final String sName)
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsSMPTransportProfileWithID (final String sID)
  {
    return false;
  }
}
