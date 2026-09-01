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
package com.helger.phoss.smp.domain.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;

import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.token.user.IUserToken;
import com.helger.photon.security.token.user.IUserTokenManager;
import com.helger.photon.security.user.IUser;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Test class for class {@link SMPCurrentUserIDProvider}.
 *
 * @author vinit-thummar
 */
public final class SMPCurrentUserIDProviderTest
{
  @Rule
  public final SMPServerTestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testRequestUserOverridesFallbackWithoutCreatingSession ()
  {
    try (final WebScoped aWebScoped = new WebScoped ())
    {
      final SMPCurrentUserIDProvider aProvider = new SMPCurrentUserIDProvider ( () -> "ui-user");

      assertEquals ("ui-user", aProvider.getCurrentUserID ());
      assertNull (aWebScoped.getRequestScope ().getSession (false));

      SMPCurrentUserIDProvider.setCurrentAPIUserID ("api-user");
      assertEquals ("api-user", aProvider.getCurrentUserID ());
      assertNull (aWebScoped.getRequestScope ().getSession (false));
    }
  }

  @Test
  public void testBasicAuthenticationSetsRequestUser () throws Exception
  {
    try (final WebScoped aWebScoped = new WebScoped ())
    {
      final BasicAuthClientCredentials aBasicAuth = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                     CSecurity.USER_ADMINISTRATOR_PASSWORD);
      final SMPAPICredentials aCredentials = SMPAPICredentials.createForBasicAuth (aBasicAuth);
      final IUser aUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);

      assertEquals (CSecurity.USER_ADMINISTRATOR_ID, aUser.getID ());
      assertEquals (CSecurity.USER_ADMINISTRATOR_ID, SMPCurrentUserIDProvider.INSTANCE.getCurrentUserID ());
      assertNull (aWebScoped.getRequestScope ().getSession (false));
    }
  }

  @Test
  public void testBearerAuthenticationSetsRequestUser () throws Exception
  {
    final IUser aAdmin = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aAdmin);

    final IUserTokenManager aUserTokenMgr = PhotonSecurityManager.getUserTokenMgr ();
    final IUserToken aUserToken = aUserTokenMgr.createUserToken (null, null, aAdmin, "API audit test");
    assertNotNull (aUserToken);

    try
    {
      final String sToken = aUserToken.getAccessTokenList ().getActiveTokenString ();
      assertNotNull (sToken);

      try (final WebScoped aWebScoped = new WebScoped ())
      {
        assertNull (SMPCurrentUserIDProvider.INSTANCE.getCurrentUserID ());

        final IUser aUser = SMPUserManagerPhoton.validateUserCredentials (SMPAPICredentials.createForBearerToken (sToken));
        assertEquals (CSecurity.USER_ADMINISTRATOR_ID, aUser.getID ());
        assertEquals (CSecurity.USER_ADMINISTRATOR_ID, SMPCurrentUserIDProvider.INSTANCE.getCurrentUserID ());
        assertNull (aWebScoped.getRequestScope ().getSession (false));
      }
    }
    finally
    {
      aUserTokenMgr.deleteUserToken (aUserToken.getID ());
    }
  }
}
