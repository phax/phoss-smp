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
package com.helger.phoss.smp.domain.serviceinfo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;

/**
 * Test class for class {@link ISMPServiceInformationManager}.
 *
 * @author Philip Helger
 */
public final class ISMPServiceInformationManagerFuncTest
{
  @Rule
  public final SMPServerTestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testAll () throws SMPServerException
  {
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final IParticipantIdentifier aPI1 = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:junittest1");
    assertNotNull (aPI1);

    final IDocumentTypeIdentifier aDocTypeID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("junit::testdoc##ext::1.0");
    assertNotNull (aDocTypeID);

    final IProcessIdentifier aProcessID = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme ("junit-proc");
    assertNotNull (aProcessID);

    final String sUserID = CSecurity.USER_ADMINISTRATOR_ID;
    if (SMPMetaManager.getInstance ().getBackendConnectionState ().isFalse ())
    {
      // Failed to get DB connection. E.g. MySQL down or misconfigured.
      return;
    }

    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI1, true);
    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (sUserID, aPI1, null, true);
    assertNotNull (aSG);

    try
    {
      final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
      final XMLOffsetDateTime aEndDT = aStartDT.plusYears (1);
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

      final SMPProcess aProcess = new SMPProcess (aProcessID, new CommonsArrayList <> (aEP), "<extproc/>");

      final SMPServiceInformation aServiceInformation = new SMPServiceInformation (aPI1,
                                                                                   aDocTypeID,
                                                                                   new CommonsArrayList <> (aProcess),
                                                                                   "<extsi/>");
      assertTrue (aServiceInfoMgr.mergeSMPServiceInformation (aServiceInformation).isSuccess ());
    }
    finally
    {
      // Don't care about the result
      aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI1, true);
    }
  }
}
