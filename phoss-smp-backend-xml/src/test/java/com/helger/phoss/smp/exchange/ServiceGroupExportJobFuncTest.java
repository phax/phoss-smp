/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.exchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.io.file.FileOperationManager;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.mgrs.longrun.ELongRunningJobResultType;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;

/**
 * Test class for class {@link ServiceGroupExportJob}.
 *
 * @author Philip Helger
 */
public final class ServiceGroupExportJobFuncTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testExportToFile () throws SMPServerException
  {
    // Ensure the user is present
    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();

    final IParticipantIdentifier aPI = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                       "0088:exportjob");
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);
    assertNotNull (aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI, null, null, true));

    File aExportFile = null;
    try
    {
      final ServiceGroupExportJob aJob = new ServiceGroupExportJob (false, aTestUser.getID ());
      final LongRunningJobResult aResult = aJob.createLongRunningJobResult ();
      assertNotNull (aResult);
      assertEquals (ELongRunningJobResultType.FILE, aResult.getType ());

      aExportFile = aResult.getResultFile ();
      assertNotNull (aExportFile);
      assertTrue ("The export file must exist", aExportFile.isFile ());
      assertTrue ("The export file must not be empty", aExportFile.length () > 0);
      assertEquals (ServiceGroupExportJob.getExportDirectory ().getAbsolutePath (),
                    aExportFile.getParentFile ().getAbsolutePath ());

      // The created file must be a valid export
      final IMicroDocument aDoc = MicroReader.readMicroXML (aExportFile);
      assertNotNull ("The export file must contain valid XML", aDoc);
      assertEquals (CSMPExchange.ELEMENT_SMP_DATA, aDoc.getDocumentElement ().getTagName ());
      assertEquals (1,
                    aDoc.getDocumentElement ().getAllChildElements (CSMPExchange.ELEMENT_SERVICEGROUP).size ());
    }
    finally
    {
      if (aExportFile != null)
        FileOperationManager.INSTANCE.deleteFileIfExisting (aExportFile);
      aServiceGroupMgr.deleteSMPServiceGroup (aPI, true);
    }
  }
}
