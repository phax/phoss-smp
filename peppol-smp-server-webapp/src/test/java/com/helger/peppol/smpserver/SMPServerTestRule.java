/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver;

import javax.annotation.Nullable;

import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.servlet.SMPWebAppListener;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;

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
      SystemProperties.setPropertyValue (SMPServerConfiguration.SYSTEM_PROPERTY_SMP_SERVER_PROPERTIES_PATH, sSMPServerPropertiesPath);
      SMPServerConfiguration.reloadConfiguration ();
    }
  }

  @Override
  public void before ()
  {
    super.before ();
    SMPWebAppListener.initBackendFromConfiguration ();
  }

  @Override
  public void after ()
  {
    // Reset for next run
    SMPMetaManager.setManagerProvider (null);
    super.after ();
  }
}
