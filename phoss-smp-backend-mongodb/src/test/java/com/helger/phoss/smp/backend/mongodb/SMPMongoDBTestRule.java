package com.helger.phoss.smp.backend.mongodb;

import com.helger.phoss.smp.backend.SMPBackendRegistry;
import com.helger.phoss.smp.backend.mongodb.spi.MongoDBSMPBackendRegistrarSPI;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.photon.app.mock.PhotonAppWebTestRule;

public class SMPMongoDBTestRule extends PhotonAppWebTestRule
{
  @Override
  public void before ()
  {
    super.before ();
    // Ensure manager provider is setup
    // Requires all managers to be implemented
    SMPMetaManager.setManagerProvider (SMPBackendRegistry.getInstance ()
                                                         .getManagerProvider (MongoDBSMPBackendRegistrarSPI.BACKEND_ID));
  }
}
