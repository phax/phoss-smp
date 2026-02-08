/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.mock;

import java.util.Locale;
import java.util.Map;

import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.photon.app.mock.PhotonAppWebTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUserManager;

/**
 * Special SMP server JUnit test rule. This test rules DOES NOT spawn a server process.
 *
 * @author Philip Helger
 */
public class SMPServerTestRule extends PhotonAppWebTestRule
{
  public SMPServerTestRule ()
  {}

  @Override
  public void before ()
  {
    super.before ();

    SMPMetaManager.initBackendFromConfiguration ();

    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    if (!aUserMgr.containsWithID (CSecurity.USER_ADMINISTRATOR_ID))
      aUserMgr.createPredefinedUser (CSecurity.USER_ADMINISTRATOR_ID,
                                     CSecurity.USER_ADMINISTRATOR_EMAIL,
                                     CSecurity.USER_ADMINISTRATOR_EMAIL,
                                     CSecurity.USER_ADMINISTRATOR_PASSWORD,
                                     "Test Firstname",
                                     "Test Lastname",
                                     "Test Description",
                                     Locale.US,
                                     (Map <String, String>) null,
                                     false);
  }

  @Override
  public void after ()
  {
    // Reset for next run
    SMPMetaManager.setManagerProvider (null);
    super.after ();
  }
}
