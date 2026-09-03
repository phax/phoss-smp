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

import org.junit.Rule;
import org.junit.Test;

import com.helger.http.CHttpHeader;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.audit.EAuditActionType;
import com.helger.photon.audit.IAuditItem;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.token.user.IUserToken;
import com.helger.photon.security.token.user.IUserTokenManager;
import com.helger.photon.security.user.IUser;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xsds.peppol.smp1.ObjectFactory;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceMetadataReferenceCollectionType;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

/**
 * Verify that authenticated REST operations persist the API user in SQL audit entries.
 *
 * @author vinit-thummar
 */
public final class AuditUserInterfaceTest extends AbstractSMPWebAppSQLTest
{
  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (PROPERTIES_FILE);

  private void _testAuditUser (final String sAuthorization)
  {
    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (PID_PREFIX_9999_PHOSS +
                                                                                                                     "-audit-" +
                                                                                                                     UUID.randomUUID ());
    final String sParticipantID = aPI.getURIEncoded ();
    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    try (final Client aClient = ClientBuilder.newClient ())
    {
      final WebTarget aTarget = aClient.target (m_aRule.getFullURL ()).path (sParticipantID);
      try
      {
        // Exercise the HTTP authentication and SQL persistence paths together.
        try (final Response aResponse = aTarget.request ()
                                              .header (CHttpHeader.AUTHORIZATION, sAuthorization)
                                              .put (Entity.xml (new ObjectFactory ().createServiceGroup (aSG))))
        {
          assertEquals (aResponse.readEntity (String.class), 200, aResponse.getStatus ());
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
          try (final Response aResponse = aTarget.request ()
                                                .header (CHttpHeader.AUTHORIZATION, sAuthorization)
                                                .delete ())
          {
            assertEquals (aResponse.readEntity (String.class), 200, aResponse.getStatus ());
          }
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
