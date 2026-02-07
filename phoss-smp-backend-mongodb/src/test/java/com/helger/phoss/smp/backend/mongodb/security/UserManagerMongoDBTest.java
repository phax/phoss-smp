package com.helger.phoss.smp.backend.mongodb.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;

import com.helger.annotation.Nonempty;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserModificationCallback;

public final class UserManagerMongoDBTest extends MongoBaseTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testUserManagerCrud ()
  {
    try (final UserManagerMongoDB mongoUserManager = new UserManagerMongoDB ())
    {
      mongoUserManager.getCollection ().drop ();

      final IUser newUser = mongoUserManager.createNewUser ("sLoginName",
                                                            "test@smp.localhost",
                                                            "im a super secure password",
                                                            "First Name",
                                                            "Last Name",
                                                            "Description",
                                                            Locale.GERMAN,
                                                            Map.of ("foo", "bar"),
                                                            false);
      final String id = newUser.getID ();
      assertEquals (newUser, mongoUserManager.getUserOfID (id));
      assertEquals (newUser, mongoUserManager.getUserOfLoginName ("sLoginName"));
      assertEquals (newUser, mongoUserManager.getUserOfEmailAddress ("test@smp.localhost"));
      assertEquals (newUser, mongoUserManager.getUserOfEmailAddressIgnoreCase ("Test@sMp.lOcalHost"));
      assertNull (mongoUserManager.getUserOfEmailAddressIgnoreCase ("im Not Here"));

      final IUser duplicateName = mongoUserManager.createNewUser ("sLoginName",
                                                                  null,
                                                                  "1234",
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  false);

      assertNull (duplicateName);

      assertTrue (mongoUserManager.getAllDisabledUsers ().isEmpty ());
      mongoUserManager.disableUser (id);
      assertEquals (1, mongoUserManager.getAllDisabledUsers ().size ());
      mongoUserManager.enableUser (id);
      assertTrue (mongoUserManager.getAllDisabledUsers ().isEmpty ());
      mongoUserManager.deleteUser (id);
      assertTrue (mongoUserManager.getAllDisabledUsers ().isEmpty ());
      assertEquals (1, mongoUserManager.getAllDeletedUsers ().size ());
      mongoUserManager.enableUser (id);
      assertEquals (1, mongoUserManager.getAllDeletedUsers ().size ());
      assertTrue (mongoUserManager.getAllDisabledUsers ().isEmpty ());
      mongoUserManager.undeleteUser (id);
      assertTrue (mongoUserManager.getAllDeletedUsers ().isEmpty ());
      assertTrue (mongoUserManager.getAllDisabledUsers ().isEmpty ());
      assertEquals (1, mongoUserManager.getAllNotDeletedUsers ().size ());

      final AtomicBoolean callBackWasCalled = new AtomicBoolean (false);
      mongoUserManager.userModificationCallbacks ().add (new IUserModificationCallback ()
      {
        @Override
        public void onUserLastFailedLoginUpdated (@NonNull @Nonempty final String sUserID)
        {
          callBackWasCalled.set (true);
        }
      });
      assertFalse (callBackWasCalled.get ());
      mongoUserManager.updateUserLastFailedLogin (id);
      assertTrue (callBackWasCalled.get ());
      assertEquals (1, mongoUserManager.getUserOfID (id).getConsecutiveFailedLoginCount ());
      mongoUserManager.updateUserLastFailedLogin (id);
      assertEquals (2, mongoUserManager.getUserOfID (id).getConsecutiveFailedLoginCount ());
      mongoUserManager.updateUserLastLogin (id);
      final IUser loggedIn = mongoUserManager.getUserOfID (id);
      assertEquals (0, loggedIn.getConsecutiveFailedLoginCount ());
      assertEquals (1, loggedIn.getLoginCount ());
      assertNotNull (loggedIn.getLastLoginDateTime ());

      mongoUserManager.setUserPassword (id, "new secure password");
      assertNotEquals (newUser.getPasswordHash ().getPasswordHashValue (),
                       mongoUserManager.getUserOfID (id).getPasswordHash ().getPasswordHashValue ());

      mongoUserManager.setUserData (id,
                                    "newloginName",
                                    "newEmail@smp.localhost",
                                    "NewFN",
                                    "NewLN",
                                    "NewDescription",
                                    Locale.US,
                                    Map.of ("test", "123"),
                                    true);
      assertEquals (1, mongoUserManager.getAllDisabledUsers ().size ());
      assertEquals (0, mongoUserManager.getAllActiveUsers ().size ());
      assertEquals (0, mongoUserManager.getActiveUserCount ());
      assertFalse (mongoUserManager.containsAnyActiveUser ());
      mongoUserManager.enableUser (id);
      assertTrue (mongoUserManager.containsAnyActiveUser ());
      assertEquals (1, mongoUserManager.getAllActiveUsers ().size ());
      assertEquals (1, mongoUserManager.getActiveUserCount ());

      final IUser newloginName = mongoUserManager.getUserOfLoginName ("newloginName");

      assertEquals ("newloginName", newloginName.getLoginName ());
      assertEquals ("newEmail@smp.localhost", newloginName.getEmailAddress ());
      assertEquals ("NewFN", newloginName.getFirstName ());
      assertEquals ("NewLN", newloginName.getLastName ());
      assertEquals ("NewDescription", newloginName.getDescription ());
      assertEquals (Locale.US, newloginName.getDesiredLocale ());
    }
  }
}
