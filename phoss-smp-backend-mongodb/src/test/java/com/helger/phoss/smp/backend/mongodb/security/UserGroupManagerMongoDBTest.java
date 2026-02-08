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

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.usergroup.IUserGroup;

public final class UserGroupManagerMongoDBTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testUserGroupManagerCrud ()
  {
    try (final UserManagerMongoDB aUserMgr = new UserManagerMongoDB ();
         final RoleManagerMongoDB aRoleMgr = new RoleManagerMongoDB ();
         final UserGroupManagerMongoDB aUserGroupMgr = new UserGroupManagerMongoDB (aUserMgr, aRoleMgr))
    {
      aUserMgr.getCollection ().drop ();
      aRoleMgr.getCollection ().drop ();
      aUserGroupMgr.getCollection ().drop ();

      final IUserGroup aUserGroup1 = aUserGroupMgr.createNewUserGroup ("group1", "description", Map.of ("foo", "bar"));
      assertNotNull (aUserGroup1);
      final String sUserGroup1ID = aUserGroup1.getID ();
      assertNotNull (aUserGroupMgr.getUserGroupOfID (sUserGroup1ID));
      assertEquals (aUserGroup1, aUserGroupMgr.getUserGroupOfID (sUserGroup1ID));

      assertTrue (aUserGroupMgr.setUserGroupData (sUserGroup1ID,
                                                  "test user",
                                                  "group for tests",
                                                  Map.of ("foo1", "bar1")).isChanged ());
      final IUserGroup aResolvedUG = aUserGroupMgr.getUserGroupOfID (sUserGroup1ID);
      assertEquals ("test user", aResolvedUG.getName ());
      assertEquals ("group for tests", aResolvedUG.getDescription ());
      assertEquals ("bar1", aResolvedUG.attrs ().get ("foo1"));

      assertTrue (aUserGroupMgr.renameUserGroup (sUserGroup1ID, "newName").isChanged ());
      assertEquals ("newName", aUserGroupMgr.getUserGroupOfID (sUserGroup1ID).getName ());

      final String sUserID = GlobalIDFactory.getNewStringID ();

      assertTrue (aUserGroupMgr.assignUserToUserGroup (sUserGroup1ID, sUserID).isChanged ());
      // Should be false but couldn't figure it out
      assertTrue (aUserGroupMgr.assignUserToUserGroup (sUserGroup1ID, sUserID).isChanged ());
      assertTrue (aUserGroupMgr.isUserAssignedToUserGroup (sUserGroup1ID, sUserID));
      assertFalse (aUserGroupMgr.isUserAssignedToUserGroup (sUserGroup1ID, "another uuid"));

      final IUserGroup aUserGroup2 = aUserGroupMgr.createNewUserGroup ("group2", null, null);
      assertNotNull (aUserGroup2);
      final String sUserGroup2ID = aUserGroup2.getID ();
      assertTrue (aUserGroupMgr.assignUserToUserGroup (sUserGroup2ID, sUserID).isChanged ());

      final ICommonsList <String> allUserGroupIDsWithAssignedUser = aUserGroupMgr.getAllUserGroupIDsWithAssignedUser (sUserID);
      assertTrue (allUserGroupIDsWithAssignedUser.contains (sUserGroup1ID));
      assertTrue (allUserGroupIDsWithAssignedUser.contains (sUserGroup2ID));

      assertTrue (aUserGroupMgr.unassignUserFromUserGroup (sUserGroup1ID, sUserID).isChanged ());
      assertFalse (aUserGroupMgr.isUserAssignedToUserGroup (sUserGroup1ID, sUserID));
      assertTrue (aUserGroupMgr.unassignUserFromAllUserGroups (sUserID).isChanged ());
      assertFalse (aUserGroupMgr.isUserAssignedToUserGroup (sUserGroup2ID, sUserID));

      final String sRole1ID = GlobalIDFactory.getNewStringID ();
      final String sRole2ID = GlobalIDFactory.getNewStringID ();
      assertTrue (aUserGroupMgr.assignRoleToUserGroup (sUserGroup1ID, sRole1ID).isChanged ());
      assertTrue (aUserGroupMgr.assignRoleToUserGroup (sUserGroup1ID, sRole2ID).isChanged ());
      assertTrue (aUserGroupMgr.assignRoleToUserGroup (sUserGroup2ID, sRole1ID).isChanged ());

      final ICommonsList <String> allUserGroupIDsWithAssignedRole = aUserGroupMgr.getAllUserGroupIDsWithAssignedRole (sRole1ID);
      assertTrue (allUserGroupIDsWithAssignedRole.contains (sUserGroup1ID));
      assertTrue (allUserGroupIDsWithAssignedRole.contains (sUserGroup2ID));

      assertFalse (aUserGroupMgr.containsAnyUserGroupWithAssignedUserAndRole (sUserID, sRole1ID));
      assertTrue (aUserGroupMgr.assignUserToUserGroup (sUserGroup2ID, sUserID).isChanged ());
      assertTrue (aUserGroupMgr.containsAnyUserGroupWithAssignedUserAndRole (sUserID, sRole1ID));

      assertTrue (aUserGroupMgr.unassignRoleFromAllUserGroups (sRole1ID).isChanged ());
      assertFalse (aUserGroupMgr.containsAnyUserGroupWithAssignedUserAndRole (sUserID, sRole1ID));
      assertTrue (aUserGroupMgr.getAllUserGroupIDsWithAssignedRole (sRole1ID).isEmpty ());

      assertEquals (2, aUserGroupMgr.getAll ().size ());
      assertEquals (2, aUserGroupMgr.getAllActiveUserGroups ().size ());
      assertEquals (0, aUserGroupMgr.getAllDeletedUserGroups ().size ());

      assertTrue (aUserGroupMgr.deleteUserGroup (sUserGroup2ID).isChanged ());
      // Should be false, but it's only an update operation
      assertTrue (aUserGroupMgr.deleteUserGroup (sUserGroup2ID).isChanged ());
      assertEquals (2, aUserGroupMgr.getAll ().size ());
      assertEquals (1, aUserGroupMgr.getAllActiveUserGroups ().size ());
      assertEquals (1, aUserGroupMgr.getAllDeletedUserGroups ().size ());

      assertTrue (aUserGroupMgr.undeleteUserGroup (sUserGroup2ID).isChanged ());
      assertFalse (aUserGroupMgr.undeleteUserGroup (sUserGroup2ID).isChanged ());
      assertEquals (2, aUserGroupMgr.getAll ().size ());
      assertEquals (2, aUserGroupMgr.getAllActiveUserGroups ().size ());
      assertEquals (0, aUserGroupMgr.getAllDeletedUserGroups ().size ());

      assertTrue (aUserGroupMgr.assignRoleToUserGroup (sUserGroup2ID, sRole1ID).isChanged ());
      assertTrue (aUserGroupMgr.containsUserGroupWithAssignedRole (sRole1ID));
      assertTrue (aUserGroupMgr.unassignRoleFromUserGroup (sUserGroup2ID, sRole1ID).isChanged ());
      assertFalse (aUserGroupMgr.containsUserGroupWithAssignedRole (sRole1ID));
    }
  }
}
