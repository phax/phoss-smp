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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bson.Document;
import org.bson.RawBsonDocument;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.annotation.Nonempty;
import com.helger.phoss.smp.backend.mongodb.SMPServerMongoDBTestRule;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.password.GlobalPasswordSettings;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.user.IUserModificationCallback;
import com.helger.photon.security.user.User;
import com.helger.security.password.salt.PasswordSalt;

public final class UserManagerMongoDBTest
{
  @Rule
  public final TestRule m_aRule = new SMPServerMongoDBTestRule ();

  @NonNull
  private static IUser _serializeAndReRead (@NonNull final IUser aUser)
  {
    final UserManagerMongoDB aUserMgr = (UserManagerMongoDB) PhotonSecurityManager.getUserMgr ();

    final Document aDoc = aUserMgr.toBson (aUser);
    assertNotNull (aDoc);

    // Fully serialize the document as BsonDocument and re-read as user document to simulate the
    // data as retrieved from the DB
    final Document aDocAsReadFromDbSimulated = new DocumentCodec ().decode (new RawBsonDocument (aDoc,
                                                                                                 new DocumentCodec ()).asBsonReader (),
                                                                            DecoderContext.builder ().build ());

    final User aUser2 = aUserMgr.toEntity (aDocAsReadFromDbSimulated);
    assertNotNull (aUser2);

    return aUser2;
  }

  @Test
  public void testMinimalUserConversion ()
  {
    IUser aUser = new User ("minimalLogin",
                            null,
                            GlobalPasswordSettings.createUserDefaultPasswordHash (PasswordSalt.createRandom (),
                                                                                  "testPassword"),
                            null,
                            null,
                            null,
                            Locale.US,
                            null,
                            false);

    IUser aUser2 = _serializeAndReRead (aUser);
    assertNotNull (aUser2);

    assertEquals (aUser.getID (), aUser2.getID ());
    assertEquals ("minimalLogin", aUser2.getLoginName ());
    assertNull (aUser2.getEmailAddress ());
    assertEquals (aUser.getPasswordHash (), aUser2.getPasswordHash ());
    assertNull (aUser2.getFirstName ());
    assertNull (aUser2.getLastName ());
    assertNull (aUser2.getDescription ());
    assertEquals (Locale.US, aUser2.getDesiredLocale ());
    assertNull (aUser2.getLastLoginDateTime ());
    assertEquals (0, aUser2.getLoginCount ());
    assertEquals (0, aUser2.getConsecutiveFailedLoginCount ());
    assertFalse (aUser2.isDisabled ());

    // Without locale
    aUser = new User ("minimalLogin",
                      null,
                      GlobalPasswordSettings.createUserDefaultPasswordHash (PasswordSalt.createRandom (),
                                                                            "testPassword"),
                      null,
                      null,
                      null,
                      null,
                      null,
                      false);

    aUser2 = _serializeAndReRead (aUser);
    assertNotNull (aUser2);

    assertEquals (aUser.getID (), aUser2.getID ());
    assertEquals ("minimalLogin", aUser2.getLoginName ());
    assertNull (aUser2.getEmailAddress ());
    assertEquals (aUser.getPasswordHash (), aUser2.getPasswordHash ());
    assertNull (aUser2.getFirstName ());
    assertNull (aUser2.getLastName ());
    assertNull (aUser2.getDescription ());
    assertNull (aUser2.getDesiredLocale ());
    assertNull (aUser2.getLastLoginDateTime ());
    assertEquals (0, aUser2.getLoginCount ());
    assertEquals (0, aUser2.getConsecutiveFailedLoginCount ());
    assertFalse (aUser2.isDisabled ());
  }

