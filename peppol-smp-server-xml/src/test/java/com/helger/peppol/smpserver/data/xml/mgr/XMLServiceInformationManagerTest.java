/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.xml.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;
import com.helger.peppol.smpserver.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;

/**
 * Test class for class {@link XMLServiceInformationManager}.
 *
 * @author Philip Helger
 */
public final class XMLServiceInformationManagerTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testServiceRegistration ()
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
    aServiceGroupMgr.deleteSMPServiceGroup (aPI);

    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI, null);
    assertNotNull (aSG);
    try
    {
      final LocalDateTime aStartDT = PDTFactory.getCurrentLocalDateTime ();
      final LocalDateTime aEndDT = aStartDT.plusYears (1);
      final IProcessIdentifier aProcessID = aIdentifierFactory.createProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                                        "testproc");
      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DEFAULT_DOCUMENT_TYPE_SCHEME,
                                                                                                  "testdoctype");

      {
        // Create a new service information
        final SMPEndpoint aEP = new SMPEndpoint ("tp",
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
        assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (new SMPServiceInformation (aSG,
                                                                                                  aDocTypeID,
                                                                                                  new CommonsArrayList <> (aProcess),
                                                                                                  "<extsi />"))
                                          .isSuccess ());

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (1,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getProcessCount ());
        assertEquals (1,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getEndpointCount ());
      }

      {
        // Replace endpoint URL with equal transport profile -> replace
        final ISMPServiceInformation aSI = aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aSG,
                                                                                                                         aDocTypeID);
        assertNotNull (aSI);
        final ISMPProcess aProcess = aSI.getProcessOfID (aProcessID);
        assertNotNull (aProcess);
        aProcess.setEndpoint (new SMPEndpoint ("tp",
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
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getProcessCount ());
        assertEquals (1,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getEndpointCount ());
        assertEquals ("http://localhost/as2-ver2",
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getAllEndpoints ()
                                      .get (0)
                                      .getEndpointReference ());
      }

      {
        // Add endpoint with different transport profile -> added to existing
        // process
        final ISMPServiceInformation aSI = aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aSG,
                                                                                                                         aDocTypeID);
        assertNotNull (aSI);
        final ISMPProcess aProcess = aSI.getProcessOfID (aProcessID);
        assertNotNull (aProcess);
        aProcess.addEndpoint (new SMPEndpoint ("tp2",
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
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getProcessCount ());
        assertEquals (2,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getEndpointCount ());
      }

      {
        // Add endpoint with different process - add to existing
        // serviceGroup+docType part
        final ISMPServiceInformation aSI = aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aSG,
                                                                                                                         aDocTypeID);
        assertNotNull (aSI);
        final SMPEndpoint aEP = new SMPEndpoint ("tp",
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
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getProcessCount ());
        assertEquals (2,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getEndpointCount ());
        assertEquals (1,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getAllProcesses ()
                                      .get (1)
                                      .getEndpointCount ());
      }
    }
    finally
    {
      aServiceGroupMgr.deleteSMPServiceGroup (aPI);
    }
  }
}
