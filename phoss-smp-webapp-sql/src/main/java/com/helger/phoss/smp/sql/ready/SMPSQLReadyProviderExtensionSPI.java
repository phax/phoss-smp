/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.sql.ready;

import java.time.Duration;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.phoss.smp.backend.sql.SMPDataSourceSingleton;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.ready.ISMPReadyProviderExtensionSPI;

/**
 * SQL specific readiness check. It takes a connection from the connection pool and validates it,
 * so that a database that went down after the connection was pooled is detected as well.
 *
 * @author vinit-thummar
 * @since 8.3.1
 */
@IsSPIImplementation
public class SMPSQLReadyProviderExtensionSPI implements ISMPReadyProviderExtensionSPI
{
  public boolean isReady ()
  {
    // Use the configured readiness timeout, but at least 1 second, because 0 would mean
    // "no timeout" for the JDBC driver. The overall time limit is enforced by SMPReadyProvider.
    final Duration aReadyTimeout = SMPServerConfiguration.getReadyTimeout ();
    final long nSeconds = Math.max (1, aReadyTimeout.toSeconds ());
    return SMPDataSourceSingleton.isDBConnectionPossible ((int) Math.min (nSeconds, Integer.MAX_VALUE));
  }
}