  @Test
  public void testUserManagerCrud ()
  {
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();

    final var aOldUser = aUserMgr.getUserOfLoginName ("UserMgrTest");
    if (aOldUser != null)
      ((UserManagerMongoDB) aUserMgr).internalDeleteUserNotRecoverable (aOldUser.getID ());
    final IUser aUser = aUserMgr.createNewUser ("UserMgrTest",
                                                "usermgr@smp.localhost",
                                                "im a super secure password",
                                                "First Name",
                                                "Last Name",
                                                "Description",
                                                Locale.GERMAN,
                                                Map.of ("foo", "bar"),
                                                false);
    assertNotNull (aUser);

    final String sUserID = aUser.getID ();
    try
    {
      assertEquals ("UserMgrTest", aUser.getLoginName ());
      assertEquals ("usermgr@smp.localhost", aUser.getEmailAddress ());
      assertNotEquals ("im a super secure password", aUser.getPasswordHash ());
      assertEquals ("First Name", aUser.getFirstName ());
      assertEquals ("Last Name", aUser.getLastName ());
      assertEquals ("Description", aUser.getDescription ());
      assertEquals (Locale.GERMAN, aUser.getDesiredLocale ());
      assertEquals (1, aUser.attrs ().size ());
      assertEquals ("bar", aUser.attrs ().get ("foo"));
      assertTrue (aUser.isEnabled ());

      assertEquals (aUser, aUserMgr.getUserOfID (sUserID));
      assertEquals (aUser, aUserMgr.getUserOfLoginName ("UserMgrTest"));
      assertEquals (aUser, aUserMgr.getUserOfEmailAddress ("usermgr@smp.localhost"));
      assertEquals (aUser, aUserMgr.getUserOfEmailAddressIgnoreCase ("UserMgr@sMp.lOcalHost"));

      assertNull (aUserMgr.getUserOfID ("im Not Here"));
      assertNull (aUserMgr.getUserOfLoginName ("im Not Here"));
      assertNull (aUserMgr.getUserOfEmailAddress ("im Not Here"));
      assertNull (aUserMgr.getUserOfEmailAddressIgnoreCase ("im Not Here"));

      // Login name already in use
      {
        final IUser aDuplicateUser = aUserMgr.createNewUser ("UserMgrTest",
                                                             null,
                                                             "1234",
                                                             null,
                                                             null,
                                                             null,
                                                             null,
                                                             null,
                                                             false);
        assertNull (aDuplicateUser);
      }

      // Disable - enable
      assertFalse (aUserMgr.getAllDisabledUsers ().contains (aUser));
      assertTrue (aUserMgr.disableUser (sUserID).isChanged ());
      assertTrue (aUserMgr.getAllDisabledUsers ().contains (aUser));
      assertTrue (aUserMgr.enableUser (sUserID).isChanged ());
      assertFalse (aUserMgr.getAllDisabledUsers ().contains (aUser));

      // Delete - undelete
      assertFalse (aUserMgr.getAllDeletedUsers ().contains (aUser));
      assertFalse (aUserMgr.getAllDisabledUsers ().contains (aUser));
      assertTrue (aUserMgr.getAllNotDeletedUsers ().contains (aUser));
      assertTrue (aUserMgr.deleteUser (sUserID).isChanged ());
      assertFalse (aUserMgr.getAllDisabledUsers ().contains (aUser));
      assertTrue (aUserMgr.getAllDeletedUsers ().contains (aUser));

      assertTrue (aUserMgr.enableUser (sUserID).isChanged ());
      assertTrue (aUserMgr.getAllDeletedUsers ().contains (aUser));
      assertFalse (aUserMgr.getAllDisabledUsers ().contains (aUser));

      assertTrue (aUserMgr.undeleteUser (sUserID).isChanged ());
      assertFalse (aUserMgr.getAllDeletedUsers ().contains (aUser));
      assertFalse (aUserMgr.getAllDisabledUsers ().contains (aUser));
      assertTrue (aUserMgr.getAllNotDeletedUsers ().contains (aUser));

      final AtomicBoolean aCBWasCalled = new AtomicBoolean (false);
      aUserMgr.userModificationCallbacks ().add (new IUserModificationCallback ()
      {
        @Override
        public void onUserLastFailedLoginUpdated (@NonNull @Nonempty final String sID)
        {
          aCBWasCalled.set (true);
        }
      });
      assertFalse (aCBWasCalled.get ());
      assertTrue (aUserMgr.updateUserLastFailedLogin (sUserID).isChanged ());
      assertTrue (aCBWasCalled.get ());
      assertEquals (1, aUserMgr.getUserOfID (sUserID).getConsecutiveFailedLoginCount ());
      assertTrue (aUserMgr.updateUserLastFailedLogin (sUserID).isChanged ());
      assertEquals (2, aUserMgr.getUserOfID (sUserID).getConsecutiveFailedLoginCount ());
      assertTrue (aUserMgr.updateUserLastLogin (sUserID).isChanged ());

      // Re-resolve user
      final IUser aLoggedInUser = aUserMgr.getActiveUserOfID (sUserID);
      assertNotNull (aLoggedInUser);

      assertEquals (0, aLoggedInUser.getConsecutiveFailedLoginCount ());
      assertTrue (aLoggedInUser.getLoginCount () >= 1);
      assertNotNull (aLoggedInUser.getLastLoginDateTime ());

      assertTrue (aUserMgr.setUserPassword (sUserID, "new secure password").isChanged ());
      assertNotEquals (aUser.getPasswordHash ().getPasswordHashValue (),
                       aUserMgr.getUserOfID (sUserID).getPasswordHash ().getPasswordHashValue ());

      assertTrue (aUserMgr.setUserData (sUserID,
                                        "newloginName",
                                        "newEmail@smp.localhost",
                                        "NewFN",
                                        "NewLN",
                                        "NewDescription",
                                        Locale.US,
                                        Map.of ("test", "123"),
                                        true).isChanged ());
      assertTrue (aUserMgr.getAllDisabledUsers ().contains (aUser));
      assertFalse (aUserMgr.getAllActiveUsers ().contains (aUser));
      assertTrue (aUserMgr.enableUser (sUserID).isChanged ());
      assertTrue (aUserMgr.containsAnyActiveUser ());

      IUser aNewLoginNameUser = aUserMgr.getUserOfLoginName ("newloginName");
      assertNotNull (aNewLoginNameUser);
      assertEquals (aUser, aNewLoginNameUser);

      assertEquals ("newloginName", aNewLoginNameUser.getLoginName ());
      assertEquals ("newEmail@smp.localhost", aNewLoginNameUser.getEmailAddress ());
      assertEquals ("NewFN", aNewLoginNameUser.getFirstName ());
      assertEquals ("NewLN", aNewLoginNameUser.getLastName ());
      assertEquals ("NewDescription", aNewLoginNameUser.getDescription ());
      assertEquals (Locale.US, aNewLoginNameUser.getDesiredLocale ());

      assertTrue (aUserMgr.setUserData (sUserID, "newloginName2", null, null, null, null, null, null, true)
                          .isChanged ());
      assertTrue (aUserMgr.getAllDisabledUsers ().contains (aUser));
      assertFalse (aUserMgr.getAllActiveUsers ().contains (aUser));
      assertTrue (aUserMgr.enableUser (sUserID).isChanged ());
      assertTrue (aUserMgr.containsAnyActiveUser ());

      aNewLoginNameUser = aUserMgr.getUserOfLoginName ("newloginName2");
      assertNotNull (aNewLoginNameUser);
      assertEquals (aUser, aNewLoginNameUser);

      assertEquals ("newloginName2", aNewLoginNameUser.getLoginName ());
      assertNull (aNewLoginNameUser.getEmailAddress ());
      assertNull (aNewLoginNameUser.getFirstName ());
      assertNull (aNewLoginNameUser.getLastName ());
      assertNull (aNewLoginNameUser.getDescription ());
      assertNull (aNewLoginNameUser.getDesiredLocale ());
    }
    finally
    {
      assertTrue (aUserMgr.deleteUser (sUserID).isChanged ());
      assertFalse (aUserMgr.getAllActiveUsers ().contains (aUser));
      assertTrue (aUserMgr.getAllDeletedUsers ().contains (aUser));
      assertTrue (aUserMgr.getAll ().contains (aUser));
      ((UserManagerMongoDB) aUserMgr).internalDeleteUserNotRecoverable (sUserID);
    }
  }
}
