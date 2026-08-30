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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
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
import com.helger.phoss.smp.domain.serviceinfo.IEndpointUsageInfo;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.security.SMPCertificateHelper;
import com.helger.photon.security.CSecurity;

/**
 * Test class for class {@link SMPServiceInformationManagerMongoDB}.
 *
 * @author vinit-thummar
 */
public final class SMPServiceInformationManagerMongoDBTest
{
  private static final String TRANSPORT_PROFILE = "mongodb-test-transport-profile";

  @Rule
  public final TestRule m_aRule = new SMPServerMongoDBTestRule ();

  private static SMPEndpoint _createEndpoint (final String sID,
                                               final String sEndpointReference,
                                               final String sCertificate)
  {
    return new SMPEndpoint (sID,
                            TRANSPORT_PROFILE,
                            sEndpointReference,
                            false,
                            null,
                            null,
                            null,
                            sCertificate,
                            null,
                            null,
                            null,
                            null);
  }

  private static SMPServiceInformation _createServiceInformation (final IParticipantIdentifier aPI,
                                                                  final IDocumentTypeIdentifier aDocTypeID,
                                                                  final IProcessIdentifier aProcessID,
                                                                  final String sExtension,
                                                                  final SMPEndpoint... aEndpoints)
  {
    final SMPProcess aProcess = new SMPProcess (aProcessID, new CommonsArrayList <> (aEndpoints), null);
    return new SMPServiceInformation (aPI, aDocTypeID, new CommonsArrayList <> (aProcess), sExtension);
  }

  private static SMPServiceInformation _createServiceInformation (final IParticipantIdentifier aPI,
                                                                  final IDocumentTypeIdentifier aDocTypeID,
                                                                  final IProcessIdentifier aProcessID,
                                                                  final String sExtension)
  {
    return _createServiceInformation (aPI,
                                      aDocTypeID,
                                      aProcessID,
                                      sExtension,
                                      _createEndpoint ("mongodb-callback-endpoint", "https://example.org/as4", null));
  }

  @Test
  public void testEndpointUsageAggregation () throws SMPServerException
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInformationMgr = SMPMetaManager.getServiceInformationMgr ();

    final IParticipantIdentifier aPI1 = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                         "0088:mongodb-usage-1");
    final IParticipantIdentifier aPI2 = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                         "0088:mongodb-usage-2");
    final IDocumentTypeIdentifier aDocTypeID1 = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                  "xml::xml##mongodb-usage-1::1");
    final IDocumentTypeIdentifier aDocTypeID2 = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                  "xml::xml##mongodb-usage-2::1");
    final IProcessIdentifier aProcessID = aIdentifierFactory.createProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                                       "mongodb-usage");
    assertNotNull (aPI1);
    assertNotNull (aPI2);
    assertNotNull (aDocTypeID1);
    assertNotNull (aDocTypeID2);
    assertNotNull (aProcessID);

    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI1, true);
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI2, true);

    final long nEndpointCountBefore = aServiceInformationMgr.getEndpointCount ();
    final IEndpointUsageInfo aEmptyCertUsageBefore = aServiceInformationMgr.getEndpointCertificateUsageMap ().get ("");
    final int nEmptyCertEndpointCountBefore = aEmptyCertUsageBefore == null ? 0 : aEmptyCertUsageBefore.getEndpointCount ();
    final int nEmptyCertServiceGroupCountBefore = aEmptyCertUsageBefore == null ? 0 : aEmptyCertUsageBefore.getServiceGroupCount ();

    final String sSharedURL = "https://mongodb-usage.example/shared";
    final String sUniqueURL = "https://mongodb-usage.example/unique";
    final String sCertPEM = "-----BEGIN CERTIFICATE-----\nQUJD\n-----END CERTIFICATE-----";
    final String sCertPlain = "QUJD";
    final String sOtherCert = "REVG";
    final String sNormalizedCert = SMPCertificateHelper.getNormalizedCert (sCertPEM);
    assertEquals (sNormalizedCert, SMPCertificateHelper.getNormalizedCert (sCertPlain));

    try
    {
      assertNotNull (aServiceGroupMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID,
                                                             aPI1,
                                                             null,
                                                             null,
                                                             false));
      assertNotNull (aServiceGroupMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID,
                                                             aPI2,
                                                             null,
                                                             null,
                                                             false));

      assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (_createServiceInformation (aPI1,
                                                                                                 aDocTypeID1,
                                                                                                 aProcessID,
                                                                                                 null,
                                                                                                 _createEndpoint ("mongodb-usage-1",
                                                                                                                  sSharedURL,
                                                                                                                  sCertPEM),
                                                                                                 _createEndpoint ("mongodb-usage-2",
                                                                                                                  sSharedURL,
                                                                                                                  sCertPlain),
                                                                                                 _createEndpoint ("mongodb-usage-3",
                                                                                                                  sUniqueURL,
                                                                                                                  null)))
                                        .isSuccess ());
      assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (_createServiceInformation (aPI2,
                                                                                                 aDocTypeID2,
                                                                                                 aProcessID,
                                                                                                 null,
                                                                                                 _createEndpoint ("mongodb-usage-4",
                                                                                                                  sSharedURL,
                                                                                                                  sCertPlain),
                                                                                                 _createEndpoint ("mongodb-usage-5",
                                                                                                                  null,
                                                                                                                  sOtherCert),
                                                                                                 _createEndpoint ("mongodb-usage-6",
                                                                                                                  "",
                                                                                                                  null)))
                                        .isSuccess ());

      assertEquals (nEndpointCountBefore + 6, aServiceInformationMgr.getEndpointCount ());

      final var aURLUsage = aServiceInformationMgr.getEndpointURLUsageMap ();
      final IEndpointUsageInfo aSharedURLUsage = aURLUsage.get (sSharedURL);
      assertNotNull (aSharedURLUsage);
      assertEquals (3, aSharedURLUsage.getEndpointCount ());
      assertEquals (2, aSharedURLUsage.getServiceGroupCount ());
      assertEquals (new CommonsArrayList <> (aPI1.getURIEncoded (), aPI2.getURIEncoded ()),
                    aSharedURLUsage.getServiceGroupIDsSorted (Comparator.naturalOrder ()));
      assertEquals (1, aURLUsage.get (sUniqueURL).getEndpointCount ());
      assertFalse (aURLUsage.containsKey (""));

      final var aCertUsage = aServiceInformationMgr.getEndpointCertificateUsageMap ();
      final IEndpointUsageInfo aNormalizedCertUsage = aCertUsage.get (sNormalizedCert);
      assertNotNull (aNormalizedCertUsage);
      assertEquals (3, aNormalizedCertUsage.getEndpointCount ());
      assertEquals (2, aNormalizedCertUsage.getServiceGroupCount ());
      assertEquals (1, aCertUsage.get (SMPCertificateHelper.getNormalizedCert (sOtherCert)).getEndpointCount ());

      final IEndpointUsageInfo aEmptyCertUsageAfter = aCertUsage.get ("");
      assertNotNull (aEmptyCertUsageAfter);
      assertEquals (nEmptyCertEndpointCountBefore + 2, aEmptyCertUsageAfter.getEndpointCount ());
      assertEquals (nEmptyCertServiceGroupCountBefore + 2, aEmptyCertUsageAfter.getServiceGroupCount ());
    }
    finally
    {
      aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI1, true);
      aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI2, true);
    }
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
