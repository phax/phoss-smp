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

public final class UserTokenManagerMongoDBTest extends MongoBaseTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testUserTokenManagerCrud ()
  {
    try (final UserManagerMongoDB mongoUserManager = new UserManagerMongoDB ();
         final UserTokenManagerMongoDB mongoUserTokenManager = new UserTokenManagerMongoDB (mongoUserManager))
    {
      mongoUserManager.getCollection ().drop ();
      mongoUserTokenManager.getCollection ().drop ();

      final IUser newUser = mongoUserManager.createNewUser ("sLoginName",
                                                            "test@smp.localhost",
                                                            "im a super secure password",
                                                            "First Name",
                                                            "Last Name",
                                                            "Description",
                                                            Locale.GERMAN,
                                                            Map.of ("foo", "bar"),
                                                            false);

      final UserToken userToken = mongoUserTokenManager.createUserToken ("asd",
                                                                         Map.of ("meta", "data"),
                                                                         newUser,
                                                                         "description");
      assertNotNull (userToken);
      assertEquals (userToken, mongoUserTokenManager.getUserTokenOfID (userToken.getID ()));
      assertEquals (userToken, mongoUserTokenManager.getAllActiveUserTokens ().get (0));

      mongoUserTokenManager.updateUserToken (userToken.getID (), Map.of ("foo", "bar"), "dummy token");
      final IUserToken token = mongoUserTokenManager.getUserTokenOfID (userToken.getID ());
      assertEquals ("dummy token", token.getDescription ());
      assertTrue (token.attrs ().containsKey ("foo"));

      assertFalse (mongoUserTokenManager.isAccessTokenUsed (token.getID ()));

      mongoUserTokenManager.createNewAccessToken (token.getID (),
                                                  newUser.getID (),
                                                  LocalDateTime.now (),
                                                  "reason",
                                                  "tokenString");
      final IUserToken tokenString = mongoUserTokenManager.getUserTokenOfTokenString ("tokenString");

      assertNotNull (tokenString);

      mongoUserTokenManager.revokeAccessToken (newUser.getID (), newUser.getID (), LocalDateTime.now (), "revoked");
      assertFalse (mongoUserTokenManager.isAccessTokenUsed (token.getID ()));
    }
  }
}
