/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.ui.cache;

import com.helger.commons.cache.Cache;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.phoss.smp.domain.SMPMetaManager;

/**
 * Simple cache for transport profile objects
 *
 * @author Philip Helger
 */
public final class SMPTransportProfileCache extends Cache <String, ISMPTransportProfile>
{
  public SMPTransportProfileCache ()
  {
    // Allow null values!
    super (x -> SMPMetaManager.getTransportProfileMgr ().getSMPTransportProfileOfID (x),
           -1,
           "Transport Profile cache",
           true);
  }
}
