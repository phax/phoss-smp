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

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;

import com.helger.annotation.Nonempty;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserModificationCallback;

public final class UserManagerMongoDBTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testUserManagerCrud ()
  {
    try (final UserManagerMongoDB aUserMgr = new UserManagerMongoDB ())
    {
      aUserMgr.getCollection ().drop ();

      final IUser aUser = aUserMgr.createNewUser ("sLoginName",
                                                  "test@smp.localhost",
                                                  "im a super secure password",
                                                  "First Name",
                                                  "Last Name",
                                                  "Description",
                                                  Locale.GERMAN,
                                                  Map.of ("foo", "bar"),
                                                  false);
      assertNotNull (aUser);
      assertEquals ("sLoginName", aUser.getLoginName ());
      assertEquals ("test@smp.localhost", aUser.getEmailAddress ());
      assertNotEquals ("im a super secure password", aUser.getPasswordHash ());
      assertEquals ("First Name", aUser.getFirstName ());
      assertEquals ("Last Name", aUser.getLastName ());
      assertEquals ("Description", aUser.getDescription ());
      assertEquals (Locale.GERMAN, aUser.getDesiredLocale ());
      assertEquals (1, aUser.attrs ().size ());
      assertEquals ("bar", aUser.attrs ().get ("foo"));
      assertTrue (aUser.isEnabled ());

      final String sUserID = aUser.getID ();
      assertEquals (aUser, aUserMgr.getUserOfID (sUserID));
      assertEquals (aUser, aUserMgr.getUserOfLoginName ("sLoginName"));
      assertEquals (aUser, aUserMgr.getUserOfEmailAddress ("test@smp.localhost"));
      assertEquals (aUser, aUserMgr.getUserOfEmailAddressIgnoreCase ("Test@sMp.lOcalHost"));

      assertNull (aUserMgr.getUserOfID ("im Not Here"));
      assertNull (aUserMgr.getUserOfLoginName ("im Not Here"));
      assertNull (aUserMgr.getUserOfEmailAddress ("im Not Here"));
      assertNull (aUserMgr.getUserOfEmailAddressIgnoreCase ("im Not Here"));

      final IUser aDuplicateUser = aUserMgr.createNewUser ("sLoginName",
                                                           null,
                                                           "1234",
                                                           null,
                                                           null,
                                                           null,
                                                           null,
                                                           null,
                                                           false);
      assertNull (aDuplicateUser);

      assertTrue (aUserMgr.getAllDisabledUsers ().isEmpty ());
      assertTrue (aUserMgr.disableUser (sUserID).isChanged ());
      assertEquals (1, aUserMgr.getAllDisabledUsers ().size ());
      assertTrue (aUserMgr.enableUser (sUserID).isChanged ());
      assertTrue (aUserMgr.getAllDisabledUsers ().isEmpty ());
      assertTrue (aUserMgr.deleteUser (sUserID).isChanged ());
      assertTrue (aUserMgr.getAllDisabledUsers ().isEmpty ());
      assertEquals (1, aUserMgr.getAllDeletedUsers ().size ());
      assertTrue (aUserMgr.enableUser (sUserID).isChanged ());
      assertEquals (1, aUserMgr.getAllDeletedUsers ().size ());
      assertTrue (aUserMgr.getAllDisabledUsers ().isEmpty ());
      assertTrue (aUserMgr.undeleteUser (sUserID).isChanged ());
      assertTrue (aUserMgr.getAllDeletedUsers ().isEmpty ());
      assertTrue (aUserMgr.getAllDisabledUsers ().isEmpty ());
      assertEquals (1, aUserMgr.getAllNotDeletedUsers ().size ());

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
      final IUser aLoggedInUser = aUserMgr.getUserOfID (sUserID);
      assertNotNull (aLoggedInUser);
      assertEquals (0, aLoggedInUser.getConsecutiveFailedLoginCount ());
      assertEquals (1, aLoggedInUser.getLoginCount ());
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
      assertEquals (1, aUserMgr.getAllDisabledUsers ().size ());
      assertEquals (0, aUserMgr.getAllActiveUsers ().size ());
      assertEquals (0, aUserMgr.getActiveUserCount ());
      assertFalse (aUserMgr.containsAnyActiveUser ());
      assertTrue (aUserMgr.enableUser (sUserID).isChanged ());
      assertTrue (aUserMgr.containsAnyActiveUser ());
      assertEquals (1, aUserMgr.getAllActiveUsers ().size ());
      assertEquals (1, aUserMgr.getActiveUserCount ());

      final IUser aNewLoginNameUser = aUserMgr.getUserOfLoginName ("newloginName");
      assertNotNull (aNewLoginNameUser);

      assertEquals ("newloginName", aNewLoginNameUser.getLoginName ());
      assertEquals ("newEmail@smp.localhost", aNewLoginNameUser.getEmailAddress ());
      assertEquals ("NewFN", aNewLoginNameUser.getFirstName ());
      assertEquals ("NewLN", aNewLoginNameUser.getLastName ());
      assertEquals ("NewDescription", aNewLoginNameUser.getDescription ());
      assertEquals (Locale.US, aNewLoginNameUser.getDesiredLocale ());
    }
  }
}
