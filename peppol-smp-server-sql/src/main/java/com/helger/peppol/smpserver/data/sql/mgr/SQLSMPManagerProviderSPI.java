package com.helger.peppol.smpserver.data.sql.mgr;

import javax.annotation.Nonnull;

import com.helger.peppol.smpserver.domain.ISMPManagerProviderSPI;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;

public class SQLSMPManagerProviderSPI implements ISMPManagerProviderSPI
{
  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new SMPServiceGroupManager ();
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr ()
  {
    return new SMPRedirectManager ();
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    return new SMPServiceInformationManager ();
  }
}
