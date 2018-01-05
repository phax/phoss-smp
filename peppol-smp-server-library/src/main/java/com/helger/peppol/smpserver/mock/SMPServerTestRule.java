/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.mock;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;

/**
 * Special SMP server JUnit test rule.
 *
 * @author Philip Helger
 */
public class SMPServerTestRule extends PhotonBasicWebTestRule
{
  public SMPServerTestRule ()
  {
    this (null);
  }

  public SMPServerTestRule (@Nullable final String sSMPServerPropertiesPath)
  {
    if (StringHelper.hasText (sSMPServerPropertiesPath))
    {
      SystemProperties.setPropertyValue (SMPServerConfiguration.SYSTEM_PROPERTY_SMP_SERVER_PROPERTIES_PATH,
                                         sSMPServerPropertiesPath);
      SMPServerConfiguration.reloadConfiguration ();
    }
  }

  @Override
  public void before ()
  {
    super.before ();
    SMPMetaManager.initBackendFromConfiguration ();
    PhotonSecurityManager.getUserMgr ().createPredefinedUser (CSecurity.USER_ADMINISTRATOR_ID,
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
