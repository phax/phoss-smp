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
package com.helger.phoss.smp.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.sgprops.ESGCustomPropertyType;
import com.helger.phoss.smp.domain.sgprops.SGCustomProperty;
import com.helger.phoss.smp.domain.sgprops.SGCustomPropertyList;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Test that an update of a Service Group is immediately visible, even if the Service Group cache of
 * the SQL backend is enabled.
 *
 * @author Philip Helger
 */
public final class ServiceGroupCacheSQLTest extends AbstractSMPWebAppSQLTest
{
  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (PROPERTIES_FILE);

  @Test
  public void testUpdateIsVisibleImmediately () throws SMPServerException
  {
    if (SMPMetaManager.getInstance ().getBackendConnectionState ().isFalse ())
    {
      // Seems like the DB is not running
      return;
    }

    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:sg-cache-update");
    final ISMPServiceGroupManager aMgr = SMPMetaManager.getServiceGroupMgr ();

    try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      aMgr.deleteSMPServiceGroupNoEx (aPI, false);
      assertNotNull (aMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null, null, false));
      try
      {
        // Read it, so that it is surely in the cache
        ISMPServiceGroup aSG = aMgr.getSMPServiceGroupOfID (aPI);
        assertNotNull (aSG);
        assertNull (aSG.getCustomProperties ());

        // Add a custom property
        final SGCustomPropertyList aProps = new SGCustomPropertyList (new SGCustomProperty (ESGCustomPropertyType.PUBLIC,
                                                                                            "cache-test",
                                                                                            "value1"));
        assertTrue (aMgr.updateSMPServiceGroup (aPI, CSecurity.USER_ADMINISTRATOR_ID, null, aProps).isChanged ());

        aSG = aMgr.getSMPServiceGroupOfID (aPI);
        assertNotNull (aSG);
        assertNotNull (aSG.getCustomProperties ());
        assertEquals ("value1", aSG.getCustomProperties ().getValue ("cache-test"));

        // Change the value - the comparison must not be based on an outdated cached version
        final SGCustomPropertyList aProps2 = new SGCustomPropertyList (new SGCustomProperty (ESGCustomPropertyType.PUBLIC,
                                                                                             "cache-test",
                                                                                             "value2"));
        assertTrue (aMgr.updateSMPServiceGroup (aPI, CSecurity.USER_ADMINISTRATOR_ID, null, aProps2).isChanged ());
        aSG = aMgr.getSMPServiceGroupOfID (aPI);
        assertNotNull (aSG);
        assertEquals ("value2", aSG.getCustomProperties ().getValue ("cache-test"));

        // Remove the custom properties again
        assertTrue (aMgr.updateSMPServiceGroup (aPI, CSecurity.USER_ADMINISTRATOR_ID, null, null).isChanged ());
        aSG = aMgr.getSMPServiceGroupOfID (aPI);
        assertNotNull (aSG);
        assertNull (aSG.getCustomProperties ());
      }
      finally
      {
        aMgr.deleteSMPServiceGroupNoEx (aPI, false);
      }
    }
  }
}
