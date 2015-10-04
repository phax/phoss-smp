package com.helger.peppol.smpserver.data.xml;

import java.util.concurrent.atomic.AtomicBoolean;

import com.helger.peppol.smpserver.data.xml.mgr.XMLManagerProvider;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;

/**
 * Special SMP server JUnit test rule for XML backend.
 *
 * @author Philip Helger
 */
public class SMPXMLTestRule extends PhotonBasicWebTestRule
{
  private static final AtomicBoolean s_aInitBackend = new AtomicBoolean (false);

  @Override
  public void before ()
  {
    super.before ();

    // Set it only once
    if (s_aInitBackend.compareAndSet (false, true))
      SMPMetaManager.setManagerFactory (new XMLManagerProvider ());
  }

  @Override
  public void after ()
  {
    super.after ();
  }
}
