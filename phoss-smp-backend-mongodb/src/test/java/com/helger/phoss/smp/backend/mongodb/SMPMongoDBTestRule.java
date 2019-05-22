/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
    if (false)
      SMPMetaManager.setManagerProvider (SMPBackendRegistry.getInstance ()
                                                           .getManagerProvider (MongoDBSMPBackendRegistrarSPI.BACKEND_ID));
  }
}
