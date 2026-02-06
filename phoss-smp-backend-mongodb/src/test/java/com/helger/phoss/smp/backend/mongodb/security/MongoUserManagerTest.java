package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.annotation.Nonempty;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserModificationCallback;
import org.jspecify.annotations.NonNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MongoUserManagerTest extends MongoBaseTest
{

  private final MongoUserManager mongoUserManager = new MongoUserManager ();

  @Test
  public void testUserManagerCrud ()
  {
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
    String id = newUser.getID ();
    Assert.assertEquals (newUser, mongoUserManager.getUserOfID (id));
    Assert.assertEquals (newUser, mongoUserManager.getUserOfLoginName ("sLoginName"));
    Assert.assertEquals (newUser, mongoUserManager.getUserOfEmailAddress ("test@smp.localhost"));
    Assert.assertEquals (newUser, mongoUserManager.getUserOfEmailAddressIgnoreCase ("Test@sMp.lOcalHost"));
    Assert.assertNull (mongoUserManager.getUserOfEmailAddressIgnoreCase ("im Not Here"));


    IUser duplicateName = mongoUserManager.createNewUser ("sLoginName", null, "1234", null,
                               null, null, null, null, false);

    Assert.assertNull (duplicateName);

    Assert.assertTrue (mongoUserManager.getAllDisabledUsers ().isEmpty ());
    mongoUserManager.disableUser (id);
    Assert.assertEquals (1, mongoUserManager.getAllDisabledUsers ().size ());
    mongoUserManager.enableUser (id);
    Assert.assertTrue (mongoUserManager.getAllDisabledUsers ().isEmpty ());
    mongoUserManager.deleteUser (id);
    Assert.assertTrue (mongoUserManager.getAllDisabledUsers ().isEmpty ());
    Assert.assertEquals (1, mongoUserManager.getAllDeletedUsers ().size ());
    mongoUserManager.enableUser (id);
    Assert.assertEquals (1, mongoUserManager.getAllDeletedUsers ().size ());
    Assert.assertTrue (mongoUserManager.getAllDisabledUsers ().isEmpty ());
    mongoUserManager.undeleteUser (id);
    Assert.assertTrue (mongoUserManager.getAllDeletedUsers ().isEmpty ());
    Assert.assertTrue (mongoUserManager.getAllDisabledUsers ().isEmpty ());
    Assert.assertEquals (1, mongoUserManager.getAllNotDeletedUsers ().size ());

    AtomicBoolean callBackWasCalled = new AtomicBoolean (false);
    mongoUserManager.userModificationCallbacks ().add (new IUserModificationCallback ()
    {
      @Override
      public void onUserLastFailedLoginUpdated (@NonNull @Nonempty String sUserID)
      {
        callBackWasCalled.set (true);
        ;
      }
    });
    Assert.assertFalse (callBackWasCalled.get ());
    mongoUserManager.updateUserLastFailedLogin (id);
    Assert.assertTrue (callBackWasCalled.get ());
    Assert.assertEquals (1, mongoUserManager.getUserOfID (id).getConsecutiveFailedLoginCount ());
    mongoUserManager.updateUserLastFailedLogin (id);
    Assert.assertEquals (2, mongoUserManager.getUserOfID (id).getConsecutiveFailedLoginCount ());
    mongoUserManager.updateUserLastLogin (id);
    IUser loggedIn = mongoUserManager.getUserOfID (id);
    Assert.assertEquals (0, loggedIn.getConsecutiveFailedLoginCount ());
    Assert.assertEquals (1, loggedIn.getLoginCount ());
    Assert.assertNotNull (loggedIn.getLastLoginDateTime ());

    mongoUserManager.setUserPassword (id, "new secure password");
    Assert.assertNotEquals (newUser.getPasswordHash ().getPasswordHashValue (),
                               mongoUserManager.getUserOfID (id).getPasswordHash ().getPasswordHashValue ());


    mongoUserManager.setUserData (id,
                               "newloginName",
                               "newEmail@smp.localhost",
                               "NewFN",
                               "NewLN",
                               "NewDescription",
                               Locale.US,
                               Map.of ("test", "123"),
                               true
    );
    Assert.assertEquals (1, mongoUserManager.getAllDisabledUsers ().size ());
    Assert.assertEquals (0, mongoUserManager.getAllActiveUsers ().size ());
    Assert.assertEquals (0, mongoUserManager.getActiveUserCount ());
    Assert.assertFalse (mongoUserManager.containsAnyActiveUser ());
    mongoUserManager.enableUser (id);
    Assert.assertTrue (mongoUserManager.containsAnyActiveUser ());
    Assert.assertEquals (1, mongoUserManager.getAllActiveUsers ().size ());
    Assert.assertEquals (1, mongoUserManager.getActiveUserCount ());

    IUser newloginName = mongoUserManager.getUserOfLoginName ("newloginName");

    Assert.assertEquals ("newloginName", newloginName.getLoginName ());
    Assert.assertEquals ("newEmail@smp.localhost", newloginName.getEmailAddress ());
    Assert.assertEquals ("NewFN", newloginName.getFirstName ());
    Assert.assertEquals ("NewLN", newloginName.getLastName ());
    Assert.assertEquals ("NewDescription", newloginName.getDescription ());
    Assert.assertEquals (Locale.US, newloginName.getDesiredLocale ());

  }

}