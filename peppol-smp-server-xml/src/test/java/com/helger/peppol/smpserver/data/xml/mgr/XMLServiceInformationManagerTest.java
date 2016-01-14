/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import org.joda.time.LocalDateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.collection.CollectionHelper;
import com.helger.datetime.PDTFactory;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.process.SimpleProcessIdentifier;
import com.helger.peppol.smpserver.data.xml.SMPXMLTestRule;
import com.helger.peppol.smpserver.data.xml.mgr.XMLServiceInformationManager;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;
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
  public final TestRule m_aTestRule = new SMPXMLTestRule ();

  @Test
  public void testServiceRegistration ()
  {
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInformationMgr = SMPMetaManager.getServiceInformationMgr ();
    assertEquals (0, aServiceInformationMgr.getSMPServiceInformationCount ());

    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:dummy");
    aServiceGroupMgr.deleteSMPServiceGroup (aPI);
    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI, null);
    try
    {
      final LocalDateTime aStartDT = PDTFactory.getCurrentLocalDateTime ();
      final LocalDateTime aEndDT = aStartDT.plusYears (1);
      final SimpleProcessIdentifier aProcessID = SimpleProcessIdentifier.createWithDefaultScheme ("testproc");
      final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createWithDefaultScheme ("testdoctype");

      {
        // Create a new service information
        final SMPEndpoint aEP = new SMPEndpoint ("tp", "http://localhost/as2", false, "minauth", aStartDT, aEndDT, "cert", "sd", "tc", "ti", "extep");
        final SMPProcess aProcess = new SMPProcess (aProcessID, CollectionHelper.newList (aEP), "extproc");
        aServiceInformationMgr.mergeSMPServiceInformation (new SMPServiceInformation (aSG, aDocTypeID, CollectionHelper.newList (aProcess), "extsi"));

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (1, CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ()).getProcessCount ());
        assertEquals (1,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getEndpointCount ());
      }

      {
        // Replace endpoint URL with equal transport profile -> replace
        final ISMPServiceInformation aSI = aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aSG, aDocTypeID);
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
                                               "extep"));
        aServiceInformationMgr.mergeSMPServiceInformation (aSI);

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (1, CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ()).getProcessCount ());
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
        final ISMPServiceInformation aSI = aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aSG, aDocTypeID);
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
                                               "extep"));
        aServiceInformationMgr.mergeSMPServiceInformation (aSI);

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (1, CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ()).getProcessCount ());
        assertEquals (2,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getEndpointCount ());
      }

      {
        // Add endpoint with different process - add to existing
        // serviceGroup+docType part
        final ISMPServiceInformation aSI = aServiceInformationMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aSG, aDocTypeID);
        assertNotNull (aSI);
        final SMPEndpoint aEP = new SMPEndpoint ("tp", "http://localhost/as2", false, "minauth", aStartDT, aEndDT, "cert", "sd", "tc", "ti", "extep");
        aSI.addProcess (new SMPProcess (SimpleProcessIdentifier.createWithDefaultScheme ("testproc2"), CollectionHelper.newList (aEP), "extproc"));
        aServiceInformationMgr.mergeSMPServiceInformation (aSI);

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (2, CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformation ()).getProcessCount ());
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
