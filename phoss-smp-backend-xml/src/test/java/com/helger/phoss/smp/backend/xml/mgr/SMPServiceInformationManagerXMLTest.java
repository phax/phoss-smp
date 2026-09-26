/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.xml.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.CollectionFind;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.audit.EAuditActionType;
import com.helger.photon.audit.IAuditor;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;

/**
 * Test class for class {@link SMPServiceInformationManagerXML}.
 *
 * @author Philip Helger
 */
public final class SMPServiceInformationManagerXMLTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testServiceRegistration () throws SMPServerException
  {
    // Ensure the user is present
    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInformationMgr = SMPMetaManager.getServiceInformationMgr ();
    assertEquals (0, aServiceInformationMgr.getSMPServiceInformationCount ());

    // Delete existing service group
    final IParticipantIdentifier aPI = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                       "0088:dummy");
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);

    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI, null, null, true);
    assertNotNull (aSG);
    try
    {
      final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
      final XMLOffsetDateTime aEndDT = aStartDT.plusYears (1);
      final IProcessIdentifier aProcessID = aIdentifierFactory.createProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                                        "testproc");
      assertNotNull (aProcessID);
      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                  "xml::xml##testdoctype::1");
      assertNotNull (aDocTypeID);

      {
        // Create a new service information
        final SMPEndpoint aEP = new SMPEndpoint ("epid",
                                                 "tp",
                                                 "http://localhost/as2",
                                                 false,
                                                 "minauth",
                                                 aStartDT,
                                                 aEndDT,
                                                 "cert",
                                                 "sd",
                                                 "tc",
                                                 "ti",
                                                 "<extep />");
        final SMPProcess aProcess = new SMPProcess (aProcessID, new CommonsArrayList <> (aEP), "<extproc />");
        assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (new SMPServiceInformation (aPI,
                                                                                                  aDocTypeID,
                                                                                                  new CommonsArrayList <> (aProcess),
                                                                                                  "<extsi />"))
                                          .isSuccess ());

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (1,
                      CollectionFind.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                    .getProcessCount ());
        assertEquals (1,
                      CollectionFind.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                    .getAllProcesses ()
                                    .get (0)
                                    .getEndpointCount ());
      }

      {
        // A REST-like replacement uses a different Java object and must still
        // be handled and audited as an update.
        final SMPEndpoint aEP = new SMPEndpoint ("epid",
                                                 "tp",
                                                 "http://localhost/as2-rest",
                                                 false,
                                                 "minauth",
                                                 aStartDT,
                                                 aEndDT,
                                                 "cert",
                                                 "sd",
                                                 "tc",
                                                 "ti",
                                                 "<extep />");
        final SMPProcess aProcess = new SMPProcess (aProcessID, new CommonsArrayList <> (aEP), "<extproc />");
        final IAuditor aOldAuditor = AuditHelper.getAuditor ();
        final ICommonsList <EAuditActionType> aAuditActions = new CommonsArrayList <> ();
        try
        {
          AuditHelper.setAuditor ((eActionType, eSuccess, aActionObjectType, sAction, aArgs) -> {
            if (SMPServiceInformation.OT.equals (aActionObjectType))
              aAuditActions.add (eActionType);
          });
          assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (new SMPServiceInformation (aPI,
                                                                                                    aDocTypeID,
                                                                                                    new CommonsArrayList <> (aProcess),
                                                                                                    "<extsi-rest />"))
                                            .isSuccess ());
          assertEquals (new CommonsArrayList <> (EAuditActionType.MODIFY), aAuditActions);
        }
        finally
        {
          AuditHelper.setAuditor (aOldAuditor);
        }

        assertEquals ("http://localhost/as2-rest",
                      CollectionFind.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                    .getAllProcesses ()
                                    .get (0)
                                    .getAllEndpoints ()
                                    .get (0)
                                    .getEndpointReference ());
      }

      {
        // Replace endpoint URL with equal transport profile -> replace
        final ISMPServiceInformation aSI = aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI,
                                                                                                                         aDocTypeID);
        assertNotNull (aSI);
        final ISMPProcess aProcess = aSI.getProcessOfID (aProcessID);
        assertNotNull (aProcess);
        aProcess.createOrUpdateEndpoint (new SMPEndpoint ("epid",
                                                          "tp",
                                                          "http://localhost/as2-ver2",
                                                          false,
                                                          "minauth",
                                                          aStartDT,
                                                          aEndDT,
                                                          "cert",
                                                          "sd",
                                                          "tc",
                                                          "ti",
                                                          "<extep />"));
        assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (aSI).isSuccess ());

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (1,
                      CollectionFind.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                    .getProcessCount ());
        assertEquals (1,
                      CollectionFind.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                    .getAllProcesses ()
                                    .get (0)
                                    .getEndpointCount ());
        assertEquals ("http://localhost/as2-ver2",
                      CollectionFind.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                    .getAllProcesses ()
                                    .get (0)
                                    .getAllEndpoints ()
                                    .get (0)
                                    .getEndpointReference ());
      }

      {
        // Add endpoint with different transport profile -> added to existing
        // process
        final ISMPServiceInformation aSI = aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI,
                                                                                                                         aDocTypeID);
        assertNotNull (aSI);
        final ISMPProcess aProcess = aSI.getProcessOfID (aProcessID);
        assertNotNull (aProcess);
        aProcess.addEndpoint (new SMPEndpoint ("epid",
                                               "tp2",
                                               "http://localhost/as2-tp2",
                                               false,
                                               "minauth",
                                               aStartDT,
                                               aEndDT,
                                               "cert",
                                               "sd",
                                               "tc",
                                               "ti",
                                               "<extep />"));
        assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (aSI).isSuccess ());

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (1,
                      CollectionFind.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                    .getProcessCount ());
        assertEquals (2,
                      CollectionFind.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                    .getAllProcesses ()
                                    .get (0)
                                    .getEndpointCount ());
      }

      {
        // Add endpoint with different process - add to existing
        // serviceGroup+docType part
        final ISMPServiceInformation aSI = aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI,
                                                                                                                         aDocTypeID);
        assertNotNull (aSI);
        final SMPEndpoint aEP = new SMPEndpoint ("epid",
                                                 "tp",
                                                 "http://localhost/as2",
                                                 false,
                                                 "minauth",
                                                 aStartDT,
                                                 aEndDT,
                                                 "cert",
                                                 "sd",
                                                 "tc",
                                                 "ti",
                                                 "<extep />");
        aSI.addProcess (new SMPProcess (PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme ("testproc2"),
                                        new CommonsArrayList <> (aEP),
                                        "<extproc />"));
        assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (aSI).isSuccess ());

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (2,
                      CollectionFind.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                    .getProcessCount ());
        assertEquals (2,
                      CollectionFind.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                    .getAllProcesses ()
                                    .get (0)
                                    .getEndpointCount ());
        assertEquals (1,
                      CollectionFind.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                    .getAllProcesses ()
                                    .get (1)
                                    .getEndpointCount ());
      }
    }
    finally
    {
      aServiceGroupMgr.deleteSMPServiceGroup (aPI, true);
    }
  }

  @Test
  public void testDeleteEndpointAndProcessFiresUpdateCallback () throws SMPServerException
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInformationMgr = SMPMetaManager.getServiceInformationMgr ();

    final IParticipantIdentifier aPI = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                       "0088:xml-callback");
    assertNotNull (aPI);
    final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                 "xml::xml##xml-callback::1");
    assertNotNull (aDocTypeID);
    final IProcessIdentifier aProcessID = aIdentifierFactory.createProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                                      "xml-callback");
    assertNotNull (aProcessID);

    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);
    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID,
                                                                         aPI,
                                                                         null,
                                                                         null,
                                                                         true);
    assertNotNull (aSG);

    final AtomicInteger aUpdatedCount = new AtomicInteger ();
    final ISMPServiceInformationCallback aCallback = new ISMPServiceInformationCallback ()
    {
      @Override
      public void onSMPServiceInformationUpdated (final ISMPServiceInformation aServiceInformation)
      {
        aUpdatedCount.incrementAndGet ();
      }
    };
    aServiceInformationMgr.serviceInformationCallbacks ().add (aCallback);
    try
    {
      final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
      final XMLOffsetDateTime aEndDT = aStartDT.plusYears (1);
      final SMPEndpoint aEP1 = new SMPEndpoint ("ep1",
                                                "tp1",
                                                "http://localhost/tp1",
                                                false,
                                                "minauth",
                                                aStartDT,
                                                aEndDT,
                                                "cert",
                                                "sd",
                                                "tc",
                                                "ti",
                                                null);
      final SMPEndpoint aEP2 = new SMPEndpoint ("ep2",
                                                "tp2",
                                                "http://localhost/tp2",
                                                false,
                                                "minauth",
                                                aStartDT,
                                                aEndDT,
                                                "cert",
                                                "sd",
                                                "tc",
                                                "ti",
                                                null);
      final SMPProcess aProcess = new SMPProcess (aProcessID, new CommonsArrayList <> (aEP1, aEP2), null);
      assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (new SMPServiceInformation (aPI,
                                                                                                 aDocTypeID,
                                                                                                 new CommonsArrayList <> (aProcess),
                                                                                                 null))
                                        .isSuccess ());
      assertEquals (0, aUpdatedCount.get ());

      // Delete a single endpoint - as done in the Endpoint Tree page
      final ISMPServiceInformation aSI = aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI,
                                                                                                                       aDocTypeID);
      assertNotNull (aSI);
      final ISMPProcess aSelectedProcess = aSI.getProcessOfID (aProcessID);
      assertNotNull (aSelectedProcess);
      assertTrue (aSelectedProcess.deleteEndpointByID ("ep1").isChanged ());
      assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (aSI).isSuccess ());
      assertEquals (1, aUpdatedCount.get ());

      // Delete the whole process - as done in the Endpoint Tree page
      assertTrue (aServiceInformationMgr.deleteSMPProcess (aSI, aSelectedProcess).isChanged ());
      assertEquals (2, aUpdatedCount.get ());
    }
    finally
    {
      aServiceInformationMgr.serviceInformationCallbacks ().removeObject (aCallback);
      aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);
    }
  }

  @NonNull
  private static SMPServiceInformation _createServiceInfo (@NonNull final IParticipantIdentifier aParticipantID,
                                                           @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                           @NonNull final IProcessIdentifier aProcessID,
                                                           @NonNull final String sEndpointReference)
  {
    final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
    final SMPEndpoint aEP = new SMPEndpoint ("epid",
                                             "tp",
                                             sEndpointReference,
                                             false,
                                             "minauth",
                                             aStartDT,
                                             aStartDT.plusYears (1),
                                             "cert",
                                             "sd",
                                             "tc",
                                             "ti",
                                             null);
    final SMPProcess aProcess = new SMPProcess (aProcessID, new CommonsArrayList <> (aEP), null);
    return new SMPServiceInformation (aParticipantID, aDocTypeID, new CommonsArrayList <> (aProcess), null);
  }

  @Test
  public void testGetServiceInformationOfServiceGroupAndDocumentType () throws SMPServerException
  {
    // Ensure the user is present
    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInformationMgr = SMPMetaManager.getServiceInformationMgr ();
    assertEquals (0, aServiceInformationMgr.getSMPServiceInformationCount ());

    final IParticipantIdentifier aPI1 = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                        "0088:xml-lookup-1");
    assertNotNull (aPI1);
    final IParticipantIdentifier aPI2 = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                        "0088:xml-lookup-2");
    assertNotNull (aPI2);
    final IProcessIdentifier aProcessID = aIdentifierFactory.createProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                                      "xml-lookup");
    assertNotNull (aProcessID);
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI1, true);
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI2, true);

    assertNotNull (aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI1, null, null, true));
    try
    {
      assertNotNull (aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI2, null, null, true));
      try
      {
        // Create a bunch of unrelated registrations
        final int nServiceInfos = 20;
        for (int i = 0; i < nServiceInfos; ++i)
        {
          final IDocumentTypeIdentifier aCurDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                         "xml::xml##lookup" +
                                                                                                                                                                     i +
                                                                                                                                                                     "::1");
          assertNotNull (aCurDocTypeID);
          assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (_createServiceInfo (aPI1,
                                                                                             aCurDocTypeID,
                                                                                             aProcessID,
                                                                                             "http://localhost/ep" + i))
                                            .isSuccess ());
        }
        assertEquals (nServiceInfos, aServiceInformationMgr.getSMPServiceInformationCount ());

        // Exact hit in the middle of many unrelated registrations
        final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                    "xml::xml##lookup7::1");
        assertNotNull (aDocTypeID);
        final ISMPServiceInformation aSI = aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI1,
                                                                                                                         aDocTypeID);
        assertNotNull (aSI);
        assertEquals ("http://localhost/ep7",
                      aSI.getAllProcesses ().get (0).getAllEndpoints ().get (0).getEndpointReference ());

        // Unknown document type
        final IDocumentTypeIdentifier aUnknownDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                           "xml::xml##lookup-unknown::1");
        assertNotNull (aUnknownDocTypeID);
        assertNull (aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI1,
                                                                                                  aUnknownDocTypeID));

        // Same document type, but a different participant
        assertNull (aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI2, aDocTypeID));

        // A participant ID that only matches after the unification
        final IParticipantIdentifier aNonUnifiedPI = new SimpleParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                      "0088:XML-Lookup-1");
        assertNotNull (aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aNonUnifiedPI,
                                                                                                     aDocTypeID));

        // Undefined parameters
        assertNull (aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (null, aDocTypeID));
        assertNull (aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI1, null));
      }
      finally
      {
        aServiceGroupMgr.deleteSMPServiceGroup (aPI2, true);
      }
    }
    finally
    {
      aServiceGroupMgr.deleteSMPServiceGroup (aPI1, true);
    }
  }
}
