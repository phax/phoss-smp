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
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.mock.MockHttpClient;
import com.helger.phoss.smp.mock.MockHttpResponse;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.audit.EAuditActionType;
import com.helger.photon.audit.IAuditItem;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.token.user.IUserToken;
import com.helger.photon.security.token.user.IUserTokenManager;
import com.helger.photon.security.user.IUser;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceGroupType;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceMetadataReferenceCollectionType;

/**
 * Verify that authenticated REST operations persist the API user in SQL audit entries.
 *
 * @author vinit-thummar
 */
public final class AuditUserInterfaceTest extends AbstractSMPWebAppSQLTest
{
  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (PROPERTIES_FILE);

  private MockHttpClient m_aClient;

  @Before
  public void before ()
  {
    m_aClient = new MockHttpClient (m_aRule.getFullURL ());
  }

  @After
  public void after ()
  {
    m_aClient.close ();
  }

  private void _testAuditUser (final String sAuthorization)
  {
    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (PID_PREFIX_9999_PHOSS +
                                                                                                                      "-audit-" +
                                                                                                                      UUID.randomUUID ());
    final String sParticipantID = aPI.getURIEncoded ();
    final String sPath = aPI.getURIPercentEncoded ();
    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    try
    {
      // Exercise the HTTP authentication and SQL persistence paths together.
      {
        final MockHttpResponse aResponse = m_aClient.put (sPath,
                                                          sAuthorization,
                                                          MockHttpClient.createXMLEntity (new SMPMarshallerServiceGroupType (),
                                                                                          aSG));
        assertEquals (aResponse.getBodyAsString (), 200, aResponse.getStatusCode ());
      }
      assertTrue (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI));

      // Read the saved audit row, not the current request's identity provider.
      // Match this operation explicitly rather than assuming the last row is ours.
      int nMatchingItems = 0;
      for (final IAuditItem aItem : PhotonSecurityManager.getAuditMgr ().getLastAuditItems (100))
        if (aItem.getType () == EAuditActionType.CREATE && aItem.getAction ().contains (sParticipantID))
        {
          assertTrue (aItem.isSuccess ());
          assertEquals (CSecurity.USER_ADMINISTRATOR_ID, aItem.getUserID ());
          ++nMatchingItems;
        }
      assertEquals ("Expected one persisted service-group creation audit entry", 1, nMatchingItems);
    }
    finally
    {
      if (SMPMetaManager.getServiceGroupMgr ().containsSMPServiceGroupWithID (aPI))
      {
        final MockHttpResponse aResponse = m_aClient.delete (sPath, sAuthorization);
        assertEquals (aResponse.getBodyAsString (), 200, aResponse.getStatusCode ());
      }
    }
  }

  @Test
  public void testBasicAuthenticationPersistsAuditUser ()
  {
    try (final WebScoped aWebScoped = new WebScoped (new MockHttpServletRequest ()))
    {
      _testAuditUser (CREDENTIALS.getRequestValue ());
    }
  }

  @Test
  public void testBearerAuthenticationPersistsAuditUser ()
  {
    try (final WebScoped aWebScoped = new WebScoped (new MockHttpServletRequest ()))
    {
      final IUser aAdmin = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
      assertNotNull (aAdmin);
      final IUserTokenManager aUserTokenMgr = PhotonSecurityManager.getUserTokenMgr ();
      final IUserToken aUserToken = aUserTokenMgr.createUserToken (null, null, aAdmin, "SQL API audit test");
      assertNotNull (aUserToken);
      try
      {
        final String sToken = aUserToken.getAccessTokenList ().getActiveTokenString ();
        assertNotNull (sToken);
        _testAuditUser ("Bearer " + sToken);
      }
      finally
      {
        aUserTokenMgr.deleteUserToken (aUserToken.getID ());
      }
    }
  }
}
