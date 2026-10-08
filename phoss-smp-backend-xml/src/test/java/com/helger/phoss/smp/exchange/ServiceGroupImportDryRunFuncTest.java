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
package com.helger.phoss.smp.exchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.level.EErrorLevel;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.directory.IPeppolDirectoryPushCallback;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;

/**
 * Test class for the dry run mode of class {@link ServiceGroupImport} with the XML backend.
 *
 * @author Philip Helger
 */
public final class ServiceGroupImportDryRunFuncTest
{
  private static final IPeppolDirectoryPushCallback PD_PUSH_FAIL = (@NonNull final IParticipantIdentifier aParticipantID) -> {
    fail ("Directory push must not be invoked in a dry run");
    return null;
  };

  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  private static int _getSummaryItemCount (@NonNull final ImportSummary aSummary)
  {
    final AtomicInteger aCount = new AtomicInteger (0);
    aSummary.forEach ( (eAction, nSuccess, nError) -> aCount.incrementAndGet ());
    return aCount.get ();
  }

  private static boolean _containsMessage (@NonNull final ICommonsList <ImportActionItem> aActionList,
                                           @NonNull final EErrorLevel eErrorLevel,
                                           @NonNull final String sMessagePart)
  {
    return aActionList.containsAny (x -> x.getErrorLevel () == eErrorLevel && x.getMessage ().contains (sMessagePart));
  }

  @Test
  public void testDryRun () throws SMPServerException
  {
    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();

    final IParticipantIdentifier aPI = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                       "0088:dryrun");
    final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                "xml::xml##dryrun::1");
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);

    final ISMPServiceGroup aSG = aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI, null, null, true);
    assertNotNull (aSG);
    try
    {
      assertNotNull (aRedirectMgr.createOrUpdateSMPRedirect (aPI, aDocTypeID, "target", "suid", null, null));

      final IMicroDocument aDoc = ServiceGroupExport.createExportDataXMLVer10 (new CommonsArrayList <> (aSG), false);
      final IMicroElement eRoot = aDoc.getDocumentElement ();

      // Overwrite an existing Service Group
      {
        final ICommonsList <ImportActionItem> aActionList = new CommonsArrayList <> ();
        final ImportSummary aSummary = new ImportSummary ();
        ServiceGroupImport.importXMLVer10 (eRoot,
                                           true,
                                           true,
                                           aTestUser,
                                           aServiceGroupMgr.getAllSMPServiceGroupIDs (),
                                           new CommonsHashSet <> (),
                                           PD_PUSH_FAIL,
                                           aActionList,
                                           aSummary);
        assertTrue (_containsMessage (aActionList, EErrorLevel.INFO, "Will overwrite Service Group"));
        assertTrue (_containsMessage (aActionList, EErrorLevel.INFO, "Dry run: nothing was changed"));
        assertFalse (_containsMessage (aActionList, EErrorLevel.ERROR, ""));
        assertEquals (0, _getSummaryItemCount (aSummary));

        // Nothing was changed
        assertTrue (aServiceGroupMgr.containsSMPServiceGroupWithID (aPI));
        assertNotNull (aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI, aDocTypeID));
      }

      // Skip an existing Service Group
      {
        final ICommonsList <ImportActionItem> aActionList = new CommonsArrayList <> ();
        final ImportSummary aSummary = new ImportSummary ();
        ServiceGroupImport.importXMLVer10 (eRoot,
                                           false,
                                           true,
                                           aTestUser,
                                           aServiceGroupMgr.getAllSMPServiceGroupIDs (),
                                           new CommonsHashSet <> (),
                                           PD_PUSH_FAIL,
                                           aActionList,
                                           aSummary);
        assertTrue (_containsMessage (aActionList, EErrorLevel.WARN, "Ignoring already existing Service Group"));
        assertEquals (0, _getSummaryItemCount (aSummary));
      }

      // Import a new Service Group with an unknown owner
      {
        eRoot.getFirstChildElement (CSMPExchange.ELEMENT_SERVICEGROUP).setAttribute ("ownerid", "unknown-owner");
        aServiceGroupMgr.deleteSMPServiceGroup (aPI, true);
        assertFalse (aServiceGroupMgr.containsSMPServiceGroupWithID (aPI));

        final ICommonsList <ImportActionItem> aActionList = new CommonsArrayList <> ();
        final ImportSummary aSummary = new ImportSummary ();
        ServiceGroupImport.importXMLVer10 (eRoot,
                                           false,
                                           true,
                                           aTestUser,
                                           aServiceGroupMgr.getAllSMPServiceGroupIDs (),
                                           new CommonsHashSet <> (),
                                           PD_PUSH_FAIL,
                                           aActionList,
                                           aSummary);
        assertTrue (_containsMessage (aActionList, EErrorLevel.INFO, "Will import Service Group"));
        assertTrue (_containsMessage (aActionList, EErrorLevel.WARN, "Failed to resolve stored owner 'unknown-owner'"));
        assertTrue (_containsMessage (aActionList, EErrorLevel.INFO, "Dry run: would import 1 and overwrite 0"));
        assertEquals (0, _getSummaryItemCount (aSummary));

        // Nothing was created
        assertFalse (aServiceGroupMgr.containsSMPServiceGroupWithID (aPI));
      }
    }
    finally
    {
      aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);
    }
  }
}
