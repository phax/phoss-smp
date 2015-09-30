package com.helger.peppol.smpserver.data.sql.mgr;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.peppol.smpserver.domain.ISMPManagerProviderSPI;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;

@IsSPIImplementation
public class SQLManagerProviderSPI implements ISMPManagerProviderSPI
{
  @Nonnull
  public ISMPUserManager createUserMgr ()
  {
    return new SQLUserManager ();
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new SQLServiceGroupManager ();
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr ()
  {
    return new SQLRedirectManager ();
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    return new SQLServiceInformationManager ();
  }
}
