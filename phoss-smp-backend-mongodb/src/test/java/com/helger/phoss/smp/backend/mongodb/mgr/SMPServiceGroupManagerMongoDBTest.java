/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.mgr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.phoss.smp.backend.mongodb.SMPServerMongoDBTestRule;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exception.SMPServerException;

/**
 * Test class for class {@link SMPServiceGroupManagerMongoDB}.
 *
 * @author vinit-thummar
 */
public final class SMPServiceGroupManagerMongoDBTest
{
  @Rule
  public final TestRule m_aRule = new SMPServerMongoDBTestRule ();

  @Test
  public void testGetAllServiceGroupIDs () throws SMPServerException
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aParticipantID = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                                   "0088:mongodb-service-group-ids");
    assertNotNull (aParticipantID);

    final ISMPServiceGroupManager aMgr = SMPMetaManager.getServiceGroupMgr ();
    final String sOwnerID = "mongodb-service-group-owner";
    aMgr.deleteSMPServiceGroupNoEx (aParticipantID, false);
    final ISMPServiceGroup aServiceGroup = aMgr.createSMPServiceGroup (sOwnerID,
                                                                      aParticipantID,
                                                                      null,
                                                                      null,
                                                                      false);
    try
    {
      assertTrue (aMgr.getAllSMPServiceGroupIDs ().contains (aServiceGroup.getID ()));
      assertFalse (aMgr.getAllSMPServiceGroupIDs ().contains (sOwnerID));
    }
    finally
    {
      aMgr.deleteSMPServiceGroupNoEx (aParticipantID, false);
    }
  }
}
