/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.domain.serviceinfo;

import org.joda.time.LocalDateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.collection.CollectionHelper;
import com.helger.datetime.PDTFactory;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.process.SimpleProcessIdentifier;
import com.helger.peppol.smpserver.SMPServerTestRule;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.photon.basic.security.CSecurity;

/**
 * Test class for class {@link ISMPServiceInformationManager}.
 *
 * @author Philip Helger
 */
public final class ISMPServiceInformationManagerTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testAll ()
  {
    final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:dummy");

    aUserMgr.createUser (CSecurity.USER_ADMINISTRATOR_ID, "bla");
    try
    {
      aServiceGroupMgr.deleteSMPServiceGroup (aPI);
      final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null);
      try
      {
        final LocalDateTime aStartDT = PDTFactory.getCurrentLocalDateTime ();
        final LocalDateTime aEndDT = aStartDT.plusYears (1);
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

        final SimpleProcessIdentifier aProcessID = SimpleProcessIdentifier.createWithDefaultScheme ("testproc");
        final SMPProcess aProcess = new SMPProcess (aProcessID, CollectionHelper.newList (aEP), "extproc");

        final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createWithDefaultScheme ("testdoctype");
        final SMPServiceInformation aServiceInformation = new SMPServiceInformation (aSG,
                                                                                     aDocTypeID,
                                                                                     CollectionHelper.newList (aProcess),
                                                                                     "extsi");
        aServiceInfoMgr.mergeSMPServiceInformation (aServiceInformation);
      }
      finally
      {
        aServiceGroupMgr.deleteSMPServiceGroup (aPI);
      }
    }
    finally
    {
      aUserMgr.deleteUser (CSecurity.USER_ADMINISTRATOR_ID);
    }
  }
}
