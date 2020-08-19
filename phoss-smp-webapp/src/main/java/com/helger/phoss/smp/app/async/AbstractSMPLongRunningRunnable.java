package com.helger.phoss.smp.app.async;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.callback.IThrowingRunnable;
import com.helger.commons.state.ESuccess;
import com.helger.commons.text.IMultilingualText;
import com.helger.commons.text.ReadOnlyMultilingualText;
import com.helger.phoss.smp.CSMPServer;
import com.helger.photon.core.longrun.ILongRunningJob;
import com.helger.photon.core.longrun.LongRunningJobManager;
import com.helger.photon.core.longrun.LongRunningJobResult;
import com.helger.photon.core.mgr.PhotonBasicManager;
import com.helger.photon.security.login.LoggedInUserManager;

public abstract class AbstractSMPLongRunningRunnable implements IThrowingRunnable <Exception>, ILongRunningJob
{
  private final IMultilingualText m_aDesc;

  public AbstractSMPLongRunningRunnable (@Nonnull @Nonempty final String sJobDesc)
  {
    ValueEnforcer.notEmpty (sJobDesc, "JobDesc");
    m_aDesc = new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, sJobDesc);
  }

  @Nonnull
  public IMultilingualText getJobDescription ()
  {
    return m_aDesc;
  }

  @Nonnull
  protected String getCurrentUserID ()
  {
    return LoggedInUserManager.getInstance ().getCurrentUserID ();
  }

  /**
   * @return The {@link LongRunningJobManager} to be used. May not return
   *         <code>null</code>.
   */
  @Nonnull
  protected LongRunningJobManager getLongRunningJobManager ()
  {
    return PhotonBasicManager.getLongRunningJobMgr ();
  }

  public void run () throws Exception
  {
    final String sUserID = getCurrentUserID ();

    // Remember that a long running job is starting
    final String sLongRunningJobID = getLongRunningJobManager ().onStartJob (this, sUserID);

    try
    {
      // Create the main result
      final LongRunningJobResult aJobResult = createLongRunningJobResult ();

      // Mark the long running job as finished
      getLongRunningJobManager ().onEndJob (sLongRunningJobID, ESuccess.SUCCESS, aJobResult);
    }
    catch (final Exception ex)
    {
      // Mark the long running job as finished
      getLongRunningJobManager ().onEndJob (sLongRunningJobID,
                                            ESuccess.FAILURE,
                                            LongRunningJobResult.createText ("Exception: " +
                                                                             ex.getClass ().getName () +
                                                                             " - " +
                                                                             ex.getMessage ()));
    }
  }

}
