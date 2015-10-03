package com.helger.peppol.smpserver;

import com.helger.peppol.smpserver.servlet.SMPWebAppListener;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;

/**
 * Special SMP server JUnit test rule.
 * 
 * @author Philip Helger
 */
public class SMPServerTestRule extends PhotonBasicWebTestRule
{
  @Override
  public void before ()
  {
    super.before ();
    SMPWebAppListener.initBackendFromConfiguration ();
  }

  @Override
  public void after ()
  {
    super.after ();
  }
}
