/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.data.xml;

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
import com.helger.peppol.smpserver.data.xml.mgr.XMLServiceInformationManager;
import com.helger.peppol.smpserver.domain.MetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;
import com.helger.photon.basic.security.AccessManager;
import com.helger.photon.basic.security.CSecurity;
import com.helger.photon.basic.security.user.IUser;

/**
 * Test class for class {@link XMLServiceInformationManager}.
 *
 * @author Philip Helger
 */
public final class XMLServiceInformationManagerTest
{
  @Rule
  public final TestRule m_aTestRule = new PhotonBasicWebTestRule ();

  @Test
  public void testServiceRegistration ()
  {
    final ISMPServiceGroupManager aServiceGroupMgr = MetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInformationMgr = MetaManager.getServiceInformationMgr ();
    assertEquals (0, aServiceInformationMgr.getSMPServiceInformationCount ());

    final IUser aTestUser = AccessManager.getInstance ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
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
                                                 "extep");
        final SMPProcess aProcess = new SMPProcess (aProcessID, CollectionHelper.newList (aEP), "extproc");
        aServiceInformationMgr.createOrUpdateSMPServiceInformation (new SMPServiceInformation (aSG,
                                                                                               aDocTypeID,
                                                                                               CollectionHelper.newList (aProcess),
                                                                                               "extsi"));

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (1,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformations ())
                                      .getProcessCount ());
        assertEquals (1,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformations ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getEndpointCount ());
      }

      {
        // Replace endpoint URL with equal transport profile -> replace
        final SMPEndpoint aEP = new SMPEndpoint ("tp",
                                                 "http://localhost/as2-ver2",
                                                 false,
                                                 "minauth",
                                                 aStartDT,
                                                 aEndDT,
                                                 "cert",
                                                 "sd",
                                                 "tc",
                                                 "ti",
                                                 "extep");
        final SMPProcess aProcess = new SMPProcess (aProcessID, CollectionHelper.newList (aEP), "extproc");
        aServiceInformationMgr.createOrUpdateSMPServiceInformation (new SMPServiceInformation (aSG,
                                                                                               aDocTypeID,
                                                                                               CollectionHelper.newList (aProcess),
                                                                                               "extsi"));

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (1,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformations ())
                                      .getProcessCount ());
        assertEquals (1,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformations ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getEndpointCount ());
        assertEquals ("http://localhost/as2-ver2",
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformations ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getAllEndpoints ()
                                      .get (0)
                                      .getEndpointReference ());
      }

      {
        // Add endpoint with different transport profile -> added to existing
        // process
        final SMPEndpoint aEP = new SMPEndpoint ("tp2",
                                                 "http://localhost/as2-tp2",
                                                 false,
                                                 "minauth",
                                                 aStartDT,
                                                 aEndDT,
                                                 "cert",
                                                 "sd",
                                                 "tc",
                                                 "ti",
                                                 "extep");
        final SMPProcess aProcess = new SMPProcess (aProcessID, CollectionHelper.newList (aEP), "extproc");
        aServiceInformationMgr.createOrUpdateSMPServiceInformation (new SMPServiceInformation (aSG,
                                                                                               aDocTypeID,
                                                                                               CollectionHelper.newList (aProcess),
                                                                                               "extsi"));

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (1,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformations ())
                                      .getProcessCount ());
        assertEquals (2,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformations ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getEndpointCount ());
      }

      {
        // Add endpoint with different process - add to existing
        // serviceGroup+docType part
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
                                                 "extep");
        final SMPProcess aProcess = new SMPProcess (SimpleProcessIdentifier.createWithDefaultScheme ("testproc2"),
                                                    CollectionHelper.newList (aEP),
                                                    "extproc");
        aServiceInformationMgr.createOrUpdateSMPServiceInformation (new SMPServiceInformation (aSG,
                                                                                               aDocTypeID,
                                                                                               CollectionHelper.newList (aProcess),
                                                                                               "extsi"));

        assertEquals (1, aServiceInformationMgr.getSMPServiceInformationCount ());
        assertEquals (2,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformations ())
                                      .getProcessCount ());
        assertEquals (2,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformations ())
                                      .getAllProcesses ()
                                      .get (0)
                                      .getEndpointCount ());
        assertEquals (1,
                      CollectionHelper.getFirstElement (aServiceInformationMgr.getAllSMPServiceInformations ())
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
