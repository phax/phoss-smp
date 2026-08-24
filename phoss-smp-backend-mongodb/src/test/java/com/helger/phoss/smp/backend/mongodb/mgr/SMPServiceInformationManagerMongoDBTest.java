/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.backend.mongodb.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.phoss.smp.backend.mongodb.SMPServerMongoDBTestRule;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.photon.security.CSecurity;

/**
 * Test class for class {@link SMPServiceInformationManagerMongoDB}.
 *
 * @author vinit-thummar
 */
public final class SMPServiceInformationManagerMongoDBTest
{
  @Rule
  public final TestRule m_aRule = new SMPServerMongoDBTestRule ();

  private static SMPServiceInformation _createServiceInformation (final IParticipantIdentifier aPI,
                                                                  final IDocumentTypeIdentifier aDocTypeID,
                                                                  final IProcessIdentifier aProcessID,
                                                                  final String sExtension)
  {
    final SMPEndpoint aEndpoint = new SMPEndpoint ("mongodb-callback-endpoint",
                                                   "mongodb-callback-transport-profile",
                                                   "https://example.org/as4",
                                                   false,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null);
    final SMPProcess aProcess = new SMPProcess (aProcessID, new CommonsArrayList <> (aEndpoint), null);
    return new SMPServiceInformation (aPI, aDocTypeID, new CommonsArrayList <> (aProcess), sExtension);
  }

  @Test
  public void testMergeReplacementFiresUpdateCallback () throws SMPServerException
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInformationMgr = SMPMetaManager.getServiceInformationMgr ();

    final IParticipantIdentifier aPI = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                        "0088:mongodb-callback");
    assertNotNull (aPI);
    final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                "xml::xml##mongodb-callback::1");
    assertNotNull (aDocTypeID);
    final IProcessIdentifier aProcessID = aIdentifierFactory.createProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                                      "mongodb-callback");
    assertNotNull (aProcessID);

    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);
    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID,
                                                                        aPI,
                                                                        null,
                                                                        null,
                                                                        true);
    assertNotNull (aSG);

    final AtomicInteger aCreatedCount = new AtomicInteger ();
    final AtomicInteger aUpdatedCount = new AtomicInteger ();
    final ISMPServiceInformationCallback aCallback = new ISMPServiceInformationCallback ()
    {
      @Override
      public void onSMPServiceInformationCreated (final ISMPServiceInformation aServiceInformation)
      {
        aCreatedCount.incrementAndGet ();
      }

      @Override
      public void onSMPServiceInformationUpdated (final ISMPServiceInformation aServiceInformation)
      {
        aUpdatedCount.incrementAndGet ();
      }
    };
    aServiceInformationMgr.serviceInformationCallbacks ().add (aCallback);
    try
    {
      assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (_createServiceInformation (aPI,
                                                                                                aDocTypeID,
                                                                                                aProcessID,
                                                                                                "<ext1/>"))
                                        .isSuccess ());
      assertEquals (1, aCreatedCount.get ());
      assertEquals (0, aUpdatedCount.get ());

      assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (_createServiceInformation (aPI,
                                                                                                aDocTypeID,
                                                                                                aProcessID,
                                                                                                "<ext2/>"))
                                        .isSuccess ());
      assertEquals (1, aCreatedCount.get ());
      assertEquals (1, aUpdatedCount.get ());
    }
    finally
    {
      aServiceInformationMgr.serviceInformationCallbacks ().removeObject (aCallback);
      aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);
    }
  }
}
