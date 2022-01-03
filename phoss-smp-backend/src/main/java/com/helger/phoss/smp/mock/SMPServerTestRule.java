/*
 * Copyright (C) 2015-2022 Philip Helger and contributors
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

import javax.annotation.Nullable;

import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.LoggingSMPRedirectCallback;
import com.helger.phoss.smp.domain.serviceinfo.LoggingSMPServiceInformationCallback;
import com.helger.photon.app.mock.PhotonAppWebTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;

/**
 * Special SMP server JUnit test rule. This test rules DOES NOT spawn a server
 * process.
 *
 * @author Philip Helger
 */
public class SMPServerTestRule extends PhotonAppWebTestRule
{
  public SMPServerTestRule ()
  {
    this (null);
  }

  public SMPServerTestRule (@Nullable final String sSMPServerPropertiesPath)
  {
    if (StringHelper.hasText (sSMPServerPropertiesPath))
    {
      SystemProperties.setPropertyValue (SMPServerConfiguration.SYSTEM_PROPERTY_SMP_SERVER_PROPERTIES_PATH, sSMPServerPropertiesPath);
      SMPServerConfiguration.reloadConfiguration ();
    }
  }

  @Override
  public void before ()
  {
    super.before ();
    SMPMetaManager.initBackendFromConfiguration ();
    // Add some logging
    if (false)
      SMPMetaManager.getServiceInformationMgr ().serviceInformationCallbacks ().add (new LoggingSMPServiceInformationCallback ());
    SMPMetaManager.getRedirectMgr ().redirectCallbacks ().add (new LoggingSMPRedirectCallback ());

    PhotonSecurityManager.getUserMgr ()
                         .createPredefinedUser (CSecurity.USER_ADMINISTRATOR_ID,
                                                CSecurity.USER_ADMINISTRATOR_LOGIN,
                                                CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                CSecurity.USER_ADMINISTRATOR_PASSWORD,
                                                "SMP",
                                                "Admin",
                                                "Description",
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
