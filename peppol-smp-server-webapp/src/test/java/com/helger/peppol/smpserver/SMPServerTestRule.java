package com.helger.peppol.smpserver;

import java.util.concurrent.atomic.AtomicBoolean;

import com.helger.peppol.smpserver.servlet.SMPWebAppListener;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;

/**
 * Special SMP server JUnit test rule.
 *
 * @author Philip Helger
 */
public class SMPServerTestRule extends PhotonBasicWebTestRule
{
  private static final AtomicBoolean s_aInitBackend = new AtomicBoolean (false);

  @Override
  public void before ()
  {
    super.before ();

    // Set it only once
    if (s_aInitBackend.compareAndSet (false, true))
      SMPWebAppListener.initBackendFromConfiguration ();
  }

  @Override
  public void after ()
  {
    super.after ();
  }
}
