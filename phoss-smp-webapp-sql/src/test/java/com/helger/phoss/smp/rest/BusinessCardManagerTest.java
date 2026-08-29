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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardName;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Test class for the SQL business card manager.
 *
 * @author vinit-thummar
 */
public final class BusinessCardManagerTest extends AbstractSMPWebAppSQLTest
{
  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (PROPERTIES_FILE);

  @Test
  public void testContainsMultiEntityBusinessCard () throws SMPServerException
  {
    if (SMPMetaManager.getInstance ().getBackendConnectionState ().isFalse ())
    {
      // Seems like MySQL is not running
      return;
    }

    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:business-card-existence");
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

    try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      ISMPBusinessCard aBusinessCard = null;
      try
      {
        assertNotNull (aServiceGroupMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID,
                                                              aPI,
                                                              null,
                                                              null,
                                                              true));
        assertFalse (aBusinessCardMgr.containsSMPBusinessCardOfID (aPI));

        final SMPBusinessCardEntity aEntity1 = new SMPBusinessCardEntity ();
        aEntity1.names ().add (new SMPBusinessCardName ("Entity 1", null));
        aEntity1.setCountryCode ("AT");
        final SMPBusinessCardEntity aEntity2 = new SMPBusinessCardEntity ();
        aEntity2.names ().add (new SMPBusinessCardName ("Entity 2", null));
        aEntity2.setCountryCode ("AT");

        aBusinessCard = aBusinessCardMgr.createOrUpdateSMPBusinessCard (aPI,
                                                                        new CommonsArrayList <> (aEntity1),
                                                                        false);
        assertNotNull (aBusinessCard);
        assertTrue (aBusinessCardMgr.containsSMPBusinessCardOfID (aPI));

        aBusinessCard = aBusinessCardMgr.createOrUpdateSMPBusinessCard (aPI,
                                                                        new CommonsArrayList <> (aEntity1,
                                                                                                 aEntity2),
                                                                        false);
        assertNotNull (aBusinessCard);
        assertTrue (aBusinessCardMgr.containsSMPBusinessCardOfID (aPI));
      }
      finally
      {
        aBusinessCardMgr.deleteSMPBusinessCard (aBusinessCard, false);
        aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);
      }
    }
  }
}
