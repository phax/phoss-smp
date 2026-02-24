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

        final IUser aNewLoginNameUser = aUserMgr.getUserOfLoginName ("newloginName");
        assertNotNull (aNewLoginNameUser);
        assertEquals (aUser, aNewLoginNameUser);

        assertEquals ("newloginName", aNewLoginNameUser.getLoginName ());
        assertEquals ("newEmail@smp.localhost", aNewLoginNameUser.getEmailAddress ());
        assertEquals ("NewFN", aNewLoginNameUser.getFirstName ());
        assertEquals ("NewLN", aNewLoginNameUser.getLastName ());
        assertEquals ("NewDescription", aNewLoginNameUser.getDescription ());
        assertEquals (Locale.US, aNewLoginNameUser.getDesiredLocale ());
      }
      finally
      {
        assertTrue (aUserMgr.deleteUser (sUserID).isChanged ());
        assertFalse (aUserMgr.getAllActive ().contains (aUser));
        assertTrue (aUserMgr.getAllDeleted ().contains (aUser));
        assertTrue (aUserMgr.getAll ().contains (aUser));
        aUserMgr.internalDeleteUserNotRecoverable (sUserID);
      }
    }
  }
}
