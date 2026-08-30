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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.base.state.ETriState;
import com.helger.io.resource.FileSystemResource;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;

/**
 * Test class for class {@link SMPSQLReadyProviderExtensionSPI}.
 *
 * @author vinit-thummar
 */
public final class SMPSQLReadyProviderExtensionSPITest
{
  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (new FileSystemResource ("src/test/resources/test-smp-server-sql.properties"));

  @Test
  public void testReadinessFollowsTheDatabase ()
  {
    final ETriState eConnectionState = SMPMetaManager.getInstance ().getBackendConnectionState ();
    if (eConnectionState.isUndefined ())
    {
      // The database was not touched yet - nothing to compare against
      return;
    }

    final boolean bReady = new SMPSQLReadyProviderExtensionSPI ().isReady ();
    if (eConnectionState.isTrue ())
    {
      // MySQL is running
      assertTrue (bReady);
    }
    else
    {
      // MySQL is not running. Note: the connection pool hands out connections
      // without validating them, so this only works because the readiness check
      // validates the connection it received.
      assertFalse (bReady);
    }
  }
}
