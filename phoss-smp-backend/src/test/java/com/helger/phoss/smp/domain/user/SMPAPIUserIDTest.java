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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Rule;
import org.junit.Test;

import com.helger.http.EHttpMethod;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.phoss.smp.exception.SMPUnknownUserException;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.login.ELoginResult;
import com.helger.photon.security.login.GlobalUserIDProvider;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.login.RequestUserIDProvider;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.token.user.IUserToken;
import com.helger.photon.security.token.user.IUserTokenManager;
import com.helger.photon.security.user.IUser;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.web.scope.mgr.WebScoped;

import jakarta.servlet.http.HttpSession;

/**
 * Test the request-scoped user identity used by SMP REST authentication.
 *
 * @author vinit-thummar
 */
public final class SMPAPIUserIDTest
{
  @Rule
  public final SMPServerTestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testRequestUserWithoutCreatingSession ()
  {
    try (final WebScoped aWebScoped = new WebScoped ())
    {
      assertNull (GlobalUserIDProvider.getCurrentUserID ());
      assertNull (aWebScoped.getRequestScope ().getSession (false));

      RequestUserIDProvider.setCurrentUserID ("api-user");
      assertEquals ("api-user", GlobalUserIDProvider.getCurrentUserID ());
      assertNull (aWebScoped.getRequestScope ().getSession (false));
    }
  }

  @Test
  public void testRequestUserDoesNotLeakToNextRequest ()
  {
    try (final WebScoped aWebScoped = new WebScoped ())
    {
      RequestUserIDProvider.setCurrentUserID ("api-user");
      assertEquals ("api-user", GlobalUserIDProvider.getCurrentUserID ());
      assertNull (aWebScoped.getRequestScope ().getSession (false));
    }

    try (final WebScoped aWebScoped = new WebScoped ())
    {
      assertNull (GlobalUserIDProvider.getCurrentUserID ());
      assertNull (aWebScoped.getRequestScope ().getSession (false));
    }
  }

  @Test
  public void testRequestUserPreservesExistingSessionUser ()
  {
    final LoggedInUserManager aLoginMgr = LoggedInUserManager.getInstance ();
    HttpSession aSession = null;
    try
    {
      final MockHttpServletRequest aUIRequest = new MockHttpServletRequest (m_aTestRule.getServletContext (),
                                                                           EHttpMethod.GET,
                                                                           false);
      try (final WebScoped aWebScoped = new WebScoped (aUIRequest))
      {
        assertNull (aWebScoped.getRequestScope ().getSession (false));
        assertEquals (ELoginResult.SUCCESS,
                      aLoginMgr.loginUser (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                           CSecurity.USER_ADMINISTRATOR_PASSWORD));
        aSession = aWebScoped.getRequestScope ().getSession (false);
        assertNotNull (aSession);
        assertEquals (CSecurity.USER_ADMINISTRATOR_ID, GlobalUserIDProvider.getCurrentUserID ());

        RequestUserIDProvider.setCurrentUserID ("api-user");
        assertEquals ("api-user", GlobalUserIDProvider.getCurrentUserID ());
        assertEquals (CSecurity.USER_ADMINISTRATOR_ID, aLoginMgr.getCurrentUserID ());
        assertSame (aSession, aWebScoped.getRequestScope ().getSession (false));
      }

      // Only the UI login persists when the next request reuses the same session
      final MockHttpServletRequest aNextRequest = new MockHttpServletRequest (m_aTestRule.getServletContext (),
                                                                             EHttpMethod.GET,
                                                                             false).setSession (aSession);
      try (final WebScoped aWebScoped = new WebScoped (aNextRequest))
      {
        assertEquals (CSecurity.USER_ADMINISTRATOR_ID, GlobalUserIDProvider.getCurrentUserID ());
        assertSame (aSession, aWebScoped.getRequestScope ().getSession (false));
      }
    }
    finally
    {
      aLoginMgr.logoutUser (CSecurity.USER_ADMINISTRATOR_ID);
      if (aSession != null)
        aSession.invalidate ();
    }
  }

  @Test
  public void testInvalidCredentialsDoNotSetRequestUser ()
  {
    try (final WebScoped aWebScoped = new WebScoped ())
    {
      final BasicAuthClientCredentials aBasicAuth = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                     "incorrect-password");
      assertThrows (SMPUnauthorizedException.class,
                    () -> SMPUserManagerPhoton.validateUserCredentials (SMPAPICredentials.createForBasicAuth (aBasicAuth)));
      assertNull (GlobalUserIDProvider.getCurrentUserID ());

      assertThrows (SMPUnknownUserException.class,
                    () -> SMPUserManagerPhoton.validateUserCredentials (SMPAPICredentials.createForBearerToken ("invalid-token")));
      assertNull (GlobalUserIDProvider.getCurrentUserID ());
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
      assertEquals (CSecurity.USER_ADMINISTRATOR_ID, GlobalUserIDProvider.getCurrentUserID ());
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
        assertNull (GlobalUserIDProvider.getCurrentUserID ());

        final IUser aUser = SMPUserManagerPhoton.validateUserCredentials (SMPAPICredentials.createForBearerToken (sToken));
        assertEquals (CSecurity.USER_ADMINISTRATOR_ID, aUser.getID ());
        assertEquals (CSecurity.USER_ADMINISTRATOR_ID, GlobalUserIDProvider.getCurrentUserID ());
        assertNull (aWebScoped.getRequestScope ().getSession (false));
      }
    }
    finally
    {
      aUserTokenMgr.deleteUserToken (aUserToken.getID ());
    }
  }
}
