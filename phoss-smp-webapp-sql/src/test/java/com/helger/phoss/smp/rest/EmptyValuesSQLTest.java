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
package com.helger.phoss.smp.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.PagingSpec;
import com.helger.collection.paging.SortField;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpointHelper;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Test empty and missing values on all databases. Oracle stores an empty string as
 * <code>NULL</code>, so these values must be stored in nullable columns, and must be read back
 * consistently on all databases.
 *
 * @author Philip Helger
 */
public final class EmptyValuesSQLTest extends AbstractSMPWebAppSQLTest
{
  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (PROPERTIES_FILE);

  @Test
  public void testEndpointEmptyValues () throws SMPServerException
  {
    if (SMPMetaManager.getInstance ().getBackendConnectionState ().isFalse ())
    {
      // Seems like the DB is not running
      return;
    }

    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:empty-values");
    final IDocumentTypeIdentifier aDocTypeID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("junit::empty-values##ext::1.0");
    final IProcessIdentifier aProcessID = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme ("junit-proc");
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

    // Longer than the 4000 bytes Oracle can handle without a CLOB
    final String sOldCert = "MIIold" + "A".repeat (5000);
    final String sNewCert = "MIInew" + "B".repeat (5000);

    try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);
      assertNotNull (aServiceGroupMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null, null, true));
      try
      {
        // All optional values empty
        final SMPEndpoint aEPEmpty = new SMPEndpoint (SMPEndpointHelper.createUniqueEndpointID (),
                                                      "tp-empty",
                                                      "",
                                                      false,
                                                      "",
                                                      null,
                                                      null,
                                                      "",
                                                      "",
                                                      "",
                                                      "",
                                                      null);
        // All optional values missing, as created by OASIS BDXR SMP v2
        final SMPEndpoint aEPNull = new SMPEndpoint (SMPEndpointHelper.createUniqueEndpointID (),
                                                     "tp-null",
                                                     null,
                                                     false,
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     null);
        final SMPEndpoint aEPCert = new SMPEndpoint (SMPEndpointHelper.createUniqueEndpointID (),
                                                     "tp-cert",
                                                     "http://localhost/as4",
                                                     false,
                                                     null,
                                                     null,
                                                     null,
                                                     sOldCert,
                                                     "sd",
                                                     "tc",
                                                     null,
                                                     null);
        final SMPProcess aProcess = new SMPProcess (aProcessID,
                                                    new CommonsArrayList <> (aEPEmpty, aEPNull, aEPCert),
                                                    null);
        assertTrue (aServiceInfoMgr.mergeSMPServiceInformation (new SMPServiceInformation (aPI,
                                                                                          aDocTypeID,
                                                                                          new CommonsArrayList <> (aProcess),
                                                                                          null))
                                   .isSuccess ());

        ISMPServiceInformation aSI = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI,
                                                                                                           aDocTypeID);
        assertNotNull (aSI);
        ISMPProcess aReadProcess = aSI.getAllProcesses ().getFirstOrNull ();
        assertNotNull (aReadProcess);
        for (final String sTP : new String [] { "tp-empty", "tp-null" })
        {
          final ISMPEndpoint aEP = aReadProcess.getEndpointOfTransportProfile (sTP);
          assertNotNull (sTP, aEP);
          // Mandatory elements are read as empty strings
          assertEquals (sTP, "", aEP.getEndpointReference ());
          assertEquals (sTP, "", aEP.getCertificate ());
          assertEquals (sTP, "", aEP.getServiceDescription ());
          assertEquals (sTP, "", aEP.getTechnicalContactUrl ());
          // Optional elements are read as null
          assertNull (sTP, aEP.getMinimumAuthenticationLevel ());
          assertNull (sTP, aEP.getTechnicalInformationUrl ());
        }

        // Compare and update a CLOB
        assertEquals (1, aServiceInfoMgr.updateAllEndpointCertificates (sOldCert, sNewCert));
        aSI = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI, aDocTypeID);
        aReadProcess = aSI.getAllProcesses ().getFirstOrNull ();
        assertEquals (sNewCert, aReadProcess.getEndpointOfTransportProfile ("tp-cert").getCertificate ());
        assertEquals ("", aReadProcess.getEndpointOfTransportProfile ("tp-empty").getCertificate ());
      }
      finally
      {
        aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);
      }
    }
  }

  @Test
  public void testRedirectWithoutCertificateUIDSortedByURL () throws SMPServerException
  {
    if (SMPMetaManager.getInstance ().getBackendConnectionState ().isFalse ())
    {
      // Seems like the DB is not running
      return;
    }

    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:redirect-sort");
    final IDocumentTypeIdentifier aDocTypeID1 = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("junit::redirect-sort-1##ext::1.0");
    final IDocumentTypeIdentifier aDocTypeID2 = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("junit::redirect-sort-2##ext::1.0");
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();

    try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);
      assertNotNull (aServiceGroupMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null, null, true));
      try
      {
        // No certificate UID, like in OASIS BDXR SMP v2
        assertNotNull (aRedirectMgr.createOrUpdateSMPRedirect (aPI,
                                                               aDocTypeID1,
                                                               "https://b.example.org/redirectsorttest",
                                                               null,
                                                               null,
                                                               null));
        assertNotNull (aRedirectMgr.createOrUpdateSMPRedirect (aPI,
                                                               aDocTypeID2,
                                                               "https://a.example.org/redirectsorttest",
                                                               "suid",
                                                               null,
                                                               null));

        final ISMPRedirect aRedirect = aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aPI, aDocTypeID1);
        assertNotNull (aRedirect);
        assertNull (aRedirect.getSubjectUniqueIdentifier ());
        assertEquals (2, aRedirectMgr.getAllSMPRedirectsOfServiceGroup (aPI).size ());

        // Sort by the URL, which is a CLOB on Oracle and DB2
        ICommonsList <ISMPRedirect> aPage = aRedirectMgr.getAllSMPRedirects (new PagingSpec (0,
                                                                                             10,
                                                                                             SortField.ascending ("targeturl")),
                                                                             "redirectsorttest");
        assertEquals (2, aPage.size ());
        assertEquals ("https://a.example.org/redirectsorttest", aPage.get (0).getTargetHref ());
        aPage = aRedirectMgr.getAllSMPRedirects (new PagingSpec (0, 10, SortField.descending ("targeturl")),
                                                 "redirectsorttest");
        assertEquals (2, aPage.size ());
        assertEquals ("https://b.example.org/redirectsorttest", aPage.get (0).getTargetHref ());
        assertEquals (2, aRedirectMgr.getSMPRedirectCount ("redirectsorttest"));
      }
      finally
      {
        aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, true);
      }
    }
  }

  @Test
  public void testSMLInfoEmptyURLSuffixes ()
  {
    if (SMPMetaManager.getInstance ().getBackendConnectionState ().isFalse ())
    {
      // Seems like the DB is not running
      return;
    }

    final ISMLInfoManager aSMLInfoMgr = SMPMetaManager.getSMLInfoMgr ();
    final ISMLInfo aSMLInfo = aSMLInfoMgr.createSMLInfo ("JUnit empty suffixes",
                                                         "junit.example.org",
                                                         "https://sml.junit.example.org/manage",
                                                         "",
                                                         "",
                                                         false);
    assertNotNull (aSMLInfo);
    try
    {
      final ISMLInfo aReadSMLInfo = aSMLInfoMgr.getSMLInfoOfID (aSMLInfo.getID ());
      assertNotNull (aReadSMLInfo);
      assertEquals ("", aReadSMLInfo.getURLSuffixManageSMP ());
      assertEquals ("", aReadSMLInfo.getURLSuffixManageParticipant ());
      assertTrue (aSMLInfoMgr.getAllSMLInfos ().containsAny (x -> x.getID ().equals (aSMLInfo.getID ())));
    }
    finally
    {
      aSMLInfoMgr.deleteSMLInfo (aSMLInfo.getID ());
    }
  }
}
