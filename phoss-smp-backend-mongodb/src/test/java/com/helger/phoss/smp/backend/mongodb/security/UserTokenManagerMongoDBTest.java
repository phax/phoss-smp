/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.token.user.IUserToken;
import com.helger.photon.security.user.IUser;

public final class UserTokenManagerMongoDBTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testUserTokenManagerCrud ()
  {
    try (final UserManagerMongoDB aUserMgr = new UserManagerMongoDB ();
         final UserTokenManagerMongoDB aUserTokenMgr = new UserTokenManagerMongoDB (aUserMgr))
    {
      final IUser aUser = aUserMgr.createNewUser ("UserTokenTestUser",
                                                  "test@usertoken.test",
                                                  "im a super secure password",
                                                  "First Name",
                                                  "Last Name",
                                                  "Description",
                                                  Locale.GERMAN,
                                                  Map.of ("foo", "bar"),
                                                  false);
      assertNotNull (aUser);

      try
      {
        final IUserToken aUserToken = aUserTokenMgr.createUserToken ("asd",
                                                                     Map.of ("meta", "data"),
                                                                     aUser,
                                                                     "description");
        assertNotNull (aUserToken);
        final String sUserTokenID = aUserToken.getID ();

        try
        {
          assertEquals (aUserToken, aUserTokenMgr.getUserTokenOfID (sUserTokenID));
          assertTrue (aUserTokenMgr.getAllActiveUserTokens ().contains (aUserToken));

          assertTrue (aUserTokenMgr.updateUserToken (sUserTokenID, Map.of ("foo", "bar"), "dummy token").isChanged ());

          final IUserToken aResolvedUserToken = aUserTokenMgr.getUserTokenOfID (sUserTokenID);
          assertNotNull (aResolvedUserToken);
          assertEquals ("dummy token", aResolvedUserToken.getDescription ());
          assertTrue (aResolvedUserToken.attrs ().containsKey ("foo"));

          assertFalse (aUserTokenMgr.isAccessTokenUsed (aResolvedUserToken.getID ()));

          assertTrue (aUserTokenMgr.createNewAccessToken (aResolvedUserToken.getID (),
                                                          aUser.getID (),
                                                          LocalDateTime.now (),
                                                          "reason",
                                                          "tokenString").isChanged ());
          final IUserToken aUserTokenOfString = aUserTokenMgr.getUserTokenOfTokenString ("tokenString");
          assertNotNull (aUserTokenOfString);

          assertTrue (aUserTokenMgr.revokeAccessToken (sUserTokenID, aUser.getID (), LocalDateTime.now (), "revoked")
                                   .isChanged ());
          assertFalse (aUserTokenMgr.isAccessTokenUsed (aResolvedUserToken.getID ()));
        }
        finally
        {
          aUserTokenMgr.internalDeleteUserTokenNotRecoverable (sUserTokenID);
        }
      }
      finally
      {
        aUserMgr.internalDeleteUserNotRecoverable (aUser.getID ());
      }
    }
  }
}
