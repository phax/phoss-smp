package com.helger.phoss.smp.backend.mongodb.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.usergroup.IUserGroup;

public final class MongoUserGroupManagerTest extends MongoBaseTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testUserGroupManagerCrud ()
  {
    final MongoUserManager mongoUserManager = new MongoUserManager ();
    final MongoRoleManager mongoRoleManager = new MongoRoleManager ();
    final MongoUserGroupManager mongoUserGroupManager = new MongoUserGroupManager (mongoUserManager, mongoRoleManager);

    mongoUserGroupManager.getCollection ().drop ();

    final IUserGroup newUserGroup = mongoUserGroupManager.createNewUserGroup ("group1",
                                                                              "description",
                                                                              Map.of ("foo", "bar"));
    final String userGroupID = newUserGroup.getID ();
    mongoUserGroupManager.getUserGroupOfID (userGroupID);

    assertEquals (newUserGroup, mongoUserGroupManager.getUserGroupOfID (userGroupID));

    mongoUserGroupManager.setUserGroupData (userGroupID, "test user", "group for tests", Map.of ("foo1", "bar1"));
    final IUserGroup updated = mongoUserGroupManager.getUserGroupOfID (userGroupID);
    assertEquals ("test user", updated.getName ());
    assertEquals ("group for tests", updated.getDescription ());
    assertEquals ("bar1", updated.attrs ().get ("foo1"));

    mongoUserGroupManager.renameUserGroup (userGroupID, "newName");
    assertEquals ("newName", mongoUserGroupManager.getUserGroupOfID (userGroupID).getName ());

    final String userId = UUID.randomUUID ().toString ();

    mongoUserGroupManager.assignUserToUserGroup (userGroupID, userId);
    assertTrue (mongoUserGroupManager.isUserAssignedToUserGroup (userGroupID, userId));
    assertFalse (mongoUserGroupManager.isUserAssignedToUserGroup (userGroupID, "another uuid"));

    final IUserGroup anotherGroup = mongoUserGroupManager.createNewUserGroup ("group2", null, null);
    final String anotherGroupUserGroupID = anotherGroup.getID ();
    mongoUserGroupManager.assignUserToUserGroup (anotherGroupUserGroupID, userId);

    final ICommonsList <String> allUserGroupIDsWithAssignedUser = mongoUserGroupManager.getAllUserGroupIDsWithAssignedUser (userId);
    assertTrue (allUserGroupIDsWithAssignedUser.contains (userGroupID));
    assertTrue (allUserGroupIDsWithAssignedUser.contains (anotherGroupUserGroupID));

    mongoUserGroupManager.unassignUserFromUserGroup (userGroupID, userId);
    assertFalse (mongoUserGroupManager.isUserAssignedToUserGroup (userGroupID, userId));
    mongoUserGroupManager.unassignUserFromAllUserGroups (userId);
    assertFalse (mongoUserGroupManager.isUserAssignedToUserGroup (anotherGroupUserGroupID, userId));

    final String roleUUID = UUID.randomUUID ().toString ();
    mongoUserGroupManager.assignRoleToUserGroup (anotherGroupUserGroupID, roleUUID);
    mongoUserGroupManager.assignRoleToUserGroup (userGroupID, roleUUID);
    final ICommonsList <String> allUserGroupIDsWithAssignedRole = mongoUserGroupManager.getAllUserGroupIDsWithAssignedRole (roleUUID);
    assertTrue (allUserGroupIDsWithAssignedRole.contains (userGroupID));
    assertTrue (allUserGroupIDsWithAssignedRole.contains (anotherGroupUserGroupID));

    assertFalse (mongoUserGroupManager.containsAnyUserGroupWithAssignedUserAndRole (userId, roleUUID));
    mongoUserGroupManager.assignUserToUserGroup (anotherGroupUserGroupID, userId);
    assertTrue (mongoUserGroupManager.containsAnyUserGroupWithAssignedUserAndRole (userId, roleUUID));

    mongoUserGroupManager.unassignRoleFromAllUserGroups (roleUUID);
    assertFalse (mongoUserGroupManager.containsAnyUserGroupWithAssignedUserAndRole (userId, roleUUID));
    assertTrue (mongoUserGroupManager.getAllUserGroupIDsWithAssignedRole (roleUUID).isEmpty ());

    assertEquals (2, mongoUserGroupManager.getAll ().size ());
    assertEquals (2, mongoUserGroupManager.getAllActiveUserGroups ().size ());
    assertEquals (0, mongoUserGroupManager.getAllDeletedUserGroups ().size ());

    mongoUserGroupManager.deleteUserGroup (anotherGroupUserGroupID);
    assertEquals (2, mongoUserGroupManager.getAll ().size ());
    assertEquals (1, mongoUserGroupManager.getAllActiveUserGroups ().size ());
    assertEquals (1, mongoUserGroupManager.getAllDeletedUserGroups ().size ());

    mongoUserGroupManager.undeleteUserGroup (anotherGroupUserGroupID);
    assertEquals (2, mongoUserGroupManager.getAll ().size ());
    assertEquals (2, mongoUserGroupManager.getAllActiveUserGroups ().size ());
    assertEquals (0, mongoUserGroupManager.getAllDeletedUserGroups ().size ());

    mongoUserGroupManager.assignRoleToUserGroup (anotherGroupUserGroupID, roleUUID);
    assertTrue (mongoUserGroupManager.containsUserGroupWithAssignedRole (roleUUID));
    mongoUserGroupManager.unassignRoleFromUserGroup (anotherGroupUserGroupID, roleUUID);
    assertFalse (mongoUserGroupManager.containsUserGroupWithAssignedRole (roleUUID));
  }

}
