package com.helger.peppol.smpserver.mock;

import java.util.Collection;

import com.helger.commons.state.EChange;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smpserver.domain.sml.ISMLInfoManager;

/**
 * Mock implementation of {@link ISMLInfoManager}.
 *
 * @author Philip Helger
 */
final class MockSMLInfoManager implements ISMLInfoManager
{
  public EChange updateSMLInfo (final String sSMLInfoID,
                                final String sDisplayName,
                                final String sDNSZone,
                                final String sManagementServiceURL,
                                final boolean bClientCertificateRequired)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange removeSMLInfo (final String sSMLInfoID)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMLInfo getSMLInfoOfID (final String sID)
  {
    return null;
  }

  public Collection <? extends ISMLInfo> getAllSMLInfos ()
  {
    throw new UnsupportedOperationException ();
  }

  public ISMLInfo createSMLInfo (final String sDisplayName,
                                 final String sDNSZone,
                                 final String sManagementServiceURL,
                                 final boolean bClientCertificateRequired)
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsSMLInfoWithID (final String sID)
  {
    return false;
  }
}
