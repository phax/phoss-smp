/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.smpserver.mock;

import java.util.Locale;

import javax.annotation.Nullable;

import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.commons.url.SMap;
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
                                                              new SMap (),
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
