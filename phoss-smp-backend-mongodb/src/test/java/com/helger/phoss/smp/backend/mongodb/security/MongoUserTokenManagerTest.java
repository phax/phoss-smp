package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.photon.security.token.user.IUserToken;
import com.helger.photon.security.token.user.UserToken;
import com.helger.photon.security.user.IUser;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

public class MongoUserTokenManagerTest extends MongoBaseTest
{

  private final MongoUserManager mongoUserManager = new MongoUserManager ();
  private final MongoUserTokenManager mongoUserTokenManager = new MongoUserTokenManager (mongoUserManager);

  @Test
  public void testUserTokenManagerCrud ()
  {
    mongoUserTokenManager.deleteAll ();
    mongoUserManager.deleteAll ();

    IUser newUser = mongoUserManager.createNewUser (
                               "sLoginName",
                               "test@smp.localhost",
                               "im a super secure password",
                               "First Name",
                               "Last Name",
                               "Description",
                               Locale.GERMAN,
                               Map.of ("foo", "bar"),
                               false
    );

    UserToken userToken = mongoUserTokenManager.createUserToken ("asd", Map.of ("meta", "data"), newUser, "description");
    Assert.assertNotNull (userToken);
    Assert.assertEquals (userToken, mongoUserTokenManager.getUserTokenOfID (userToken.getID ()));
    Assert.assertEquals (userToken, mongoUserTokenManager.getAllActiveUserTokens ().get (0));

    mongoUserTokenManager.updateUserToken (userToken.getID (), Map.of ("foo", "bar"), "dummy token");
    IUserToken token = mongoUserTokenManager.getUserTokenOfID (userToken.getID ());
    Assert.assertEquals ("dummy token", token.getDescription ());
    Assert.assertTrue (token.attrs ().containsKey ("foo"));

    Assert.assertFalse (mongoUserTokenManager.isAccessTokenUsed (token.getID ()));

    mongoUserTokenManager.createNewAccessToken (token.getID (),
                               newUser.getID (),
                               LocalDateTime.now (),
                               "reason",
                               "tokenString"
    );
    IUserToken tokenString = mongoUserTokenManager.getUserTokenOfTokenString ("tokenString");

    Assert.assertNotNull (tokenString);

    mongoUserTokenManager.revokeAccessToken (newUser.getID (), newUser.getID (), LocalDateTime.now (), "revoked");
    Assert.assertFalse (mongoUserTokenManager.isAccessTokenUsed (token.getID ()));
  }

}