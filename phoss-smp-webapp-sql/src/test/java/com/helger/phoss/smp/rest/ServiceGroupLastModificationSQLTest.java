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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.datetime.domain.IHasLastModificationDateTime;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpointHelper;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.domain.sgprops.ESGCustomPropertyType;
import com.helger.phoss.smp.domain.sgprops.SGCustomProperty;
import com.helger.phoss.smp.domain.sgprops.SGCustomPropertyList;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Test that every write path of the SQL backend updates the last modification of the Service
 * Group, which is used for the HTTP header <code>Last-Modified</code>.
 *
 * @author Philip Helger
 */
public final class ServiceGroupLastModificationSQLTest extends AbstractSMPWebAppSQLTest
{
  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (PROPERTIES_FILE);

  private static LocalDateTime _getLastModDT (@NonNull final IParticipantIdentifier aPI)
  {
    final IHasLastModificationDateTime aLastMod = SMPMetaManager.getServiceGroupMgr ()
                                                                .getSMPServiceGroupLastModification (aPI);
    return aLastMod == null ? null : aLastMod.getLastModificationDateTime ();
  }

  @NonNull
  private static LocalDateTime _assertModified (@NonNull final IParticipantIdentifier aPI,
                                                @NonNull final LocalDateTime aPrevLastModDT)
  {
    final LocalDateTime ret = _getLastModDT (aPI);
    assertNotNull (ret);
    assertTrue (ret + " is not after " + aPrevLastModDT, ret.isAfter (aPrevLastModDT));
    // The last modification has millisecond precision
    ThreadHelper.sleep (Duration.ofMillis (5));
    return ret;
  }

  @Test
  public void testAllWritePaths () throws SMPServerException
  {
    if (SMPMetaManager.getInstance ().getBackendConnectionState ().isFalse ())
    {
      // Seems like the DB is not running
      return;
    }

    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:last-modification");
    final IDocumentTypeIdentifier aDocTypeID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("junit::last-modification##ext::1.0");
    final IDocumentTypeIdentifier aDocTypeID2 = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("junit::last-modification2##ext::1.0");
    final IProcessIdentifier aProcessID = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme ("junit-proc");
    final IProcessIdentifier aProcessID2 = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme ("junit-proc2");
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();

    // Unique per test run, because the certificate bulk change is not limited to a participant
    final String sOldCert = "MIIlastmodold" + System.nanoTime ();
    final String sNewCert = "MIIlastmodnew" + System.nanoTime ();

    try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, false);
      assertNull (aServiceGroupMgr.getSMPServiceGroupLastModification (aPI));

