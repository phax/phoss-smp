/**
 * Copyright (C) 2019-2020 Philip Helger and contributors
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
package com.helger.phoss.smp.mongodb.servlet;

import com.helger.phoss.smp.backend.mongodb.audit.MongoDBAuditor;
import com.helger.phoss.smp.servlet.SMPWebAppListener;
import com.helger.photon.audit.AuditHelper;

/**
 * Special SMP web app listener for MongoDB
 * 
 * @author Philip Helger
 */
public class SMPWebAppListenerMongoDB extends SMPWebAppListener
{
  @Override
  protected void initGlobalSettings ()
  {
    super.initGlobalSettings ();

    // Set the special Auditor that directly writes to the DB
    AuditHelper.setAuditor (new MongoDBAuditor ());
  }
}
