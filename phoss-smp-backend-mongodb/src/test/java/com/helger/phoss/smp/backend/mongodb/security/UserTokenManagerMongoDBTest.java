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
import com.helger.photon.security.token.user.UserToken;
import com.helger.photon.security.user.IUser;

public final class UserTokenManagerMongoDBTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testUserTokenManagerCrud ()
  {
    try (final UserManagerMongoDB aUserMger = new UserManagerMongoDB ();
         final UserTokenManagerMongoDB aUserTokenMgr = new UserTokenManagerMongoDB (aUserMger))
    {
      aUserMger.getCollection ().drop ();
      aUserTokenMgr.getCollection ().drop ();

      final IUser aUser = aUserMger.createNewUser ("sLoginName",
                                                   "test@smp.localhost",
                                                   "im a super secure password",
                                                   "First Name",
                                                   "Last Name",
                                                   "Description",
                                                   Locale.GERMAN,
                                                   Map.of ("foo", "bar"),
                                                   false);
      assertNotNull (aUser);

      final UserToken aUserToken = aUserTokenMgr.createUserToken ("asd", Map.of ("meta", "data"), aUser, "description");
      assertNotNull (aUserToken);
      assertEquals (aUserToken, aUserTokenMgr.getUserTokenOfID (aUserToken.getID ()));
      assertEquals (aUserToken, aUserTokenMgr.getAllActiveUserTokens ().get (0));

      assertTrue (aUserTokenMgr.updateUserToken (aUserToken.getID (), Map.of ("foo", "bar"), "dummy token")
                               .isChanged ());
      final IUserToken aResolvedUserToken = aUserTokenMgr.getUserTokenOfID (aUserToken.getID ());
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

      assertTrue (aUserTokenMgr.revokeAccessToken (aUserToken.getID (), aUser.getID (), LocalDateTime.now (), "revoked")
                               .isChanged ());
      assertFalse (aUserTokenMgr.isAccessTokenUsed (aResolvedUserToken.getID ()));
    }
  }
}
