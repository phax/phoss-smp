package com.helger.phoss.smp.app.async;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.text.IMultilingualText;
import com.helger.commons.text.ReadOnlyMultilingualText;
import com.helger.phoss.smp.CSMPServer;
import com.helger.photon.core.job.longrun.AbstractScopeAwareLongRunningJob;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.quartz.JobDataMap;

public abstract class AbstractSMPLongRunningJob extends AbstractScopeAwareLongRunningJob
{
  private final IMultilingualText m_aDesc;

  public AbstractSMPLongRunningJob (@Nonnull @Nonempty final String sJobDesc)
  {
    ValueEnforcer.notEmpty (sJobDesc, "JobDesc");
    m_aDesc = new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, sJobDesc);
  }

  @Nonnull
  public IMultilingualText getJobDescription ()
  {
    return m_aDesc;
  }

  @Override
  protected String getCurrentUserID (final JobDataMap aJobDataMap)
  {
    return LoggedInUserManager.getInstance ().getCurrentUserID ();
  }
}