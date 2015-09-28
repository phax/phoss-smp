package com.helger.peppol.smpserver.domain;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;

@IsSPIImplementation
public interface ISMPManagerProviderSPI
{
  @Nonnull
  ISMPServiceGroupManager createServiceGroupMgr ();

  @Nonnull
  ISMPRedirectManager createRedirectMgr ();

  @Nonnull
  ISMPServiceInformationManager createServiceInformationMgr ();
}