      final LocalDateTime aStart = LocalDateTime.MIN;
      assertNotNull (aServiceGroupMgr.createSMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null, null, false));
      try
      {
        LocalDateTime aLastModDT = _assertModified (aPI, aStart);
        // The object read from the backend contains it as well
        assertEquals (aLastModDT, aServiceGroupMgr.getSMPServiceGroupOfID (aPI).getLastModificationDateTime ());

        // Service Group update
        final SGCustomPropertyList aProps = new SGCustomPropertyList (new SGCustomProperty (ESGCustomPropertyType.PUBLIC,
                                                                                            "lastmod-test",
                                                                                            "value1"));
        assertTrue (aServiceGroupMgr.updateSMPServiceGroup (aPI, CSecurity.USER_ADMINISTRATOR_ID, null, aProps)
                                    .isChanged ());
        aLastModDT = _assertModified (aPI, aLastModDT);

        // Unchanged Service Group update
        assertTrue (aServiceGroupMgr.updateSMPServiceGroup (aPI, CSecurity.USER_ADMINISTRATOR_ID, null, aProps)
                                    .isUnchanged ());
        assertEquals (aLastModDT, _getLastModDT (aPI));

        // Create Service Information
        final SMPEndpoint aEP = new SMPEndpoint (SMPEndpointHelper.createUniqueEndpointID (),
                                                 "tp",
                                                 "http://localhost/lastmod-old",
                                                 false,
                                                 null,
                                                 null,
                                                 null,
                                                 sOldCert,
                                                 "sd",
                                                 "http://localhost/contact-old",
                                                 null,
                                                 null);
        final SMPEndpoint aEP2 = new SMPEndpoint (SMPEndpointHelper.createUniqueEndpointID (),
                                                  "tp",
                                                  "http://localhost/lastmod-old",
                                                  false,
                                                  null,
                                                  null,
                                                  null,
                                                  sOldCert,
                                                  "sd",
                                                  "http://localhost/contact-old",
                                                  null,
                                                  null);
        assertTrue (aServiceInfoMgr.mergeSMPServiceInformation (new SMPServiceInformation (aPI,
                                                                                           aDocTypeID,
                                                                                           new CommonsArrayList <> (new SMPProcess (aProcessID,
                                                                                                                                    new CommonsArrayList <> (aEP),
                                                                                                                                    null),
                                                                                                                    new SMPProcess (aProcessID2,
                                                                                                                                    new CommonsArrayList <> (aEP2),
                                                                                                                                    null)),
                                                                                           null))
                                   .isSuccess ());
        aLastModDT = _assertModified (aPI, aLastModDT);

        assertTrue (aServiceInfoMgr.containsSMPServiceInformationWithEndpoint (aPI, aDocTypeID));
        assertFalse (aServiceInfoMgr.containsSMPServiceInformationWithEndpoint (aPI, aDocTypeID2));
        assertFalse (aRedirectMgr.containsSMPRedirect (aPI, aDocTypeID2));
        
        // Bulk changes
        assertEquals (2, aServiceInfoMgr.updateAllEndpointURLs (aPI, "http://localhost/lastmod-old", "http://localhost/lastmod-new"));
        aLastModDT = _assertModified (aPI, aLastModDT);

        assertEquals (2, aServiceInfoMgr.updateAllEndpointTechnicalContactUrls (aPI,
                                                                               "http://localhost/contact-old",
                                                                               "http://localhost/contact-new"));
        aLastModDT = _assertModified (aPI, aLastModDT);

        assertEquals (2, aServiceInfoMgr.updateAllEndpointCertificates (sOldCert, sNewCert));
        aLastModDT = _assertModified (aPI, aLastModDT);

        assertEquals (2,
                      aServiceInfoMgr.updateAllEndpointServiceValidity (aDocTypeID,
                                                                        aPI,
                                                                        true,
                                                                        XMLOffsetDateTime.now (),
                                                                        false,
                                                                        null));
        aLastModDT = _assertModified (aPI, aLastModDT);

        // Nothing matches - no change
        assertEquals (0, aServiceInfoMgr.updateAllEndpointURLs (aPI, "http://localhost/lastmod-old", "http://localhost/x"));
        assertEquals (aLastModDT, _getLastModDT (aPI));

        // Delete a single process
        final ISMPServiceInformation aSI = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI,
                                                                                                                  aDocTypeID);
        assertNotNull (aSI);
        assertTrue (aServiceInfoMgr.deleteSMPProcess (aSI, aSI.getProcessOfID (aProcessID2)).isChanged ());
        aLastModDT = _assertModified (aPI, aLastModDT);

        // Delete the Service Information
        assertTrue (aServiceInfoMgr.deleteSMPServiceInformation (aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aPI,
                                                                                                                                         aDocTypeID))
                                   .isChanged ());
        aLastModDT = _assertModified (aPI, aLastModDT);

        assertFalse (aServiceInfoMgr.containsSMPServiceInformationWithEndpoint (aPI, aDocTypeID));
        
        // Redirect create, update and delete
        assertNotNull (aRedirectMgr.createOrUpdateSMPRedirect (aPI, aDocTypeID2, "http://localhost/other-smp", "CN=x", null, null));
        aLastModDT = _assertModified (aPI, aLastModDT);

        assertTrue (aRedirectMgr.containsSMPRedirect (aPI, aDocTypeID2));
        
        final ISMPRedirect aRedirect = aRedirectMgr.createOrUpdateSMPRedirect (aPI,
                                                                               aDocTypeID2,
                                                                               "http://localhost/other-smp2",
                                                                               "CN=x",
                                                                               null,
                                                                               null);
        assertNotNull (aRedirect);
        aLastModDT = _assertModified (aPI, aLastModDT);

        assertTrue (aRedirectMgr.deleteSMPRedirect (aRedirect).isChanged ());
        aLastModDT = _assertModified (aPI, aLastModDT);

        assertFalse (aRedirectMgr.containsSMPRedirect (aPI, aDocTypeID2));
        
        // Explicit update
        assertTrue (aServiceGroupMgr.updateSMPServiceGroupLastModification (aPI).isChanged ());
        _assertModified (aPI, aLastModDT);
      }
      finally
      {
        aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI, false);
      }
      assertNull (aServiceGroupMgr.getSMPServiceGroupLastModification (aPI));
    }
  }
}
